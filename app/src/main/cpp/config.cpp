#include <jni.h>

#include "private.h"

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
extern "C" {

JNIEXPORT jstring JNICALL Java_com_demo_pet_petapp_conversations_KatchyOnline_getKatchyOnlineId(
        JNIEnv *env,
        jobject __unused thiz) {
    return env->NewStringUTF(KATCHY_ONLINE_CLIENT_ID);
}

JNIEXPORT jstring JNICALL Java_com_demo_pet_petapp_conversations_KatchyOnline_getKatchyOnlineSec(
        JNIEnv *env,
        jobject __unused thiz) {
    return env->NewStringUTF(KATCHY_ONLINE_CLIENT_SECRET);
}

JNIEXPORT jstring JNICALL Java_com_demo_pet_petapp_conversations_KatchyOnline_getKatchyOnlineRefresh(
        JNIEnv *env,
        jobject __unused thiz) {
    return env->NewStringUTF(KATCHY_ONLINE_REFRESH_TOKEN);
}

JNIEXPORT jstring JNICALL Java_com_demo_pet_petapp_tts_TTSControllerGoogleCloudApi_getId(
        JNIEnv *env,
        jobject __unused thiz) {
    return env->NewStringUTF(GCP_TTS_CLIENT_ID);
}

JNIEXPORT jstring JNICALL Java_com_demo_pet_petapp_tts_TTSControllerGoogleCloudApi_getSec(
        JNIEnv *env,
        jobject __unused thiz) {
    return env->NewStringUTF(GCP_TTS_CLIENT_SECRET);
}

JNIEXPORT jstring JNICALL Java_com_demo_pet_petapp_tts_TTSControllerGoogleCloudApi_getRefresh(
        JNIEnv *env,
        jobject __unused thiz) {
    return env->NewStringUTF(GCP_TTS_REFRESH_TOKEN);
}

} // extern "C"

#pragma clang diagnostic pop
