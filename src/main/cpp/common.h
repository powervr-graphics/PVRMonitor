/*!****************************************************************************

 @file         common.h
 @copyright    Copyright (c) Imagination Technologies Limited.
 @brief        A collection of helper functions and configuration information.

******************************************************************************/
#ifndef _COMMON_H_
#define _COMMON_H_

#include <android/log.h>
// Output definitions
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "PVRScopeHUD", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "PVRScopeHUD", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "PVRScopeHUD", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "PVRScopeHUD", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "PVRScopeHUD", __VA_ARGS__)

// Disable not use PVRScope
#define DISABLE_PVRSCOPE 0
// Output counter values as text
#define DUMP_PVRSCOPE_DATA 0
// Output CPU values as text
#define DUMP_CPU_DATA 0

// Used to determine how many empty readings should we do to PVRScope
// Data polling interval (from the Java layer)
#define SOCKET_READ_INTERVAL_MS 100
// The manual says that the poll interval should be 33ms
#define POLL_INTERVAL_MS 33

#endif
