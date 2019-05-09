/******************************************************************************


 @File         HUDView.cpp

 @Title        HUDView

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Renders the performance statistics overlay.

 ******************************************************************************/
package com.powervr.PVRMonitor;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class HUDView extends View {

    private int mNumCounters = 6;

    private int mWidth, mHeight;
    private Paint mLoadPaint;
    private int mBarAlpha = 100;

    // Offset from edges of graph - in pixels.
    int mBoundaryOffset;

    private CounterInUse[] mCountersSelected = new CounterInUse[mNumCounters];
    private int mEnabledCounters = 0;
    private int mNumCountersReturned = 0;
    private int mMonitorFPSIndex = 0; //FPS of PVRMonitor
    private int mFPSIndex = 0; //FPS of top PID

    private int mLeftMargin;
    private int mRightMargin;
    private int mTextColor;
    private int mBaseTextSize;
    private int mRows;
    private int mRowHeight;

    private boolean mCPUEnabled = false;
    private boolean mToAverageCPU = false;
    private boolean mToAverageGPU = false;
    private boolean mIsPVRScopeDisabled = false;
    private ValueReadingBuffer<CPUMetricsReading> mCPUReading = new ValueReadingBuffer<CPUMetricsReading>(10);
    private ValueReadingBuffer<PVRScopeReading> mGPUReading = new ValueReadingBuffer<PVRScopeReading>(10);


    public HUDView(Context context, int width, int height, PVRScopeBridge communications) {
        super(context);
        Create(width, height);
    }

    public void Create(int width, int height){
        this.mWidth = width;
        this.mHeight = height;

        mLeftMargin = (mHeight/14);
        mRightMargin = mHeight/14;

        mTextColor = applyAlpha(Color.WHITE);
        mBaseTextSize = mWidth > mHeight ? mWidth/26 : mHeight/26;
        mRowHeight = mBaseTextSize*17/12;

        mLoadPaint = new Paint();
        mLoadPaint.setAntiAlias(false);
        mLoadPaint.setTextSize(mBaseTextSize);
        mLoadPaint.setARGB(255, 255, 0, 0);

        mBoundaryOffset = 10;

        mBarAlpha = 200;
        mToAverageCPU = false;
    }

    // Used to pick the color to fill the bars
    private int getColor(float load,float maxLoad,boolean inverse) {
        int retColorR = 0x00;
        int retColorG = 0xFF;
        int retColorB = 0x2D;

        load = load > maxLoad ? maxLoad : load;

        // Go from G -> R
        if(!inverse) {
            retColorR = (int) ((load * 0xFF) / maxLoad);
            retColorG -= (int) ((load * 0xFF) / maxLoad);
        }else {
            retColorR = 0xFF;
            retColorG = (int) ((load * 0xFF) / maxLoad);
            retColorR -= (int) ((load * 0xFF) / maxLoad);
        }

        int retColor = (retColorR << 16) + (retColorG << 8) + retColorB;

        return applyAlpha(retColor);
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        return true;
    }

    private int applyAlpha (int color) {
        return (color & 0x00FFFFFF) + (mBarAlpha << 24);
    }

    //@TargetApi(16)
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void updateUIOptions(Vector<CounterInUse> countersSelected) {
        for(int i = 0; i < countersSelected.size(); i++){
            mCountersSelected[i].mOption = countersSelected.elementAt(i).mOption;
        }
    }

    public Rect getViewOverlay(float xOffset, float yOffset) {
        return new Rect(mLeftMargin + (int)xOffset, mHeight/6 + (int)yOffset, mWidth - mRightMargin + (int)xOffset, mHeight/6 * mRows + (int)yOffset);
    }

    public void setTotalNumCounters(int num){
        mNumCountersReturned = num;
    }

    private void drawCombinedBar(String title, float[] values, int topPosition, Canvas canvas) {
        if (values.length == 0) return;

        int totalWidth = mWidth - (mRightMargin / 2);
        int totalHeight = (mRowHeight * 4) - mBoundaryOffset;

        int bgColor = applyAlpha(0x334455);

        // Draw the background
        mLoadPaint.setColor(bgColor);
        canvas.drawRect(mLeftMargin / 2 , topPosition, totalWidth, topPosition + totalHeight, mLoadPaint);

        // Draw Text white background
        mLoadPaint.setColor(applyAlpha(0xFFFFFF));
        canvas.drawRect((mLeftMargin / 2) + mBoundaryOffset, topPosition + mBoundaryOffset, totalWidth - mBoundaryOffset, topPosition + (totalHeight / 5) + mBoundaryOffset, mLoadPaint);

        // Draw text (counter name)
        mLoadPaint.setTextSize(mBaseTextSize);
        mLoadPaint.setColor(applyAlpha(Color.BLACK));

        canvas.drawText(title,(mLeftMargin / 2) + (mBaseTextSize/3) * 2, topPosition + (totalHeight / 5) + (mBaseTextSize/6) , mLoadPaint);

        // Draw the load bars
        int barStartX = (mLeftMargin / 2) + mBoundaryOffset;
        int barEndX = totalWidth - mBoundaryOffset;
        float barWidth = (float)(barEndX - barStartX) / (float)values.length;


        int barBottomY =  topPosition + totalHeight - mBoundaryOffset;
        int barMaxY = topPosition + (totalHeight / 5) + mBoundaryOffset;

        //canvas.drawRect(barStartX, barMaxY, barStartX + (barWidth * (float)values.length) , barBottomY, mLoadPaint);

        for (int i = 0; i < values.length; ++i) {
            mLoadPaint.setColor(getColor(values[i], 100.0f,false));

            float barHeight = values[i]/100.0f;
            float barTopY = barBottomY - ((barBottomY - barMaxY) * barHeight);
            canvas.drawRect(barStartX + (barWidth * i), barTopY, barStartX + (barWidth * (i + 1)), barBottomY, mLoadPaint);
        }

        mLoadPaint.setColor(applyAlpha(Color.BLACK));
        mLoadPaint.setStyle(Paint.Style.STROKE);
        int strokeWidth = 2;
        mLoadPaint.setStrokeWidth(strokeWidth);

        for(int i = 0; i < values.length; i++){
            canvas.drawRect(barStartX + (barWidth * i), barMaxY, barStartX + (barWidth * (i + 1)), barBottomY, mLoadPaint);
        }

        mLoadPaint.setStyle(Paint.Style.FILL);
    }

    private void drawGraph(String name, CounterInUse counter, float value, int topPosition, Canvas canvas){
        //total size is two rows.
        int totalHeight = (mRowHeight * 4) - mBoundaryOffset;
        int totalWidth = mWidth - (mRightMargin / 2);

        int bgColor = applyAlpha(0x334455);

        mLoadPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Draw the background
        mLoadPaint.setColor(bgColor);
        canvas.drawRect(mLeftMargin / 2 , topPosition, totalWidth, topPosition + totalHeight, mLoadPaint);

        // Draw Text white background
        mLoadPaint.setColor(applyAlpha(0xFFFFFF));
        canvas.drawRect((mLeftMargin / 2) + mBoundaryOffset, topPosition + mBoundaryOffset, totalWidth - mBoundaryOffset, topPosition + (totalHeight / 5) + mBoundaryOffset, mLoadPaint);

        // Draw text (counter name)
        mLoadPaint.setTextSize(mBaseTextSize);
        mLoadPaint.setColor(applyAlpha(Color.BLACK));

        int maxTextWidth = (totalWidth - mBoundaryOffset) - ((mLeftMargin/2) + mBoundaryOffset);

        boolean textResized = false;
        String nameText = counter.mName;

        while((int)mLoadPaint.measureText(nameText) > maxTextWidth){
            nameText = nameText.substring(0, nameText.length() - 1);
            textResized = true;
        }

        if(textResized)
            nameText = nameText.substring(0, nameText.length() - 3) + "...";

        canvas.drawText(nameText,(mRightMargin / 2) + (mBaseTextSize/3) * 2, topPosition + (totalHeight / 5) + (mBaseTextSize/6) , mLoadPaint);

        // Draw values (current, min, max)
        // Current
        mLoadPaint.setColor(applyAlpha(Color.BLACK));
        mLoadPaint.setTextSize(mBaseTextSize * 5 / 4);

        int widthWithOffset = totalWidth - mBoundaryOffset;
        String text = FormatIntegerText(value, counter.mPercentage);
        canvas.drawText(text, widthWithOffset - mLoadPaint.measureText(text), topPosition + (7 * totalHeight / 10), mLoadPaint);

        // Max
        mLoadPaint.setTextSize(mBaseTextSize * 3 / 4);

        text = FormatIntegerText(counter.getmMax(), counter.mPercentage);
        text = "Max: " + text;
        canvas.drawText(text, widthWithOffset - mLoadPaint.measureText(text),topPosition + ( 2 * totalHeight / 5), mLoadPaint);

        // Min
        text = FormatIntegerText(counter.getmMin(), counter.mPercentage);
        text = "Min: " + text;
        canvas.drawText(text, widthWithOffset - mLoadPaint.measureText(text), topPosition + totalHeight - mBoundaryOffset, mLoadPaint);

        // for positioning
        final String maxText = "Max: 99.9%";

        // Draw bars
        float barStartX = (mLeftMargin / 2) + mBoundaryOffset;
        float barEndX = widthWithOffset - mBoundaryOffset - mLoadPaint.measureText(maxText);
        float barWidth =  (barEndX - barStartX) / CounterInUse.mValueCount;

        int barBottomY = topPosition + totalHeight - mBoundaryOffset;
        int barMaxY = topPosition + (totalHeight / 5) + mBoundaryOffset;

        for(int i = 0; i < counter.mPastCounterValues.size(); i++){
            float currentVal = counter.mPastCounterValues.get(i);

            float graphTop = counter.mPercentage ? 100.0f : (counter.getmMax() * 1.3f);
            float barHeight =  Math.min((currentVal / graphTop), 1.0f);

            float barTopY = barBottomY - ((barBottomY - barMaxY) * barHeight);

            mLoadPaint.setColor(getColor(currentVal, currentVal > 100.0f ? (counter.getmMax() * 1.3f) : 100.0f, false));

            canvas.drawRect(barStartX + (barWidth * i), barTopY, barStartX + (barWidth * (i + 1)), barBottomY, mLoadPaint);
        }

    }

    private String FormatIntegerText(float value, boolean textPercentage){
        //output a more readable format
        String text = String.format(Locale.US, "%.1f", value);
        if(value > 999999999){
            int integers = text.length() % 11;
            text = text.substring(0, integers) + (integers < 3 ? "." + text.substring(integers, 3) : text.substring(integers, 3)) + "G";
        }
        else if(value > 999999){
            int integers = text.length() % 8;
            text = text.substring(0, integers) + (integers < 3 ? "." + text.substring(integers, 3) : text.substring(integers, 3)) + "M";
        }
        else if(value > 999){
            int integers = text.length() % 5;
            text = text.substring(0, integers) + (integers < 3 ? "." + text.substring(integers, 3) : text.substring(integers, 3)) + "k";
        }
        else if(textPercentage) text+="%";
        return text;
    }

    private void updateFrameValues()
    {
            for (int i = 0; i < mEnabledCounters; i++) {
                float currentVal;

                if(mCountersSelected[i].mName == "Frames per second (FPS)") {
                    currentVal = mFPSIndex == -1 ? 0 : getReadingValue(mFPSIndex, false);
                }
                else
                    currentVal = getReadingValue(mCountersSelected[i].mIndex, false);

                mCountersSelected[i].mPastCounterValues.add(currentVal);

                if (currentVal > mCountersSelected[i].getmMax())
                    mCountersSelected[i].setmMax(currentVal);

                if ((currentVal < mCountersSelected[i].getmMin() && currentVal > 0) || mCountersSelected[i].getmMin() == 0)
                    mCountersSelected[i].setmMin(currentVal);


            }


    }

    // Get a value from either the PVRScopeReading or the CPUMetrics reading
    private float getReadingValue(int index, boolean CPU) {

        if(CPU){
            int retVal = 0;
            // The reading changes depending on if smoothing is turned on or not
            CPUMetricsReading cpuMetricsReading =
                    mToAverageCPU ? mCPUReading.getSmoothValue() : mCPUReading.getTopValue();

            //TODO: clean up this mess.
            if (cpuMetricsReading != null) {
                if (index == 0) {
                    retVal = cpuMetricsReading.getCoreLoad(0);
                }
                else if(index > 8)
                    return retVal;
                else
                    retVal = cpuMetricsReading.getCoreLoad(index);

                return retVal;

            }

        } else {
            float retVal = 0;

            PVRScopeReading pvrScopeReading =
                    mToAverageGPU ? mGPUReading.getSmoothValue() : mGPUReading.getTopValue();
            if(pvrScopeReading != null)
                retVal = pvrScopeReading.getReadingValue(index);


            return retVal;
        }

    return 0;

    }

    private void drawText(String name, float currentValue, float maxValue, boolean percentage, float topPosition, Canvas canvas)
    {
        int barHeight = mRowHeight * 2;
        float y = topPosition + barHeight/2;
        int totalWidth = mWidth - (mRightMargin / 2);

        drawBackground(topPosition, topPosition + barHeight - mBoundaryOffset, canvas);

        // Draw the text
        mLoadPaint.setTextSize(mBaseTextSize);
        mLoadPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mLoadPaint.setColor(mTextColor);

        int textX= (mLeftMargin / 2) + mBoundaryOffset;
        int maxTextWidth = (mWidth*3/4) - textX;

        boolean textResized = false;
        String nameText = name;


        while((int)mLoadPaint.measureText(nameText) > maxTextWidth){
            nameText = nameText.substring(0, nameText.length() - 1);
            textResized = true;
        }
        mLoadPaint.setColor(mTextColor);

        String text = FormatIntegerText(currentValue, percentage);

        if(textResized)
            nameText = nameText.substring(0, nameText.length() - 3) + "...";
        canvas.drawText(nameText, textX, y, mLoadPaint);

        canvas.drawText(text, totalWidth - mBoundaryOffset - mLoadPaint.measureText(text), y, mLoadPaint);
    }

    private void drawBackground(float topPosition, float bottomPosition, Canvas canvas)
    {
        int bgColor = applyAlpha(0x334455);

        // Draw the background
        mLoadPaint.setColor(bgColor);
        canvas.drawRect(mRightMargin / 2, topPosition, mWidth - (mRightMargin/2), bottomPosition, mLoadPaint );
    }


    // Called after every reading
    // Draws the HUDView overlay
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Set the rowHeight
        mRows = 1;

        //re-apply current alpha variable.
        mTextColor = applyAlpha(Color.WHITE);

        updateFrameValues();

        if(mCPUEnabled){
            float values[] = new float[CPUMetricsReading.mNumCores];
            for (int i = 0; i < values.length; ++i) {
                values[i] = getReadingValue(i, true);
            }
            drawCombinedBar("CPU Loads", values, mRowHeight * mRows, canvas);
            mRows += 4;
        }

        for(int i = 0; i < mEnabledCounters; i++){
            //draw the counters
            float value;
                value = mCountersSelected[i].mPastCounterValues.getYoungest();

            switch (mCountersSelected[i].mOption){
                case 0:
                    drawText(mCountersSelected[i].mName, value, mCountersSelected[i].getmMax(), mCountersSelected[i].mPercentage, mRowHeight * mRows, canvas);
                    mRows+=2;
                    break;
                case 1:
                    drawGraph(mCountersSelected[i].mName, mCountersSelected[i], value, mRowHeight * mRows, canvas);
                    mRows+=4;
                    break;
            }
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO Auto-generated method stub
    }

    public void addCPUMetricsReading(CPUMetricsReading cpuMetricsReading) {
        mCPUReading.addReading(cpuMetricsReading);
    }

    public void addPVRScopeReading(PVRScopeReading pvrScopeReading, int monitorFPSIndex, int topFPSIndex) {
        mMonitorFPSIndex = monitorFPSIndex;
        mFPSIndex = topFPSIndex;
        mGPUReading.addReading(pvrScopeReading);
    }

    public void setCountersSelected(Vector<CounterInUse> countersSelected, boolean cpuEnabled) {
        for (int i = 0; i < countersSelected.size(); ++i){
            mCountersSelected[i] = countersSelected.elementAt(i);
        }

        mEnabledCounters = countersSelected.size();
        mCPUEnabled = cpuEnabled;
        //Disable all the PVRScope counters
        if (mIsPVRScopeDisabled) {

           mEnabledCounters = 0;
        }
    }

    public void setBarOpacity(int opacity) {
        // The value is from 0-0xFF
        if (opacity > 0xFF) opacity = 0xFF;
        else if (opacity < 0) opacity = 0;

        mBarAlpha = opacity;
    }

    public void setAverageCPUEnabled(boolean toAverageCPU) {
        mToAverageCPU = toAverageCPU;
    }

    public void setAverageGPUEnabled(boolean toAverageGPU) {
        mToAverageGPU = toAverageGPU;
    }

    public boolean getToAverageCPU() {
        return mToAverageCPU;
    }

    public void disablePVRScopeCounters() {
        mEnabledCounters = 0;
        mIsPVRScopeDisabled = true;
        //Disable the counters turned on by default
    }


}
