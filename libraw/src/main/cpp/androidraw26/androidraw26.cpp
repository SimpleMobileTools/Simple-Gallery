//
// Created by dburc on 1/11/2022.
//

#include <jni.h>
#include <../common.h>
#include <../AndroidLibRaw.h>
#include <android/hardware_buffer_jni.h>

extern "C"
JNIEXPORT jboolean JNICALL Java_com_homesoft_photo_libraw_LibRaw26_drawHardwareBuffer(JNIEnv* env, jobject jLibRaw, jobject jHardwareBuffer) {
    auto hardwareBuffer = AHardwareBuffer_fromHardwareBuffer(env, jHardwareBuffer);
    if (hardwareBuffer == nullptr) {
        return false;
    }
    AHardwareBuffer_Desc desc;
    AHardwareBuffer_describe(hardwareBuffer, &desc);
    void* bufferPtr;
    auto libRaw = getLibRaw(env, jLibRaw);
    RET_CHECK(AHardwareBuffer_lock(hardwareBuffer, AHARDWAREBUFFER_USAGE_CPU_WRITE_RARELY, -1, nullptr, &bufferPtr));
    libRaw->copyImage(desc.width, desc.height, desc.stride, desc.format, bufferPtr);
    AHardwareBuffer_unlock(hardwareBuffer, nullptr);
    return true;
}