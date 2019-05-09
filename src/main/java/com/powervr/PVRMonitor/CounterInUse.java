package com.powervr.PVRMonitor;

import java.util.ArrayList;

class LimitedSizeQueue<K> extends ArrayList<K> {

    private int maxSize;

    public LimitedSizeQueue(int size){
        this.maxSize = size;
    }

    public boolean add(K k){
        boolean r = super.add(k);
        if (size() > maxSize){
            removeRange(0, size() - maxSize - 1);
        }
        return r;
    }

    public K getYoungest() {
        return get(size() - 1);
    }

    public K getOldest() {
        return get(0);
    }
}

public class CounterInUse{
    //number of recordings per counter.
    public static int mValueCount = 50;

    //name of counter
    public String mName;

    //index of counter in counters array pulled from PVRScope
    public int mIndex;

    //display option (text, graph etc.)
    public int mOption;

    //extended value identification.
    public int mId;

    public int mFrameNumber = 0;

    public boolean mPercentage = false;

    //counter values returned from Scope.
    public LimitedSizeQueue<Float> mPastCounterValues;

    private float mMax;
    private float mMin;

    public CounterInUse() {

    }

    public CounterInUse(String name, int index, int option, int Id){
        this.mName = name;

        if(mName.contains("%")) {
            mPercentage = true;
            mName = mName.substring(0, mName.length() - 4);
        }

        this.mIndex = index;
        this.mOption = option;
        this.mId = Id;
        mPastCounterValues = new LimitedSizeQueue<>(mValueCount);
    }

    public void ResetCounter(){
        mFrameNumber = 0;
    }

    public void setmMax(float mMax) {
        this.mMax = mMax;
    }

    public float getmMax() {
        return mMax;
    }

    public void setmMin(float mMin) {
        this.mMin = mMin;
    }

    public float getmMin() {
        return mMin;
    }

    //public void setmPastCounterValue(float value, int index) {
    //    mPastCounterValues[index] = value;
    //}
//
    //public float[] getmPastCounterValues() {
   //     return mPastCounterValues;
   // }
}