#include <jni.h>
#include <android/log.h>
#include <GLES3/gl3.h>
#include <cmath>
#include <cstdint>
#include <mutex>
#include <vector>

#define LOG_TAG "AnimakerProNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
struct Point {
    float x;
    float y;
    float size;
    float alpha;
};

struct Engine {
    std::mutex mutex;
    int width = 1;
    int height = 1;
    int currentFrame = 0;
    float brushSize = 8.0f;
    float opacity = 1.0f;
    float colorR = 0.02f;
    float colorG = 0.02f;
    float colorB = 0.02f;
    bool pressure = true;
    float stabilization = 0.35f;
    bool drawing = false;
    float lastX = 0.0f;
    float lastY = 0.0f;
    float smoothX = 0.0f;
    float smoothY = 0.0f;
    std::vector<std::vector<Point>> frames;

    Engine() : frames(1) {}

    void setFrame(int frame) {
        std::lock_guard<std::mutex> lock(mutex);
        currentFrame = frame < 0 ? 0 : frame;
        if (currentFrame >= static_cast<int>(frames.size())) frames.resize(currentFrame + 1);
    }

    void resize(int w, int h) {
        width = w > 0 ? w : 1;
        height = h > 0 ? h : 1;
        glViewport(0, 0, width, height);
    }

    void addPoint(float x, float y, float p) {
        if (frames.empty()) frames.resize(1);
        if (currentFrame >= static_cast<int>(frames.size())) frames.resize(currentFrame + 1);
        float pressureValue = pressure ? std::clamp(p, 0.05f, 1.5f) : 1.0f;
        float targetX = x;
        float targetY = y;
        if (drawing) {
            smoothX += (targetX - smoothX) * (1.0f - stabilization);
            smoothY += (targetY - smoothY) * (1.0f - stabilization);
        } else {
            smoothX = targetX;
            smoothY = targetY;
        }
        const float size = brushSize * (0.45f + pressureValue * 0.75f);
        frames[currentFrame].push_back({smoothX, smoothY, size, opacity});
        lastX = smoothX;
        lastY = smoothY;
        drawing = true;
    }

    void begin(float x, float y, float p) {
        std::lock_guard<std::mutex> lock(mutex);
        drawing = false;
        addPoint(x, y, p);
    }

    void move(float x, float y, float p) {
        std::lock_guard<std::mutex> lock(mutex);
        const float dx = x - lastX;
        const float dy = y - lastY;
        const float distance = std::sqrt(dx * dx + dy * dy);
        const int steps = std::max(1, static_cast<int>(distance / 3.0f));
        for (int i = 1; i <= steps; ++i) {
            const float t = static_cast<float>(i) / static_cast<float>(steps);
            addPoint(lastX + dx * t, lastY + dy * t, p);
        }
    }

    void end() {
        std::lock_guard<std::mutex> lock(mutex);
        drawing = false;
    }

    void clearFrame() {
        std::lock_guard<std::mutex> lock(mutex);
        if (currentFrame >= static_cast<int>(frames.size())) frames.resize(currentFrame + 1);
        frames[currentFrame].clear();
    }
};

static const char* vertexShader = R"(
#version 300 es
precision highp float;
layout(location=0) in vec2 aPosition;
layout(location=1) in float aSize;
layout(location=2) in float aAlpha;
out float vAlpha;
void main() {
    float x = (aPosition.x / 640.0) - 1.0;
    float y = 1.0 - (aPosition.y / 360.0);
    gl_Position = vec4(x, y, 0.0, 1.0);
    gl_PointSize = aSize;
    vAlpha = aAlpha;
}
)";

static const char* fragmentShader = R"(
#version 300 es
precision mediump float;
in float vAlpha;
uniform vec4 uColor;
out vec4 fragColor;
void main() {
    vec2 p = gl_PointCoord * 2.0 - 1.0;
    float d = dot(p, p);
    if (d > 1.0) discard;
    float edge = 1.0 - smoothstep(0.72, 1.0, d);
    fragColor = vec4(uColor.rgb, uColor.a * vAlpha * edge);
}
)";

GLuint compileShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    GLint ok = GL_FALSE;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char log[1024]{};
        glGetShaderInfoLog(shader, sizeof(log), nullptr, log);
        LOGE("shader compile failed: %s", log);
    }
    return shader;
}

GLuint createProgram() {
    GLuint vs = compileShader(GL_VERTEX_SHADER, vertexShader);
    GLuint fs = compileShader(GL_FRAGMENT_SHADER, fragmentShader);
    GLuint program = glCreateProgram();
    glAttachShader(program, vs);
    glAttachShader(program, fs);
    glLinkProgram(program);
    glDeleteShader(vs);
    glDeleteShader(fs);
    return program;
}

GLuint program = 0;
GLint colorUniform = -1;
Engine* engineFrom(jlong handle) { return reinterpret_cast<Engine*>(handle); }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_animakerpro_app_NativeCanvasView_nativeCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new Engine());
}

extern "C" JNIEXPORT void JNICALL
Java_com_animakerpro_app_NativeCanvasView_nativeDestroy(JNIEnv*, jobject, jlong handle) {
    delete engineFrom(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_animakerpro_app_NativeCanvasView_nativeResize(JNIEnv*, jobject, jlong handle, jint width, jint height) {
    Engine* e = engineFrom(handle);
    e->resize(width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_animakerpro_app_NativeCanvasView_nativeRender(JNIEnv*, jobject, jlong handle) {
    Engine* e = engineFrom(handle);
    if (program == 0) {
        program = createProgram();
        colorUniform = glGetUniformLocation(program, "uColor");
    }

    glViewport(0, 0, e->width, e->height);
    glClearColor(0.94f, 0.94f, 0.94f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);

    std::lock_guard<std::mutex> lock(e->mutex);
    if (e->currentFrame >= static_cast<int>(e->frames.size())) return;
    const auto& points = e->frames[e->currentFrame];
    if (points.empty()) return;

    std::vector<float> vertices;
    vertices.reserve(points.size() * 4);
    const float sx = 1280.0f / static_cast<float>(e->width);
    const float sy = 720.0f / static_cast<float>(e->height);
    for (const auto& p : points) {
        vertices.push_back(p.x * sx);
        vertices.push_back(p.y * sy);
        vertices.push_back(p.size);
        vertices.push_back(p.alpha);
    }

    glUseProgram(program);
    glUniform4f(colorUniform, e->colorR, e->colorG, e->colorB, 1.0f);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glEnableVertexAttribArray(0);
    glEnableVertexAttribArray(1);
    glEnableVertexAttribArray(2);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), vertices.data());
    glVertexAttribPointer(1, 1, GL_FLOAT, GL_FALSE, 4 * sizeof(float), vertices.data() + 2);
    glVertexAttribPointer(2, 1, GL_FLOAT, GL_FALSE, 4 * sizeof(float), vertices.data() + 3);
    glDrawArrays(GL_POINTS, 0, static_cast<GLsizei>(points.size()));
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glDisableVertexAttribArray(2);
}

extern "C" JNIEXPORT void JNICALL
Java_com_animakerpro_app_NativeCanvasView_nativeBeginStroke(JNIEnv*, jobject, jlong handle, jfloat x, jfloat y, jfloat pressure) {
    engineFrom(handle)->begin(x, y, pressure);
}

extern "C" JNIEXPORT void JNICALL
Java_com_animakerpro_app_NativeCanvasView_nativeMoveStroke(JNIEnv*, jobject, jlong handle, jfloat x, jfloat y, jfloat pressure) {
    engineFrom(handle)->move(x, y, pressure);
}

extern "C" JNIEXPORT void JNICALL
Java_com_animakerpro_app_NativeCanvasView_nativeEndStroke(JNIEnv*, jobject, jlong handle) {
    engineFrom(handle)->end();
}

extern "C" JNIEXPORT void JNICALL
Java_com_animakerpro_app_NativeCanvasView_nativeSetBrush(JNIEnv*, jobject, jlong handle, jfloat size, jfloat opacity, jboolean pressure, jfloat stabilization) {
    Engine* e = engineFrom(handle);
    std::lock_guard<std::mutex> lock(e->mutex);
    e->brushSize = std::clamp(size, 1.0f, 200.0f);
    e->opacity = std::clamp(opacity, 0.01f, 1.0f);
    e->pressure = pressure;
    e->stabilization = std::clamp(stabilization, 0.0f, 0.9f);
}

extern "C" JNIEXPORT void JNICALL
Java_com_animakerpro_app_NativeCanvasView_nativeSetFrame(JNIEnv*, jobject, jlong handle, jint frame) {
    engineFrom(handle)->setFrame(frame);
}

extern "C" JNIEXPORT void JNICALL
Java_com_animakerpro_app_NativeCanvasView_nativeClearFrame(JNIEnv*, jobject, jlong handle) {
    engineFrom(handle)->clearFrame();
}
