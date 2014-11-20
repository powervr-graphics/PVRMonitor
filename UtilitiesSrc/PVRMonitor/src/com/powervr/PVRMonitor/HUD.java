/******************************************************************************

 @File         HUD.cpp

 @Title        HUD

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Configure the behaviour of the performance statistics overlay.

******************************************************************************/
package com.powervr.PVRMonitor;

import android.app.Service;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class HUD extends Service {
    private final IBinder mBinder = new LocalBinder();
    private PVRScopeBridge mPVRScopeBridge = new PVRScopeBridge();
	private int mWidth = 0;
	private int mHeight = 0;
	private HUDView mView = null;
    boolean mToShowTopRight = true;
    boolean mIsPVRScopeDisabled = false;
    
    public class LocalBinder extends Binder {
    	HUD getService() {
            return HUD.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Get the WindowManager and the screen size
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        
        // Create the HUDView
        mWidth = size.x;
        mHeight = size.y;
        if (mWidth < mHeight) {
        	mHeight = mWidth;
        }
        else {
        	mWidth = mHeight;
        }
        mView = new HUDView(this, mWidth/2, mHeight/2, mPVRScopeBridge);
        
        // Set up the view as a System Overlay
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
        		mWidth/2, 
                mHeight/2,
                //WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                |WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.RGBA_8888);
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.setTitle("PVRMonitor");

        wm.addView(mView, params);
        
        // Initialize the PVRScopeHUD library
        if (!mPVRScopeBridge.initPVRScope()) {
        	//There has been an issue initializing PVRScope
        	mView.disablePVRScopeCounters();
        	mIsPVRScopeDisabled = true;
        }
       
        Thread thr = new Thread(null, mTask, "HUD");
        thr.start();
    }

    // Worker thread
    Runnable mTask = new Runnable() {

        public void run() {
        	boolean toStop = false;
        	while (!toStop) {
                try {
                	// Sleep 100ms
					Thread.sleep(100);
	                mPVRScopeBridge.readData(mView);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					toStop = true;
				}
        	}
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Remove the view
        if(mView != null)
        {
        	WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.removeView(mView);
            mView = null;
        }
        
        mPVRScopeBridge.deinitPVRScope();
    }

	public void showHUD() {
		if (mView != null) {
			mView.setVisibility(View.VISIBLE);
		}
	}
    
    public void hideHUD() {
		if (mView != null) {
			mView.setVisibility(View.GONE);
		}
    }
    
	public void setEnabledValues(boolean isEnabled[]) {
		mView.setEnabledValues(isEnabled);
	}

	public void showInTopRight(boolean toShowTopRight) {
		//Get the Layout parameters
		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		WindowManager.LayoutParams params =  (LayoutParams) mView.getLayoutParams();
		
		//Change the gravity
		if (toShowTopRight) {
	        params.gravity = Gravity.RIGHT | Gravity.TOP;
		}
		else {
	        params.gravity = Gravity.LEFT | Gravity.TOP;
		}

		//Update the Layout
		wm.updateViewLayout(mView, params);
	}
	
	public void setOpacity(int opacity) {
		mView.setBarOpacity(opacity);
	}

	public void setAverageCPUEnabled(boolean toAverageCPU) {
		mView.setAverageCPUEnabled(toAverageCPU);
	}
	
	public void setAverageGPUEnabled(boolean toAverageGPU) {
		mView.setAverageGPUEnabled(toAverageGPU);
	}
	
	public boolean getIsPVRScopeDisabled() {
		return mIsPVRScopeDisabled;
	}
}
