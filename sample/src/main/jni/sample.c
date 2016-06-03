#include "jni.h"
#include <string.h>

//
// Created by lake on 2016.04.05
//
JNIEXPORT void JNICALL Java_me_lake_librestreaming_sample_softfilter_BlackWhiteFilterSoft_BlackWhite
(JNIEnv * env, jobject thiz, jbyteArray srcarray,jint ySize,jint totalSize,jbyte gap) {
	unsigned char *src = (unsigned char *)(*env)->GetByteArrayElements(env,srcarray, 0);
	unsigned char igap = gap;
	int i;
	for (i = 0; i < ySize; i++) {
		if (src[i] > igap) {
			src[i] =  0xFF;
		} else {
			src[i] =  0x00;
		}
	}
	for (i = ySize; i < totalSize; i++) {
		src[i] = 127;
	}
	(*env)->ReleaseByteArrayElements(env,srcarray,src,JNI_ABORT);
	return;
}