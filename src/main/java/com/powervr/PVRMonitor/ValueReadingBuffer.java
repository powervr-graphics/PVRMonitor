/******************************************************************************

 @File         ValueReadingBuffer.java

 @Title        ValueReadingBuffer

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  This ValueReading extension is used to smooth the values of
 collected data.

 ******************************************************************************/
package com.powervr.PVRMonitor;

import java.util.ArrayList;

// CPU Reading Buffer is used to smooth the CPU values over 1 second
// Is a very simple sliding window implementation
public class ValueReadingBuffer<T extends ValueReading<T>> {
    private ArrayList<T> mBuffer;
    private int mIndexHead;
    private int mCapacity;

    public ValueReadingBuffer(int capacity) {
        // Populate ArrayList
        mBuffer = new ArrayList<T>(capacity);

        mCapacity = capacity;
        mIndexHead = 0;
    }

    public void addReading(T reading) {
        if (mBuffer.size() < mCapacity) {
            mBuffer.add(reading);
        }
        else {
            mIndexHead = (mIndexHead + 1) % mBuffer.size();
            mBuffer.set(mIndexHead, reading);
        }
    }

    public T getSmoothValue() {
        T retVal = null;

        if (mBuffer.size() >= mCapacity) {
            // Add the whole buffer contents
            retVal = mBuffer.get(0);
            for (int i = 1; i < mBuffer.size(); ++i) {
                retVal.addReading(mBuffer.get(i));
            }
        }

        return retVal;
    }

    // Get the top value (most recent one)
    public T getTopValue() {
        // Return the last value
        return (mBuffer.size() >= mCapacity) ? mBuffer.get(mIndexHead) : null;
    }
}
