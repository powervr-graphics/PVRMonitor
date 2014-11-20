/******************************************************************************

 
@File         HUDView.cpp

 @Title        HUDView

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Renders the performance statistics overlay.

******************************************************************************/
package com.powervr.PVRMonitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

public class HUDView extends View {
	public static final Map<Integer, HWCounter> NAMES = createNameMap();
	private static Map<Integer, HWCounter> createNameMap() {
	    Map<Integer, HWCounter> result = new HashMap<Integer, HWCounter>();
	    result.put(HWCounter.FPS.getPosition(), HWCounter.FPS);
	    result.put(HWCounter.LOAD_CPU.getPosition(), HWCounter.LOAD_CPU);
	    result.put(HWCounter.LOAD_CPU_C0.getPosition(), HWCounter.LOAD_CPU_C0);
	    result.put(HWCounter.LOAD_CPU_C1.getPosition(), HWCounter.LOAD_CPU_C1);
	    result.put(HWCounter.LOAD_CPU_C2.getPosition(), HWCounter.LOAD_CPU_C2);
	    result.put(HWCounter.LOAD_CPU_C3.getPosition(), HWCounter.LOAD_CPU_C3);
	    result.put(HWCounter.LOAD_CPU_C4.getPosition(), HWCounter.LOAD_CPU_C4);
	    result.put(HWCounter.LOAD_CPU_C5.getPosition(), HWCounter.LOAD_CPU_C5);
	    result.put(HWCounter.LOAD_CPU_C6.getPosition(), HWCounter.LOAD_CPU_C6);
	    result.put(HWCounter.LOAD_CPU_C7.getPosition(), HWCounter.LOAD_CPU_C7);
	    result.put(HWCounter.LOAD_2D.getPosition(), HWCounter.LOAD_2D);
	    result.put(HWCounter.LOAD_3D.getPosition(), HWCounter.LOAD_3D);
	    result.put(HWCounter.LOAD_TA.getPosition(), HWCounter.LOAD_TA);
	    result.put(HWCounter.LOAD_TSP.getPosition(), HWCounter.LOAD_TSP);
	    result.put(HWCounter.LOAD_PIXEL.getPosition(), HWCounter.LOAD_PIXEL);
	    result.put(HWCounter.LOAD_VERTEX.getPosition(), HWCounter.LOAD_VERTEX);
	    return Collections.unmodifiableMap(result);
	}
	
    private final int mWidth, mHeight;
	private Paint mLoadPaint;
	private int mBarAlpha = 100;
    
    //private PVRScopeReading mPVRScopeReading = null;
    private boolean mToAverageCPU = false;
    private boolean mToAverageGPU = false;
    private boolean mIsPVRScopeDisabled = false;
    private ValueReadingBuffer<CPUMetricsReading> mCPUReading = new ValueReadingBuffer<CPUMetricsReading>(10);
    private ValueReadingBuffer<PVRScopeReading> mGPUReading = new ValueReadingBuffer<PVRScopeReading>(5);
    
    private boolean[] mIsEnabled = new boolean[16]; 
    
    public HUDView(Context context, int mWidth, int mHeight, PVRScopeBridge communications) {
        super(context);
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        
        int textHeight = mHeight/14;
        
		mLoadPaint = new Paint();
		mLoadPaint.setAntiAlias(false);
		mLoadPaint.setTextSize(textHeight);
		mLoadPaint.setARGB(255, 255, 0, 0);
		
		for (int i = 0; i < 16; ++i) mIsEnabled[i] = false;
		
		// Set the initial values in case the overlay is restarted
		mIsEnabled[HWCounter.LOAD_CPU.getPosition()] = true;
		mIsEnabled[HWCounter.LOAD_TA.getPosition()] = true;
		mIsEnabled[HWCounter.LOAD_3D.getPosition()] = true;
		
		mBarAlpha = 200;
		mToAverageCPU = false;
    }   
    
    // Used to pick the color to fill the bars
    private int getColor(float load) {
    	int retColorR = 0x00;
    	int retColorG = 0xFF;
    	int retColorB = 0x2D;
    	
    	// Go from G -> R
    	retColorR = (int)((load*0xFF)/100);
    	retColorG -= (int)((load*0xFF)/100);
    	
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
    
    private void drawCombinedBar(String title, float[] values, float topPosition, Canvas canvas) {
    	if (values.length == 0) return; 
    	
		int bgColor = applyAlpha(0xCCDDEE);
    	
    	int leftMargin = (int) (mWidth/5);
    	int rightMargin = mHeight/14;
    	int barMargin = mHeight/140;
    	
    	int totalWidth = mWidth - leftMargin - rightMargin - barMargin;
        int totalHeight = mHeight/14 * 3;
        
        int bottomPosition = (int) (topPosition + totalHeight);

    	// Draw the purple background
        mLoadPaint.setColor(bgColor);
        canvas.drawRect(leftMargin, topPosition, mWidth - rightMargin, bottomPosition, mLoadPaint );
        
        // Draw the load bars
        int barWidth = totalWidth/values.length;
        float average = 0;
        for (int i = 0; i < values.length; ++i) {
        	mLoadPaint.setColor(getColor(values[i]));
        	
        	float barValue = (values[i] * totalHeight)/100; 
            canvas.drawRect(leftMargin + barMargin + barWidth * i, 
            		topPosition + (totalHeight - barValue) - barMargin,
            		leftMargin + barWidth * (i + 1) , 
            		bottomPosition - barMargin,  
            		mLoadPaint);
            
            average += values[i];
        }
        
        // Draw the average value
        average /= values.length;
        mLoadPaint.setColor(Color.RED);
        canvas.drawLine(leftMargin + barMargin, 
        		bottomPosition - barMargin - (average * totalHeight)/100, 
        		mWidth - rightMargin - barMargin, 
        		bottomPosition - barMargin - (average * totalHeight)/100, 
        		mLoadPaint);
        
        // Draw the text
        mLoadPaint.setColor(Color.BLACK);
        int textPlace = leftMargin + (totalWidth/2) - (int) (mLoadPaint.measureText(title)/2); 
        canvas.drawText(title, textPlace, topPosition + totalHeight/2, mLoadPaint);
    }
    
    private void drawBar(String title, float value, float topPosition, Canvas canvas) {
    	int bgColor = applyAlpha(0xCCDDEE);
    	
        int barHeight = mHeight/14;
    	int leftMargin = (int) (mWidth/5);
    	int rightMargin = barHeight;
    	
    	int totalValue = mWidth - leftMargin - rightMargin;
    	float barValue = (value * totalValue)/100; 
    	
    	int textPlace = leftMargin + (totalValue/2) - (int) (mLoadPaint.measureText(title)/2); 
    	
    	// Draw the purple background
        mLoadPaint.setColor(bgColor);
        canvas.drawRect(leftMargin, topPosition, mWidth - rightMargin, topPosition + barHeight, mLoadPaint );
        
        // Draw the load bars
        mLoadPaint.setColor(getColor(value));
        canvas.drawRect(leftMargin + barHeight/10, topPosition + barHeight/10, leftMargin + barValue, topPosition + barHeight - barHeight/10, mLoadPaint );
        
        // Draw the text
        mLoadPaint.setColor(Color.BLACK);
        canvas.drawText(title, textPlace, topPosition + barHeight - barHeight/10, mLoadPaint);
    }
    
    // Get a value from either the PVRScopeReading or the CPUMetrics reading
    private int getReadingValue(int index) {
    	int retVal = 0;
    	
    	// The reading changes depending on if smoothing is turned on or not
    	CPUMetricsReading cpuMetricsReading = 
    			mToAverageCPU ? mCPUReading.getSmoothValue() : mCPUReading.getTopValue();
    
    	PVRScopeReading pvrScopeReading = 
    			mToAverageGPU ? mGPUReading.getSmoothValue() : mGPUReading.getTopValue();
    	
    	HWCounter counter = HWCounter.getByIndex(index);
    	switch (counter) {
	    	case FPS:
	    		if (pvrScopeReading != null)
	    			retVal = pvrScopeReading.getFPS();
	    		break;
	    	case LOAD_CPU:
	    		if (cpuMetricsReading != null)
	    			retVal = cpuMetricsReading.getCPULoad();
	    		break;
	    	case LOAD_CPU_C0:
	    		if (cpuMetricsReading != null)
	    			retVal = cpuMetricsReading.getCoreLoad(0);
	    		break;
	    	case LOAD_CPU_C1:
	    		if (cpuMetricsReading != null)
	    			retVal = cpuMetricsReading.getCoreLoad(1);
	    		break;
	    	case LOAD_CPU_C2:
	    		if (cpuMetricsReading != null)
	    			retVal = cpuMetricsReading.getCoreLoad(2);
	    		break;
	    	case LOAD_CPU_C3:
	    		if (cpuMetricsReading != null)
	    			retVal = cpuMetricsReading.getCoreLoad(3);
	    		break;
	    	case LOAD_CPU_C4:
	    		if (cpuMetricsReading != null)
	    			retVal = cpuMetricsReading.getCoreLoad(4);
	    		break;
	    	case LOAD_CPU_C5:
	    		if (cpuMetricsReading != null)
	    			retVal = cpuMetricsReading.getCoreLoad(5);
	    		break;
	    	case LOAD_CPU_C6:
	    		if (cpuMetricsReading != null)
	    			retVal = cpuMetricsReading.getCoreLoad(6);
	    		break;
	    	case LOAD_CPU_C7:
	    		if (cpuMetricsReading != null)
	    			retVal = cpuMetricsReading.getCoreLoad(7);
	    		break;
	    	case LOAD_2D:
	    		if (pvrScopeReading != null)
	    			retVal = pvrScopeReading.get2DLoad();
	    		break;
	    	case LOAD_3D:
	    		if (pvrScopeReading != null)
	    			retVal = pvrScopeReading.get3DLoad();
	    		break;
	    	case LOAD_TA:
	    		if (pvrScopeReading != null)
	    			retVal = pvrScopeReading.getTALoad();
	    		break;
	    	case LOAD_TSP:
	    		if (pvrScopeReading != null)
	    			retVal = pvrScopeReading.getTSPLoad();
	    		break;
	    	case LOAD_PIXEL:
	    		if (pvrScopeReading != null)
	    			retVal = pvrScopeReading.getPixelLoad();
	    		break;
	    	case LOAD_VERTEX:
	    		if (pvrScopeReading != null)
	    			retVal = pvrScopeReading.getVertexLoad();
	    		break;
	    	default:
	    		break;
    	}
    	
    	//Sanitize the value returned
    	if (retVal < 0) {
    		retVal = 0;
    	}
    	else if (retVal > 100) {
    		retVal = 100;
    	}
    	
    	return retVal;
    }
    
    // Called after every reading
    // Draws the HUDView overlay
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
    	//Set the rowHeight
    	int rowHeight = mHeight/12;
        int row = 1;
        
        // If the CPU1 is enabled then display all the cores on its own widget
        if (mIsEnabled[HWCounter.LOAD_CPU_C1.getPosition()]) {
        	float values[] = new float[CPUMetricsReading.mNumCores];
        	for (int i = 0; i < values.length; ++i) {
        		values[i] = getReadingValue(i + HWCounter.LOAD_CPU_C0.getPosition());
        	}
        	drawCombinedBar("CPU", values, rowHeight * row, canvas);
        	row += 3;
        }
        
        for (int i = 0; i < 16; ++i) {
        	if (mIsEnabled[i]) {
        		//Get the name and value of the counter
	        	String name = NAMES.get(i).getName();
	        	int value = getReadingValue(i);
	        	
	        	//Either draw a bar or draw the text
	        	if (NAMES.get(i).getType() == HWCounterType.BAR) {
	        		drawBar(name, value, rowHeight * row, canvas);
	        		++row;
	        	}
	        	else if (NAMES.get(i).getType() == HWCounterType.TEXT) {
	                mLoadPaint.setColor(Color.WHITE);
	                canvas.drawText(value + " " + name, 0, rowHeight * row, mLoadPaint);
	                ++row;
	        	}
	        	
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

	public void addPVRScopeReading(PVRScopeReading pvrScopeReading) {
		mGPUReading.addReading(pvrScopeReading);
	}
	
	public void setEnabledValues(boolean isEnabled[]) {
		if (isEnabled.length < 16) { 
			return;
		}
		
		for (int i = 0; i < 16; ++i) mIsEnabled[i] = isEnabled[i];
		
		//Disable all the PVRScope counters
		if (mIsPVRScopeDisabled) {
			mIsEnabled[HWCounter.FPS.getPosition()] = false;
			mIsEnabled[HWCounter.LOAD_2D.getPosition()] = false;
			mIsEnabled[HWCounter.LOAD_3D.getPosition()] = false;
			mIsEnabled[HWCounter.LOAD_TA.getPosition()] = false;
			mIsEnabled[HWCounter.LOAD_TSP.getPosition()] = false;
			mIsEnabled[HWCounter.LOAD_PIXEL.getPosition()] = false;
			mIsEnabled[HWCounter.LOAD_VERTEX.getPosition()] = false;
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
		mIsPVRScopeDisabled = true;
		//Disable the counters turned on by default
		mIsEnabled[HWCounter.LOAD_TA.getPosition()] = false;
		mIsEnabled[HWCounter.LOAD_3D.getPosition()] = false;
	}


}
