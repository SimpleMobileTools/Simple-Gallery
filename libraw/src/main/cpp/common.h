//
// Created by dburc on 1/11/2022.
//

#ifndef USB_CAMERA_VIEWER_UTILS_H
#define USB_CAMERA_VIEWER_UTILS_H
#include <android/log.h>

// Macros for logging
#define LOG_TAG "libraw"
#define LOG(severity, ...) ((void)__android_log_print(ANDROID_LOG_##severity, LOG_TAG, __VA_ARGS__))
#define LOGE(...) LOG(ERROR, __VA_ARGS__)
#define LOGV(...) LOG(VERBOSE, __VA_ARGS__)

// Log an error and return false if condition fails
#define RET_CHECK(condition)                                                    \
    do {                                                                        \
        if ((condition)) {                                                     \
            LOGE("Check failed at %s:%u - %s", __FILE__, __LINE__, #condition); \
            return false;                                                       \
        }                                                                       \
    } while (0)
#endif //USB_CAMERA_VIEWER_UTILS_H
