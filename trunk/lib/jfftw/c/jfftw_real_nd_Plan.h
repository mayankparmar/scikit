/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class jfftw_real_nd_Plan */

#ifndef _Included_jfftw_real_nd_Plan
#define _Included_jfftw_real_nd_Plan
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     jfftw_real_nd_Plan
 * Method:    createPlan
 * Signature: ([III)V
 */
JNIEXPORT void JNICALL Java_jfftw_real_nd_Plan_createPlan
  (JNIEnv *, jobject, jintArray, jint, jint);

/*
 * Class:     jfftw_real_nd_Plan
 * Method:    destroyPlan
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jfftw_real_nd_Plan_destroyPlan
  (JNIEnv *, jobject);

/*
 * Class:     jfftw_real_nd_Plan
 * Method:    transform
 * Signature: ([D)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_jfftw_real_nd_Plan_transform___3D
  (JNIEnv *, jobject, jdoubleArray);

/*
 * Class:     jfftw_real_nd_Plan
 * Method:    transform
 * Signature: (I[DII[DII)V
 */
JNIEXPORT void JNICALL Java_jfftw_real_nd_Plan_transform__I_3DII_3DII
  (JNIEnv *, jobject, jint, jdoubleArray, jint, jint, jdoubleArray, jint, jint);

#ifdef __cplusplus
}
#endif
#endif
