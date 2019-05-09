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

import java.util.Vector;

public class PVRScopeReading implements ValueReading<PVRScopeReading>{

    private int mNumberOfReadings;

    private Vector<Float> mReadingValues = new Vector<>();
    //private float[] mReadingValues;

    public PVRScopeReading(float[] rawData, int size) {
        if (size == 0) return;
        mNumberOfReadings = 1;

        mReadingValues.ensureCapacity(size);

        for(int i = 0; i < size; i++) {
            mReadingValues.add(Float.valueOf(rawData[i]));
        }

    }

    @Override
    public void addReading(PVRScopeReading reading) {
        if (reading == null) return;

        if(mReadingValues.size() != reading.mReadingValues.size()){
            mReadingValues.ensureCapacity(reading.mReadingValues.size());
        }

        for(int i = 1; i < reading.mReadingValues.size(); i++){
            try {
                mReadingValues.set(i, mReadingValues.elementAt(i) + reading.mReadingValues.elementAt(i));
            } catch(ArrayIndexOutOfBoundsException e){
                mReadingValues.add(reading.mReadingValues.elementAt(i));
            }

        }

        mNumberOfReadings += reading.mNumberOfReadings;
    }

    //stub function
    public void addData(int[] rawData, int size) {

    }

    public void addData(float[] rawData, int size) {
        PVRScopeReading reading = new PVRScopeReading(rawData, size);
        addReading(reading);
    }

    public float getReadingValue(int index) {
        return mReadingValues.elementAt(index)/(float)mNumberOfReadings;
    }
}
