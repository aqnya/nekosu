#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "Nekosu"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jstring JNICALL
Java_me.neko.nksu_MainActivity_stringFromJNI(
    JNIEnv* env,
    jobject /* this */) {
    std::string hello = "Hello from C++";
    LOGI("Native library loaded successfully");
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jint JNICALL
Java_me.neko.nksu_MainActivity_calculateFromJNI(
    JNIEnv* env,
    jobject /* this */,
    jint a,
    jint b) {
    LOGI("Calculating with values: %d and %d", a, b);
    return a * b + 42; // Example calculation
}

}