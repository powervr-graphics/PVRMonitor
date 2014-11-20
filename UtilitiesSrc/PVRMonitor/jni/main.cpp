/******************************************************************************

 @File         main.cpp

 @Title        main

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Entry point for native code execution.

******************************************************************************/
#include <unistd.h>
#include <string.h>
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

jbyteArray Java_com_powervr_PVRMonitor_PVRScopeBridge_returnCPUMetrics (JNIEnv *env, jobject thiz) {


       //Update the CPU usage
       pCPUMetrics->updateCPU();

       // Send the CPUMetrics
       unsigned int nNumCores = pCPUMetrics->getNumCores();

       //-- No error handling is being done...
       jbyte* buf = new jbyte[nNumCores];
       memset (buf, 0, nNumCores);

       for (unsigned int i = 0; i < nNumCores; ++i)
       {
    	   float load = pCPUMetrics->getCPULoad(i);
    	   buf[i] = (unsigned char) (load + 0.5);
       }

       jbyteArray ret = env->NewByteArray(nNumCores);
       env->SetByteArrayRegion (ret, 0, nNumCores, buf);

       delete[] buf;
       return ret;
}

jbyteArray
Java_com_powervr_PVRMonitor_PVRScopeBridge_returnPVRScope (JNIEnv *env, jobject thiz)
{
       jbyte* buf = new jbyte[8];
       memset (buf, 0, 8);

#if !DISABLE_PVRSCOPE
       pPVRScopeHUD->readCounters(true);

	   unsigned short *pBuff = (unsigned short *) buf;
	   pBuff[0] = (unsigned short) (pPVRScopeHUD->getCounter(eFPS) + 0.5);

	   buf[2] = (unsigned char) (pPVRScopeHUD->getCounter(e2DLoad) + 0.5);
	   buf[3] = (unsigned char) (pPVRScopeHUD->getCounter(e3DLoad) + 0.5);
	   buf[4] = (unsigned char) (pPVRScopeHUD->getCounter(eTALoad) + 0.5);
	   buf[5] = (unsigned char) (pPVRScopeHUD->getCounter(eTSPLoad) + 0.5);
	   buf[6] = (unsigned char) (pPVRScopeHUD->getCounter(ePixelLoad) + 0.5);
	   buf[7] = (unsigned char) (pPVRScopeHUD->getCounter(eVertexLoad) + 0.5);
#endif

       jbyteArray ret = env->NewByteArray(8);
       env->SetByteArrayRegion (ret, 0, 8, buf);

       delete[] buf;
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
