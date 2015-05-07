LOCAL_PATH := $(call my-dir)
PVRSDKDIR := $(realpath $(LOCAL_PATH)/../../..)

APP_PLATFORM := android-10

# Module PVRScopeDeveloper
include $(CLEAR_VARS)
LOCAL_MODULE := PVRScopeDeveloper
LOCAL_SRC_FILES := $(PVRSDKDIR)/Builds/Android/$(TARGET_ARCH_ABI)/Lib/libPVRScopeDeveloper.a
include $(PREBUILT_STATIC_LIBRARY)

# Module PVRMonitor
include $(CLEAR_VARS)

LOCAL_MODULE:= PVRScopeHUD
LOCAL_MODULE_TAGS:= optional

LOCAL_SRC_FILES:= \
				main.cpp \
				PVRScopeHUD.cpp	\
				CPUMetrics.cpp 
 
LOCAL_C_INCLUDES :=	$(PVRSDKDIR)/Builds/Include

LOCAL_LDLIBS :=  \
				-ldl \
				-llog 
				
LOCAL_STATIC_LIBRARIES := PVRScopeDeveloper 
				
LOCAL_LDFLAGS += -Wl,--no-undefined

include $(BUILD_SHARED_LIBRARY)
