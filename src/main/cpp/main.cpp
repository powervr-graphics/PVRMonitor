/******************************************************************************

 @File         main.cpp

 @Title        main

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Entry point for native code execution.

******************************************************************************/
#include <unistd.h>
#include <string>
#include <jni.h>

#include "common.h"
#include <pthread.h>
#include "PVRScopeHUD.h"
#include "CPUMetrics.h"

#if defined(__cplusplus)
extern "C"
#endif
{

PVRScopeHUD *pPVRScopeHUD = NULL;
CPUMetrics *pCPUMetrics = NULL;

jintArray Java_com_powervr_PVRMonitor_PVRScopeBridge_returnCPUMetrics (JNIEnv *env, jobject thiz) {


    //Update the CPU usage
    pCPUMetrics->updateCPU();

    // Send the CPUMetrics
    unsigned int nNumCores = pCPUMetrics->getNumCores();

    //-- No error handling is being done...
    jint* buf = new jint[nNumCores];
    memset (buf, 0, nNumCores);

    for (unsigned int i = 0; i < nNumCores; ++i)
    {
        float load = pCPUMetrics->getCPULoad(i);
        buf[i] = (unsigned char) (load + 0.5);
    }

    jintArray ret = env->NewIntArray(nNumCores);
    env->SetIntArrayRegion (ret, 0, nNumCores, buf);

    delete[] buf;
    return ret;
}

jfloatArray
Java_com_powervr_PVRMonitor_PVRScopeBridge_returnPVRScope (JNIEnv *env, jobject thiz)
{
    size_t numCounters  = 0;

#if !DISABLE_PVRSCOPE
    pPVRScopeHUD->readCounters(true);

    numCounters = pPVRScopeHUD->getNumCounters();
    //LOGE("Number of counters: %d", (int)numCounters);

    jfloat * buf = new jfloat[numCounters];
    memset (buf, 0, numCounters);

    for(unsigned int i = 0; i < numCounters; i++)
    {
        float temp = pPVRScopeHUD->getCounter(i);
        buf[i] = temp;
    }
#endif

    jfloatArray ret = env->NewFloatArray((jsize)numCounters);
    env->SetFloatArrayRegion (ret, 0, (jsize)numCounters, buf);

    delete[] buf;
    return ret;
}

jobjectArray
Java_com_powervr_PVRMonitor_PVRScopeBridge_returnPVRScopeStrings (JNIEnv *env, jobject thiz)
{
    size_t numCounters  = 0;
#if !DISABLE_PVRSCOPE
    pPVRScopeHUD->readCounters(true);


    numCounters = pPVRScopeHUD->getNumCounters();

    jobjectArray ret = env->NewObjectArray((jsize)numCounters, env->FindClass("java/lang/String"), 0);
    for(unsigned int i = 0; i < numCounters; i++)
    {
        const char* temp = pPVRScopeHUD->getName(i).c_str();
            jstring str = env->NewStringUTF(temp);
            env->SetObjectArrayElement(ret, i, str);

    }
#endif
    return ret;
}

jboolean
Java_com_powervr_PVRMonitor_PVRScopeBridge_initPVRScope( JNIEnv* env,
                                                         jobject thiz )
{
    //Initialize the metrics
    pCPUMetrics = new CPUMetrics();

    //Initialize PVRScope
#if !DISABLE_PVRSCOPE
    pPVRScopeHUD = new PVRScopeHUD();
    if (!pPVRScopeHUD->initialisePVRScope())
    {
        LOGE("Error: cannot connect to PVRScope.\n");
        return false;
    }
    LOGI("Initialised PVRScope");
#else
    return false;
#endif

    return true;
}

jboolean
Java_com_powervr_PVRMonitor_PVRScopeBridge_deinitPVRScope( JNIEnv* env,
                                                           jobject thiz )
{
#if !DISABLE_PVRSCOPE
    //De-initialize PVRScope
    pPVRScopeHUD->deinitialisePVRScope();

    delete(pPVRScopeHUD);
    pPVRScopeHUD = NULL;
#endif

    return true;
}

}
