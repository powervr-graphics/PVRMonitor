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
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import java.util.Vector;

public class HUD extends Service {
    private final IBinder mBinder = new LocalBinder();
    private PVRScopeBridge mPVRScopeBridge = new PVRScopeBridge();
    public int mWidth = 0;
    public int mHeight = 0;
    public float mX = 0;
    public float mY = 0;
    private HUDView mView = null;
    boolean mToShowTopRight = true;
    boolean mIsPVRScopeDisabled = false;

    boolean mLandscape = false;


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

        mLandscape = mWidth >= mHeight;

        int displayWidth = /*mLandscape ? mWidth / 3 : */mWidth / 2;
        mView = new HUDView(this, displayWidth, mHeight/2, mPVRScopeBridge);

        // Set up the view as a System Overlay
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                mWidth,
                mHeight,
                //WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?  WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
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
                    mPVRScopeBridge.readData(mView, Integer.toString(android.os.Process.myPid()), mIsPVRScopeDisabled);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            CheckScreenOrientation();
                        }
                    });

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    toStop = true;
                }
            }
        }
    };

    private void CheckScreenOrientation(){
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);

        mWidth = size.x;
        mHeight = size.y;

        boolean landscape = mWidth >= mHeight;
        if(landscape != mLandscape && mView != null){
            mLandscape = landscape;
            //reset activity somehow.
            int displayWidth = /*landscape ? mWidth / 3 : */mWidth / 2;
            mView.Create(displayWidth, mHeight / 2);

            WindowManager.LayoutParams params =  (LayoutParams) mView.getLayoutParams();

            params.width = mWidth; params.height = mHeight;
            if (mToShowTopRight) {
                params.x = /*landscape ? 2 * mWidth / 3 :*/ mWidth / 2; params.y = 0;
            }
            else {
                params.x = 0; params.y = 0;
            }
            mX = params.x; mY = params.y;
            mView.setX(mX);
            mView.setY(mY);
            wm.updateViewLayout(mView, params);
        }
    }


    public String[] getStrings()
    {
        String[] strings = mPVRScopeBridge.getStrings(false);
        mView.setTotalNumCounters(strings.length);
        return strings;
    }


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

    public void setViewPos(float x, float y) {

        //TODO: make this more readable. (width value = leftMargin from HUDView, height value = BarHeight from HUDView)
        float xOffset = mWidth / 2 / 5;
        float yOffset = mWidth / 2 / 6;

        x -= xOffset;
        y -= yOffset;

        mView.setX(x);
        mView.setY(y);


        mX = x; mY = y;
    }

    public Rect getOverlay() {
        return mView.getViewOverlay(mX, mY);
    }

    public void setCountersSelected(Vector<CounterInUse> countersSelected, boolean cpuEnabled) {
        mView.setCountersSelected(countersSelected, cpuEnabled);
    }

    public void showInTopRight(boolean toShowTopRight) {
        //Get the Layout parameters
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params =  (LayoutParams) mView.getLayoutParams();

        //Change the gravity
        if (toShowTopRight) {
            params.x = mWidth / 2; params.y = 0;
        }
        else {
            params.x = 0; params.y = 0;
        }


        mX = params.x; mY = params.y;

        mView.setX(mX);
        mView.setY(mY);

        //Update the Layout
        wm.updateViewLayout(mView, params);
        mToShowTopRight = toShowTopRight;
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

    public void updateUIOptions(Vector<CounterInUse> mCountersSelected)
    {
        mView.updateUIOptions(mCountersSelected);
    }

    public boolean getIsPVRScopeDisabled() {
        return mIsPVRScopeDisabled;
    }

}
