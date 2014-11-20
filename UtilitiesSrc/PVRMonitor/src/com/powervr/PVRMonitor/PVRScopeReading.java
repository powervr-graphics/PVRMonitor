/******************************************************************************

 @File         PVRScopeReading.java

 @Title        PVRScopeReading

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Provides a Java interface to interact with the native module's
               PVRScope data.

******************************************************************************/
package com.powervr.PVRMonitor;

public class PVRScopeReading implements ValueReading<PVRScopeReading>{
	private int mAverageFPS;
	private int mAverageTALoad;
	private int mAverage3DLoad;
	private int mAverage2DLoad;
	private int mAverageTSPLoad;
	private int mAveragePixelLoad;
	private int mAverageVertexLoad;
	private int mNumberOfReadings;
	
	public PVRScopeReading(byte[] rawData, int size) {
		if (size != 8) return;
		
		mAverageFPS = (rawData[1] << 8) + rawData[0];
		mAverage2DLoad = rawData[2];
		mAverage3DLoad = rawData[3];
		mAverageTALoad = rawData[4];
		mAverageTSPLoad = rawData[5];
		mAveragePixelLoad = rawData[6];
		mAverageVertexLoad = rawData[7];
		mNumberOfReadings = 1;
	}
	
	@Override
	public void addReading(PVRScopeReading reading) {
		if (reading == null) return; 
			
		mAverageFPS += reading.mAverageFPS;
		mAverage2DLoad += reading.mAverage2DLoad;
		mAverage3DLoad += reading.mAverage3DLoad;
		mAverageTALoad += reading.mAverageTALoad;
		mAverageTSPLoad += reading.mAverageTSPLoad;
		mAveragePixelLoad += reading.mAveragePixelLoad;
		mAverageVertexLoad += reading.mAverageVertexLoad;
		mNumberOfReadings += reading.mNumberOfReadings;
	}
	
	@Override
	public void addData(byte[] rawData, int size) {
		PVRScopeReading reading = new PVRScopeReading(rawData, size);
		addReading(reading);
	}
	
	public int getFPS() {
		return mAverageFPS/mNumberOfReadings;
	}
	
	public int get3DLoad() {
		return mAverage3DLoad/mNumberOfReadings;
	}
	
	public int get2DLoad() {
		return mAverage2DLoad/mNumberOfReadings;
	}
	
	public int getTALoad() {
		return mAverageTALoad/mNumberOfReadings;
	}
	
	public int getTSPLoad() {
		return mAverageTSPLoad/mNumberOfReadings;
	}
	
	public int getPixelLoad() {
		return mAveragePixelLoad/mNumberOfReadings;
	}
	
	public int getVertexLoad() {
		return mAverageVertexLoad/mNumberOfReadings;
	}
}
