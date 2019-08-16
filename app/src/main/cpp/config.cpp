#include <jni.h>

#include "private.h"

extern "C" {

JNIEXPORT jstring JNICALL Java_com_demo_pet_petapp_conversations_KatchyOnline_getKatchyOnlineId(
        JNIEnv *env,
        jobject thiz) {
    return env->NewStringUTF(KATCHY_ONLINE_CLIENT_ID);
}

JNIEXPORT jstring JNICALL Java_com_demo_pet_petapp_conversations_KatchyOnline_getKatchyOnlineSec(
        JNIEnv *env,
        jobject thiz) {
    return env->NewStringUTF(KATCHY_ONLINE_CLIENT_SECRET);
}

JNIEXPORT jstring JNICALL Java_com_demo_pet_petapp_conversations_KatchyOnline_getKatchyOnlineRefresh(
        JNIEnv *env,
        jobject thiz) {
    return env->NewStringUTF(KATCHY_ONLINE_REFRESH_TOKEN);
}

} // extern "C"
