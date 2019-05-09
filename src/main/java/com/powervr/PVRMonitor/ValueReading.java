/******************************************************************************

 @File         ValueReading.java

 @Title        ValueReading

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Java interface to interact with collected counter data.

 ******************************************************************************/
package com.powervr.PVRMonitor;

public interface ValueReading<T> {
    public void addData(int[] rawData, int size);
    public void addReading(T reading);
}
