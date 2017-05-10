LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


#OPENCV_CAMERA_MODULES:=off
#OPENCV_INSTALL_MODULE:=off
#OPEN_LIB_TYPE:=SHARED

include native/jni/OpenCV.mk

LOCAL_SRC_FILES := jni_part.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS +=  -llog -ldl

LOCAL_MODULE := native_sample

include $(BUILD_SHARED_LIBRARY)
