/******************************************************************************

 @File         PVRMonitor.cpp

 @Title        PVRMonitor

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Handles the application's life cycle.

******************************************************************************/
package com.powervr.PVRMonitor;



import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class PVRMonitor extends Activity {
	private HUD mBoundService;
	
	// Values to save and restore
	private final static String EXTRA_IS_BOUND = "extra_mIsBound"; 
	private boolean mIsBound = false;
	private final static String EXTRA_SHOW_RIGHT = "extra_mToShowTopRight"; 
	private boolean mToShowTopRight = true;
	private final static String EXTRA_ALPHA_VALUE = "extra_mAlphaValue";
	private int mAlphaValue = 100;
	private final static String EXTRA_AVERAGE_CPU = "extra_mToAverageCPU";
	private boolean mToAverageCPU = false;
	private final static String EXTRA_AVERAGE_GPU = "extra_mToAverageGPU";
	private boolean mToAverageGPU = false;
	private final static String EXTRA_SHOW_VALUES = "extra_mToShowValue";
	private boolean mToShowValue[] = new boolean[16]; 
	
	// Back key handling
	private boolean isBackKeyPressed = false;
	
	private class WaitBackKey extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			isBackKeyPressed = false;
			super.onPostExecute(v);
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        mBoundService = ((HUD.LocalBinder)service).getService();
	       
	        mBoundService.setEnabledValues(mToShowValue);
	        mBoundService.showInTopRight(mToShowTopRight);
	        mBoundService.setOpacity(mAlphaValue);
	        mBoundService.setAverageCPUEnabled(mToAverageCPU);
	        mBoundService.setAverageGPUEnabled(mToAverageGPU);
	        
	        //Check if PVRScope is enabled or not
	        if (mBoundService.mIsPVRScopeDisabled) setPVRScopeDisabled();
	        
	        mIsBound = true;
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        mBoundService = null;
	        mIsBound = false;
	    }
	};
	
	void doBindService() {
	    bindService(
	    		new Intent(PVRMonitor.this, HUD.class), 
	    		mConnection, 
	    		Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}

	void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  super.onSaveInstanceState(savedInstanceState);
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  savedInstanceState.putBoolean(EXTRA_SHOW_RIGHT, mToShowTopRight);
	  savedInstanceState.putBoolean(EXTRA_AVERAGE_CPU, mToAverageCPU);
	  savedInstanceState.putBoolean(EXTRA_AVERAGE_GPU, mToAverageGPU);
	  savedInstanceState.putInt(EXTRA_ALPHA_VALUE, mAlphaValue);
	  savedInstanceState.putBooleanArray(EXTRA_SHOW_VALUES, mToShowValue);
	  savedInstanceState.putBoolean(EXTRA_IS_BOUND, mIsBound);
	}
	
	private void setupViews() {
		// Set the seekbar functions
		SeekBar opacity = (SeekBar) findViewById(R.id.slide_opacity);
		 
		opacity.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;
 
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                if (mBoundService != null) {
                	mBoundService.setOpacity(progressChanged);
                }
                mAlphaValue = progressChanged;
            }
 
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
 
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mBoundService != null) {
                	mBoundService.setOpacity(progressChanged);
                }
                mAlphaValue = progressChanged;
            }
        });
		opacity.setProgress(mAlphaValue);
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
    		!isBackKeyPressed) {
        	// Show the toast
	        Toast myToast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.back_button_warning), Toast.LENGTH_LONG); 
	        myToast.show();
	        
	        // Flag back key as pressed
	        isBackKeyPressed = true;
	        
	        // Keep the key up 
	        new WaitBackKey().execute();
        }
		else {
	        // Call the default behaviour
	        return super.onKeyDown(keyCode, event);
		}
        return true;
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (savedInstanceState != null) {
			mToShowValue = savedInstanceState.getBooleanArray(EXTRA_SHOW_VALUES);
			mAlphaValue = savedInstanceState.getInt(EXTRA_ALPHA_VALUE, 100);
			mToShowTopRight = savedInstanceState.getBoolean(EXTRA_SHOW_RIGHT, true);
			mToAverageCPU = savedInstanceState.getBoolean(EXTRA_AVERAGE_CPU, false);
			mToAverageGPU = savedInstanceState.getBoolean(EXTRA_AVERAGE_GPU, false);
			mIsBound = savedInstanceState.getBoolean(EXTRA_IS_BOUND, false);
		}
		else {
			// Initialize all the values
			loadPreferences();
		}
		restoreUI();
		
		//Start the background service
		doBindService();
		
		setupViews();
	}

	private void restoreUI() {
        CheckedTextView ctv;
		//Show FPS
        //Not implemented mToShowValue[HWCounter.FPS.getPosition()]
		
		//Average CPU Load
        ctv = (CheckedTextView) findViewById(R.id.show_smooth_cpu);
		ctv.setChecked(mToAverageCPU);
		
		//Average GPU Load
        ctv = (CheckedTextView) findViewById(R.id.show_smooth_gpu);
		ctv.setChecked(mToAverageGPU);
		
		//Load CPU
		ctv = (CheckedTextView) findViewById(R.id.show_cpu);
		ctv.setChecked(mToShowValue[HWCounter.LOAD_CPU.getPosition()] || mToShowValue[HWCounter.LOAD_CPU_C0.getPosition()]);
		
		//Show Multi-core
		ctv = (CheckedTextView) findViewById(R.id.show_multi_core);
		ctv.setChecked(mToShowValue[HWCounter.LOAD_CPU_C0.getPosition()]);
        
		//Show 2D core
        //Not implemented mToShowValue[HWCounter.LOAD_2D.getPosition()]
		
		//Show TA core
		ctv = (CheckedTextView) findViewById(R.id.show_ta);
		ctv.setChecked(mToShowValue[HWCounter.LOAD_TA.getPosition()]);
		
		//Show 3D core
		ctv = (CheckedTextView) findViewById(R.id.show_3d);
		ctv.setChecked(mToShowValue[HWCounter.LOAD_3D.getPosition()]);
		
        //Show TSP
		// Not implemented mToShowValue[HWCounter.LOAD_TSP.getPosition()]
		
		//Show Load Pixel
		ctv = (CheckedTextView) findViewById(R.id.show_pixel);
		ctv.setChecked(mToShowValue[HWCounter.LOAD_PIXEL.getPosition()]);
		
		//Show Load Vertex
		ctv = (CheckedTextView) findViewById(R.id.show_vertex);
		ctv.setChecked(mToShowValue[HWCounter.LOAD_VERTEX.getPosition()]);
		
		// Left and right corners
		ctv = (CheckedTextView) findViewById(R.id.position_left);
		ctv.setChecked(!mToShowTopRight);
		ctv = (CheckedTextView) findViewById(R.id.position_right);
		ctv.setChecked(mToShowTopRight);

	}

	@Override
	protected void onDestroy() {
		//Stop and unbind the service
		unbindService(mConnection);
		
		//Destroy activity
		super.onDestroy();
	}
	
	private void savePreferences() {
		SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		
		// Save the UI states
		editor.putBoolean(EXTRA_SHOW_RIGHT, mToShowTopRight);
		editor.putInt(EXTRA_ALPHA_VALUE, mAlphaValue);
		
		// Save the CPU averaging 
		editor.putBoolean(EXTRA_AVERAGE_CPU, mToAverageCPU);
		
		// Save the CPU averaging 
		editor.putBoolean(EXTRA_AVERAGE_GPU, mToAverageGPU);
		
		// Save the values
		for (int i = 0; i < mToShowValue.length; ++i) {
			editor.putBoolean(EXTRA_SHOW_VALUES + "_" + i, mToShowValue[i]);
		}
		
		editor.commit();
	}
	
	private void loadPreferences() {
		SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
		
		// Load the UI states
		mToShowTopRight = prefs.getBoolean(EXTRA_SHOW_RIGHT, true);
		mAlphaValue = prefs.getInt(EXTRA_ALPHA_VALUE, 100);
		
		// Load the CPU averaging 
		mToAverageCPU = prefs.getBoolean(EXTRA_AVERAGE_CPU, false);
		
		// Load the GPU averaging 
		mToAverageGPU = prefs.getBoolean(EXTRA_AVERAGE_GPU, false);
		
		// Set the defaults
        mToShowValue[HWCounter.FPS.getPosition()] = false;
        
        mToShowValue[HWCounter.LOAD_CPU.getPosition()] = true;
        mToShowValue[HWCounter.LOAD_CPU_C0.getPosition()] = false;
        mToShowValue[HWCounter.LOAD_CPU_C1.getPosition()] = false;
        mToShowValue[HWCounter.LOAD_CPU_C2.getPosition()] = false;
        mToShowValue[HWCounter.LOAD_CPU_C3.getPosition()] = false;
        mToShowValue[HWCounter.LOAD_CPU_C4.getPosition()] = false;
        mToShowValue[HWCounter.LOAD_CPU_C5.getPosition()] = false;
        mToShowValue[HWCounter.LOAD_CPU_C6.getPosition()] = false;
        mToShowValue[HWCounter.LOAD_CPU_C7.getPosition()] = false;
        
        mToShowValue[HWCounter.LOAD_2D.getPosition()] = false;	
        mToShowValue[HWCounter.LOAD_3D.getPosition()] = true;
        mToShowValue[HWCounter.LOAD_TA.getPosition()] = true;
        
        mToShowValue[HWCounter.LOAD_TSP.getPosition()] = false;
        mToShowValue[HWCounter.LOAD_PIXEL.getPosition()] = false;
        mToShowValue[HWCounter.LOAD_VERTEX.getPosition()] = false;	
        
		// Load the values
		for (int i = 0; i < mToShowValue.length; ++i) {
			 mToShowValue[i] = prefs.getBoolean(EXTRA_SHOW_VALUES + "_" + i, mToShowValue[i]);
		}
		
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onStop() {		
		super.onStop();
	}

	@Override
	protected void onPause() {
		// Save preferences
		savePreferences();
		
		super.onPause();
	}
	
	private void setEnabled(View v, int index) {
		CheckedTextView ctv = (CheckedTextView) v;
		ctv.setChecked(!ctv.isChecked());
		
		mToShowValue[index] = ctv.isChecked();
		if (mBoundService != null) {
			mBoundService.setEnabledValues(mToShowValue);
		}
	}
	
	public void onShowFPS(View v) {
		setEnabled(v, HWCounter.FPS.getPosition());
	}
	
	public void onShowEachCore(View v) {
		CheckedTextView ctv = (CheckedTextView) v;
		ctv.setChecked(!ctv.isChecked());
		
		CheckedTextView ctv_cpu = (CheckedTextView) findViewById(R.id.show_cpu);
		
		if (ctv_cpu.isChecked()) {
			for (int i = 0; i < CPUMetricsReading.mNumCores; ++i) {
				mToShowValue[i + HWCounter.LOAD_CPU_C0.getPosition()] = ctv.isChecked();
			}
		}
		
		if (ctv.isChecked()) {
			mToShowValue[HWCounter.LOAD_CPU.getPosition()] = false;
		}
		else {
			if (ctv_cpu.isChecked()) {
				mToShowValue[HWCounter.LOAD_CPU.getPosition()] = true;
			}
		}
		
		if (mBoundService != null) {
			mBoundService.setEnabledValues(mToShowValue);
		}
	}
	
	public void onShowCPULoad(View v) {
		CheckedTextView ctv = (CheckedTextView) v;
		ctv.setChecked(!ctv.isChecked());
		CheckedTextView ctv_multi = (CheckedTextView) findViewById(R.id.show_multi_core);
		
		
		if (ctv.isChecked()) {
			mToShowValue[HWCounter.LOAD_CPU.getPosition()] = !ctv_multi.isChecked();
			for (int i = 0; i < CPUMetricsReading.mNumCores; ++i) {
				mToShowValue[i + HWCounter.LOAD_CPU_C0.getPosition()] = ctv_multi.isChecked();
			}
		}
		else {
			ctv_multi.setChecked(false);
			mToShowValue[HWCounter.LOAD_CPU.getPosition()] = ctv.isChecked();
			for (int i = 0; i < CPUMetricsReading.mNumCores; ++i) {
				mToShowValue[i + HWCounter.LOAD_CPU_C0.getPosition()] = false;
			}
		}
		if (mBoundService != null) {
			mBoundService.setEnabledValues(mToShowValue);
		}
	}
	
	public void onShow3DLoad(View v) {
		setEnabled(v, HWCounter.LOAD_3D.getPosition());
	}
	
	public void onShowTALoad(View v) {
		setEnabled(v, HWCounter.LOAD_TA.getPosition());
	}
	
	public void onShowTSPLoad(View v) {
		setEnabled(v, HWCounter.LOAD_TSP.getPosition());
	}
	
	public void onShowPixelLoad(View v) {
		setEnabled(v, HWCounter.LOAD_PIXEL.getPosition());
	}
	
	public void onShowVertexLoad(View v) {
		setEnabled(v, HWCounter.LOAD_VERTEX.getPosition());
	}
	
	public void onPositionRight(View v) {
		CheckedTextView ctv = (CheckedTextView) v;
		ctv.setChecked(true);
		
		CheckedTextView ctvLeft = (CheckedTextView) findViewById(R.id.position_left);
		ctvLeft.setChecked(false);
		
		if (mBoundService != null) {
			mBoundService.showInTopRight(true);
			mToShowTopRight = true;
		}
	}
	
	public void onPositionLeft(View v) {
		CheckedTextView ctv = (CheckedTextView) v;
		ctv.setChecked(true);
		
		CheckedTextView ctvRight = (CheckedTextView) findViewById(R.id.position_right);
		ctvRight.setChecked(false);
		
		if (mBoundService != null) {
			mBoundService.showInTopRight(false);
			mToShowTopRight = false;
		}
	}
	
	public void onAverageCPU(View v) {
		CheckedTextView ctv = (CheckedTextView) v;
		ctv.setChecked(!ctv.isChecked());
		mToAverageCPU = ctv.isChecked();
		
		if (mBoundService != null) {
			mBoundService.setAverageCPUEnabled(mToAverageCPU);
		}
	}
	
	public void onAverageGPU(View v) {
		CheckedTextView ctv = (CheckedTextView) v;
		ctv.setChecked(!ctv.isChecked());
		mToAverageGPU = ctv.isChecked();
		
		if (mBoundService != null) {
			mBoundService.setAverageGPUEnabled(mToAverageGPU);
		}
	}
	
	private void setPVRScopeDisabled() {
		Toast toast = Toast.makeText(this, "PVRScope not found in this system. Is this a PowerVR enabled device?", Toast.LENGTH_LONG);
		toast.show();
		
		//Disable the 3D Load counter
		CheckedTextView ctv = (CheckedTextView) findViewById(R.id.show_3d);
		ctv.setEnabled(false);
		
		//Disable the TA Load counter
		ctv = (CheckedTextView) findViewById(R.id.show_ta);
		ctv.setEnabled(false);
		
		//Disable the TSP Load counter
		//ctv = (CheckedTextView) findViewById(R.id.show_tsp);
		//ctv.setEnabled(false);
		
		//Disable the Pixel Load counter
		ctv = (CheckedTextView) findViewById(R.id.show_pixel);
		ctv.setEnabled(false);
		
		//Disable the Vertex Load counter
		ctv = (CheckedTextView) findViewById(R.id.show_vertex);
		ctv.setEnabled(false);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	Intent i = null; 
        // Handle your other action bar items...
        switch (item.getItemId()) {
        	case R.id.action_about:
	        	AboutDialog dialog = new AboutDialog();
	        	dialog.show(getFragmentManager(), "Information");
	        	break;
        }

        return super.onOptionsItemSelected(item);
    }

}
