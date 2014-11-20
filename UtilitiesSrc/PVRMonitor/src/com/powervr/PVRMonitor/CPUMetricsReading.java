/******************************************************************************

 @File         CPUMetricsReading.java

 @Title        CPUMetricsReading

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Provides a Java interface to interact with the native module's
               CPUMetrics data.

******************************************************************************/
package com.powervr.PVRMonitor;

public class CPUMetricsReading implements ValueReading<CPUMetricsReading>{
	public static int mNumCores = 0;
	
	private int mAverageCoreLoad[] = new int[8];
	private int mAverageCPULoad;
    private int mNumberOfReadings = 0;
	
	public CPUMetricsReading(byte[] rawData, int size) {
		if (size == 0) return;
		
		if (mNumCores == 0) mNumCores = size;
		
		mAverageCPULoad = 0;
		
        for (int i = 0; i < 8; ++i) {
        	mAverageCoreLoad[i] = 0;
        }
        
		if (size < mNumCores) return;
        
        for (int i = 0; i < mNumCores; ++i) {
        	mAverageCoreLoad[i] = rawData[i];
        	mAverageCPULoad += rawData[i];
        }
        mAverageCPULoad /= mNumCores;
        
        mNumberOfReadings = 1;
	}
	
	@Override
	public void addReading(CPUMetricsReading reading) {
		if (reading == null) return; 
		
		for (int i = 0; i < mNumCores; ++i) { 
			mAverageCoreLoad[i] += reading.getCoreLoad(i);
		}
		mAverageCPULoad += reading.getCPULoad();
		mNumberOfReadings += reading.getNumberOfReadings();
	}
	
	@Override
	public void addData(byte[] rawData, int size) {
		CPUMetricsReading reading = new CPUMetricsReading(rawData, size);
		addReading(reading);
	}
	
	public int getCoreLoad(int index) {
		return mAverageCoreLoad[index]/mNumberOfReadings;
	}
	
	public int getCPULoad() {
		return mAverageCPULoad/mNumberOfReadings;
	}
	
	public int getNumCores() {
		return mNumCores;
	}
	
	public int getNumberOfReadings() {
		return mNumberOfReadings;
	}
	
}