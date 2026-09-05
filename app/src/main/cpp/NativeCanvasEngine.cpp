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
    int width=1,height=1,currentFrame=0;
    float brushSize=8.0f,opacity=1.0f,colorR=0.02f,colorG=0.02f,colorB=0.02f;
    bool pressure=true,drawing=false,onionSkin=true;
    float stabilization=0.35f,lastX=0,lastY=0,smoothX=0,smoothY=0;
    float zoom=1,panX=0,panY=0,rotation=0;
    std::vector<std::vector<Point>> frames;
    Engine():frames(1){}
    void setFrame(int f){std::lock_guard<std::mutex>l(mutex);currentFrame=std::max(0,f);if(currentFrame>=static_cast<int>(frames.size()))frames.resize(currentFrame+1);}
    void resize(int w,int h){width=std::max(1,w);height=std::max(1,h);glViewport(0,0,width,height);}
    void addPoint(float x,float y,float p){
        if(frames.empty())frames.resize(1);if(currentFrame>=static_cast<int>(frames.size()))frames.resize(currentFrame+1);
        float pv=pressure?std::clamp(p,.05f,1.5f):1.0f;
        if(drawing){smoothX+=(x-smoothX)*(1.0f-stabilization);smoothY+=(y-smoothY)*(1.0f-stabilization);}else{smoothX=x;smoothY=y;}
        frames[currentFrame].push_back({smoothX,smoothY,brushSize*(.45f+pv*.75f),opacity});lastX=smoothX;lastY=smoothY;drawing=true;
    }
    void begin(float x,float y,float p){std::lock_guard<std::mutex>l(mutex);drawing=false;addPoint(x,y,p);}
    void move(float x,float y,float p){std::lock_guard<std::mutex>l(mutex);float dx=x-lastX,dy=y-lastY,d=std::sqrt(dx*dx+dy*dy);int n=std::max(1,(int)(d/3.0f));float sx=lastX,sy=lastY;for(int i=1;i<=n;i++){float t=(float)i/n;addPoint(sx+dx*t,sy+dy*t,p);}}
    void end(){std::lock_guard<std::mutex>l(mutex);drawing=false;}
    void clearFrame(){std::lock_guard<std::mutex>l(mutex);if(currentFrame>=static_cast<int>(frames.size()))frames.resize(currentFrame+1);frames[currentFrame].clear();}
    void ensure(int count){std::lock_guard<std::mutex>l(mutex);if(count>static_cast<int>(frames.size()))frames.resize(count);}
    void duplicate(int src,int insert){std::lock_guard<std::mutex>l(mutex);if(src<0||src>=static_cast<int>(frames.size()))return;insert=std::clamp(insert,0,(int)frames.size());frames.insert(frames.begin()+insert,frames[src]);currentFrame=insert;}
    void remove(int index){std::lock_guard<std::mutex>l(mutex);if(frames.size()<=1||index<0||index>=static_cast<int>(frames.size()))return;frames.erase(frames.begin()+index);currentFrame=std::min(currentFrame,(int)frames.size()-1);}
    void loop(int start,int end){std::lock_guard<std::mutex>l(mutex);if(start<0||end>=static_cast<int>(frames.size())||start>end)return;std::vector<std::vector<Point>>copy;for(int i=start;i<=end;i++)copy.push_back(frames[i]);frames.insert(frames.end(),copy.begin(),copy.end());currentFrame=(int)frames.size()-1;}
};
static const char* vs=R"(
#version 300 es
precision highp float;layout(location=0)in vec2 aPosition;layout(location=1)in float aSize;layout(location=2)in float aAlpha;
uniform vec2 uViewport;uniform float uZoom;uniform vec2 uPan;uniform float uRotation;out float vAlpha;
void main(){vec2 p=aPosition-uViewport*.5;float c=cos(uRotation),s=sin(uRotation);p=mat2(c,-s,s,c)*p*uZoom+uPan;vec2 ndc=p/(uViewport*.5);gl_Position=vec4(ndc.x,-ndc.y,0,1);gl_PointSize=aSize*uZoom;vAlpha=aAlpha;})";
static const char* fs=R"(
#version 300 es
precision mediump float;in float vAlpha;uniform vec4 uColor;out vec4 fragColor;
void main(){vec2 p=gl_PointCoord*2.-1.;float d=dot(p,p);if(d>1.)discard;float edge=1.-smoothstep(.72,1.,d);fragColor=vec4(uColor.rgb,uColor.a*vAlpha*edge);})";
GLuint compileShader(GLenum t,const char*s){GLuint sh=glCreateShader(t);glShaderSource(sh,1,&s,nullptr);glCompileShader(sh);GLint ok=0;glGetShaderiv(sh,GL_COMPILE_STATUS,&ok);if(!ok){char log[1024]{};glGetShaderInfoLog(sh,sizeof(log),nullptr,log);LOGE("shader: %s",log);}return sh;}
GLuint createProgram(){GLuint a=compileShader(GL_VERTEX_SHADER,vs),b=compileShader(GL_FRAGMENT_SHADER,fs),p=glCreateProgram();glAttachShader(p,a);glAttachShader(p,b);glLinkProgram(p);glDeleteShader(a);glDeleteShader(b);return p;}
GLuint program=0;GLint colorU=-1,viewportU=-1,zoomU=-1,panU=-1,rotationU=-1;Engine*E(jlong h){return reinterpret_cast<Engine*>(h);}
void drawPoints(const std::vector<Point>&pts,Engine*e){if(pts.empty())return;std::vector<float>v;v.reserve(pts.size()*4);for(auto&p:pts){v.push_back(p.x);v.push_back(p.y);v.push_back(p.size);v.push_back(p.alpha);}glEnableVertexAttribArray(0);glEnableVertexAttribArray(1);glEnableVertexAttribArray(2);glVertexAttribPointer(0,2,GL_FLOAT,GL_FALSE,4*sizeof(float),v.data());glVertexAttribPointer(1,1,GL_FLOAT,GL_FALSE,4*sizeof(float),v.data()+2);glVertexAttribPointer(2,1,GL_FLOAT,GL_FALSE,4*sizeof(float),v.data()+3);glDrawArrays(GL_POINTS,0,(GLsizei)pts.size());glDisableVertexAttribArray(0);glDisableVertexAttribArray(1);glDisableVertexAttribArray(2);}
}
extern "C" JNIEXPORT jlong JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeCreate(JNIEnv*,jobject){return(jlong)new Engine();}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeDestroy(JNIEnv*,jobject,jlong h){delete E(h);}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeResize(JNIEnv*,jobject,jlong h,jint w,jint hgt){E(h)->resize(w,hgt);}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeRender(JNIEnv*,jobject,jlong h){
 Engine*e=E(h);if(program==0){program=createProgram();colorU=glGetUniformLocation(program,"uColor");viewportU=glGetUniformLocation(program,"uViewport");zoomU=glGetUniformLocation(program,"uZoom");panU=glGetUniformLocation(program,"uPan");rotationU=glGetUniformLocation(program,"uRotation");}
 glViewport(0,0,e->width,e->height);glClearColor(.94f,.94f,.94f,1);glClear(GL_COLOR_BUFFER_BIT);std::lock_guard<std::mutex>l(e->mutex);if(e->currentFrame>=(int)e->frames.size())return;
 glUseProgram(program);glUniform4f(colorU,e->colorR,e->colorG,e->colorB,1);glUniform2f(viewportU,(float)e->width,(float)e->height);glUniform1f(zoomU,e->zoom);glUniform2f(panU,e->panX,e->panY);glUniform1f(rotationU,e->rotation);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
 if(e->onionSkin&&e->currentFrame>0){glUniform4f(colorU,.9f,.2f,.25f,.28f);drawPoints(e->frames[e->currentFrame-1],e);}glUniform4f(colorU,e->colorR,e->colorG,e->colorB,1);drawPoints(e->frames[e->currentFrame],e);
}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeBeginStroke(JNIEnv*,jobject,jlong h,jfloat x,jfloat y,jfloat p){E(h)->begin(x,y,p);}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeMoveStroke(JNIEnv*,jobject,jlong h,jfloat x,jfloat y,jfloat p){E(h)->move(x,y,p);}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeEndStroke(JNIEnv*,jobject,jlong h){E(h)->end();}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeSetBrush(JNIEnv*,jobject,jlong h,jfloat size,jfloat op,jboolean press,jfloat stab){Engine*e=E(h);std::lock_guard<std::mutex>l(e->mutex);e->brushSize=std::clamp(size,1.f,200.f);e->opacity=std::clamp(op,.01f,1.f);e->pressure=press;e->stabilization=std::clamp(stab,0.f,.9f);}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeSetFrame(JNIEnv*,jobject,jlong h,jint f){E(h)->setFrame(f);}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeClearFrame(JNIEnv*,jobject,jlong h){E(h)->clearFrame();}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeSetTransform(JNIEnv*,jobject,jlong h,jfloat z,jfloat x,jfloat y,jfloat r){Engine*e=E(h);std::lock_guard<std::mutex>l(e->mutex);e->zoom=std::clamp(z,.15f,8.f);e->panX=x;e->panY=y;e->rotation=r;}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeSetOnionSkin(JNIEnv*,jobject,jlong h,jboolean enabled){Engine*e=E(h);std::lock_guard<std::mutex>l(e->mutex);e->onionSkin=enabled;}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeEnsureFrames(JNIEnv*,jobject,jlong h,jint count){E(h)->ensure(std::max(1,count));}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeDuplicateFrame(JNIEnv*,jobject,jlong h,jint src,jint insert){E(h)->duplicate(src,insert);}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeRemoveFrame(JNIEnv*,jobject,jlong h,jint index){E(h)->remove(index);}
extern "C" JNIEXPORT void JNICALL Java_com_animakerpro_app_NativeCanvasView_nativeMakeLoop(JNIEnv*,jobject,jlong h,jint start,jint end){E(h)->loop(start,end);}
