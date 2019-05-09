/******************************************************************************

 @File         PVRScopeBridge.java

 @Title        PVRScopeBridge

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  The bridge between Java and native code.

 ******************************************************************************/
package com.powervr.PVRMonitor;
import java.util.Arrays;
import java.util.Vector;

public class PVRScopeBridge {
    // Native functions
    public native boolean initPVRScope();
    public native boolean deinitPVRScope();

    private native int[] returnCPUMetrics();
    private native float[] returnPVRScope();

    private native String[] returnPVRScopeStrings();

    private int mNumberCounters = 0;
    private int mInitialNumCounters = 0;
    private int mMonitorFPSIndex = 0;
    private int mTopFPSIndex = 0;

    private Vector<String> mScopeStrings = new Vector<>();

    static {
        System.loadLibrary("PVRScopeHUD");
    }

    public void readData(HUDView mView, String PID, boolean scopeDisabled) {
        if (mView == null) return;

        int dataSubset[] = null;
        PVRScopeReading pvrScopeReading = null;
        CPUMetricsReading cpuMetricsReading = null;

        // Get the CPU metrics
        dataSubset = returnCPUMetrics();
        if (dataSubset.length > 0) {
            cpuMetricsReading = new CPUMetricsReading(dataSubset, dataSubset.length);
            mView.addCPUMetricsReading(cpuMetricsReading);
        }

        if(!scopeDisabled) {
            // Get the PVRScope data
            float scopeDataSubset[] = null;
            scopeDataSubset = returnPVRScope();

            if (mNumberCounters != scopeDataSubset.length) {
                //changed. read strings again.
                String[] s = getStrings(true);
                mScopeStrings.ensureCapacity(s.length);
                mScopeStrings.clear();

                mScopeStrings.addAll(Arrays.asList(s));
            }

            mTopFPSIndex = -1;
            float highestFPS = 0.0f;
            for (int i = 0; i < scopeDataSubset.length; i++) {
                //find monitor FPS
                if (mScopeStrings.elementAt(i).contains(PID) || scopeDataSubset[i] == scopeDataSubset[mMonitorFPSIndex] || mMonitorFPSIndex == 0) {
                    mMonitorFPSIndex = i;
                }// ".*\\d.*"

                if (mScopeStrings.elementAt(i).matches(".*\\d+:.*") && i != mMonitorFPSIndex) {

                    if (scopeDataSubset[i] > highestFPS) {
                        highestFPS = scopeDataSubset[i];
                        mTopFPSIndex = i;

                    }
                }
            }

            mNumberCounters = scopeDataSubset.length;

            if (scopeDataSubset.length > 0) {
                pvrScopeReading = new PVRScopeReading(scopeDataSubset, scopeDataSubset.length);
                mView.addPVRScopeReading(pvrScopeReading, mMonitorFPSIndex, mTopFPSIndex);
            }
        }

        if (pvrScopeReading != null || cpuMetricsReading != null) {
            mView.postInvalidate();
        }

    }

    public String[] getStrings(boolean keepFPS) {
        String[] strings = returnPVRScopeStrings();

        if(mInitialNumCounters == 0) {
            mInitialNumCounters = strings.length;
            for (int i = strings.length - 1; i > 0; i--) {
                if (strings[i].contains(": Frames")) {
                    //don't count additional FPS counters.
                    mInitialNumCounters--;
                }
            }
        }

        if(!keepFPS){
            String[] s = new String[mInitialNumCounters];
            for(int i = 0; i < mInitialNumCounters; i++)
                s[i] = strings[i];
            return s;
        }

        return strings;
    }
}
