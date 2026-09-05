#include <jni.h>
#include <android/log.h>
#include <GLES3/gl3.h>
#include <algorithm>
#include <cmath>
#include <mutex>
#include <vector>

#define LOG_TAG "AnimakerProNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
struct Point { float x, y, size, alpha; };

struct Engine {
    std::mutex mutex;
    int width = 1, height = 1, currentFrame = 0;
    float brushSize = 8.0f, opacity = 1.0f;
    float colorR = 0.02f, colorG = 0.02f, colorB = 0.02f;
    bool pressure = true, drawing = false;
    float stabilization = 0.35f;
    float lastX = 0.0f, lastY = 0.0f, smoothX = 0.0f, smoothY = 0.0f;
    float zoom = 1.0f, panX = 0.0f, panY = 0.0f, rotation = 0.0f;
    std::vector<std::vector<Point>> frames;

    Engine() : frames(1) {}

    void setFrame(int frame) {
        std::lock_guard<std::mutex> lock(mutex);
        currentFrame = std::max(0, frame);
        if (currentFrame >= static_cast<int>(frames.size())) frames.resize(currentFrame + 1);
    }
    void resize(int w, int h) {
        width = std::max(1, w); height = std::max(1, h);
        glViewport(0, 0, width, height);
    }
    void addPoint(float x, float y, float p) {
        if (frames.empty()) frames.resize(1);
        if (currentFrame >= static_cast<int>(frames.size())) frames.resize(currentFrame + 1);
        const float pressureValue = pressure ? std::clamp(p, 0.05f, 1.5f) : 1.0f;
        if (drawing) {
            smoothX += (x - smoothX) * (1.0f - stabilization);
            smoothY += (y - smoothY) * (1.0f - stabilization);
        } else { smoothX = x; smoothY = y; }
        const float size = brushSize * (0.45f + pressureValue * 0.75f);
        frames[currentFrame].push_back({smoothX, smoothY, size, opacity});
        lastX = smoothX; lastY = smoothY; drawing = true;
    }
    void begin(float x, float y, float p) {
        std::lock_guard<std::mutex> lock(mutex); drawing = false; addPoint(x, y, p);
    }
    void move(float x, float y, float p) {
        std::lock_guard<std::mutex> lock(mutex);
        const float dx = x - lastX, dy = y - lastY;
        const float distance = std::sqrt(dx * dx + dy * dy);
        const int steps = std::max(1, static_cast<int>(distance / 3.0f));
        const float startX = lastX, startY = lastY;
        for (int i = 1; i <= steps; ++i) {
            const float t = static_cast<float>(i) / static_cast<float>(steps);
            addPoint(startX + dx * t, startY + dy * t, p);
        }
    }
    void end() { std::lock_guard<std::mutex> lock(mutex); drawing = false; }
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
uniform vec2 uViewport;
uniform float uZoom;
uniform vec2 uPan;
uniform float uRotation;
out float vAlpha;
void main() {
    vec2 p = aPosition - uViewport * 0.5;
    float c = cos(uRotation), s = sin(uRotation);
    p = mat2(c, -s, s, c) * p * uZoom + uPan;
    vec2 ndc = p / (uViewport * 0.5);
    gl_Position = vec4(ndc.x, -ndc.y, 0.0, 1.0);
    gl_PointSize = aSize * uZoom;
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
    glShaderSource(shader, 1, &source, nullptr); glCompileShader(shader);
    GLint ok = GL_FALSE; glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) { char log[1024]{}; glGetShaderInfoLog(shader, sizeof(log), nullptr, log); LOGE("shader: %s", log); }
    return shader;
}
GLuint createProgram() {
    GLuint vs = compileShader(GL_VERTEX_SHADER, vertexShader), fs = compileShader(GL_FRAGMENT_SHADER, fragmentShader);
    GLuint p = glCreateProgram(); glAttachShader(p, vs); glAttachShader(p, fs); glLinkProgram(p);
    glDeleteShader(vs); glDeleteShader(fs); return p;
}
GLuint program = 0;
GLint colorUniform = -1, viewportUniform = -1, zoomUniform = -1, panUniform = -1, rotationUniform = -1;
Engine* engineFrom(jlong handle) { return reinterpret_cast<Engine*>(handle); }
}

extern "C" JNIEXPORT jlong JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeCreate(JNIEnv*, jobject) { return reinterpret_cast<jlong>(new Engine()); }
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeDestroy(JNIEnv*, jobject, jlong handle) { delete engineFrom(handle); }
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeResize(JNIEnv*, jobject, jlong handle, jint w, jint h) { engineFrom(handle)->resize(w, h); }

extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeRender(JNIEnv*, jobject, jlong handle) {
    Engine* e = engineFrom(handle);
    if (program == 0) {
        program = createProgram();
        colorUniform = glGetUniformLocation(program, "uColor");
        viewportUniform = glGetUniformLocation(program, "uViewport");
        zoomUniform = glGetUniformLocation(program, "uZoom");
        panUniform = glGetUniformLocation(program, "uPan");
        rotationUniform = glGetUniformLocation(program, "uRotation");
    }
    glViewport(0, 0, e->width, e->height); glClearColor(0.94f, 0.94f, 0.94f, 1.0f); glClear(GL_COLOR_BUFFER_BIT);
    std::lock_guard<std::mutex> lock(e->mutex);
    if (e->currentFrame >= static_cast<int>(e->frames.size())) return;
    const auto& points = e->frames[e->currentFrame]; if (points.empty()) return;
    std::vector<float> vertices; vertices.reserve(points.size() * 4);
    for (const auto& p : points) { vertices.push_back(p.x); vertices.push_back(p.y); vertices.push_back(p.size); vertices.push_back(p.alpha); }
    glUseProgram(program);
    glUniform4f(colorUniform, e->colorR, e->colorG, e->colorB, 1.0f);
    glUniform2f(viewportUniform, static_cast<float>(e->width), static_cast<float>(e->height));
    glUniform1f(zoomUniform, e->zoom); glUniform2f(panUniform, e->panX, e->panY); glUniform1f(rotationUniform, e->rotation);
    glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnableVertexAttribArray(0); glEnableVertexAttribArray(1); glEnableVertexAttribArray(2);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4*sizeof(float), vertices.data());
    glVertexAttribPointer(1, 1, GL_FLOAT, GL_FALSE, 4*sizeof(float), vertices.data()+2);
    glVertexAttribPointer(2, 1, GL_FLOAT, GL_FALSE, 4*sizeof(float), vertices.data()+3);
    glDrawArrays(GL_POINTS, 0, static_cast<GLsizei>(points.size()));
    glDisableVertexAttribArray(0); glDisableVertexAttribArray(1); glDisableVertexAttribArray(2);
}

extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeBeginStroke(JNIEnv*, jobject, jlong h, jfloat x, jfloat y, jfloat p) { engineFrom(h)->begin(x,y,p); }
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeMoveStroke(JNIEnv*, jobject, jlong h, jfloat x, jfloat y, jfloat p) { engineFrom(h)->move(x,y,p); }
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeEndStroke(JNIEnv*, jobject, jlong h) { engineFrom(h)->end(); }
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeSetBrush(JNIEnv*, jobject, jlong h, jfloat size, jfloat opacity, jboolean pressure, jfloat stabilization) {
    Engine* e = engineFrom(h); std::lock_guard<std::mutex> lock(e->mutex);
    e->brushSize = std::clamp(size,1.0f,200.0f); e->opacity = std::clamp(opacity,0.01f,1.0f); e->pressure = pressure; e->stabilization = std::clamp(stabilization,0.0f,0.9f);
}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeSetFrame(JNIEnv*, jobject, jlong h, jint frame) { engineFrom(h)->setFrame(frame); }
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeClearFrame(JNIEnv*, jobject, jlong h) { engineFrom(h)->clearFrame(); }
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeSetTransform(JNIEnv*, jobject, jlong h, jfloat zoom, jfloat panX, jfloat panY, jfloat rotation) {
    Engine* e = engineFrom(h); std::lock_guard<std::mutex> lock(e->mutex);
    e->zoom = std::clamp(zoom,0.15f,8.0f); e->panX = panX; e->panY = panY; e->rotation = rotation;
}
