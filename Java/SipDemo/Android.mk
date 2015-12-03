LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src) 

LOCAL_PACKAGE_NAME := SipDemo
#LOCAL_CERTIFICATE := platform
#LOCAL_PRIVILEGED_MODULE := true

#LOCAL_PROGUARD_ENABLED := optimization
# Workaround for "local variable type mismatch" error.
#LOCAL_DX_FLAGS += --no-locals

LOCAL_JAVA_LIBRARIES := voip-common
#LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
