LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := sample.c 

LOCAL_MODULE := sample

include $(BUILD_SHARED_LIBRARY)