/******************************************************************************

 @File         PVRMonitor.cpp

 @Title        PVRMonitor

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Handles the application's life cycle.

 ******************************************************************************/
package com.powervr.PVRMonitor;



import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.Vector;


public class PVRMonitor extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
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
    private boolean mToAverageGPU = true;
    private final static String EXTRA_SHOW_VALUES = "extra_mCountersEnabled";
    private final static String EXTRA_SHOW_VALUES_OPTIONS = "extra_mCountersEnabled_options";
    private Context mContext;
    private final static String EXTRA_CPU_ENABLED = "extra_mCpuEnabled";
    private final static String EXTRA_SHOW_VALUES_LENGTH= "extra_mCountersEnabled_length";

    Vector<CounterInUse> mCountersSelected = new Vector<>();

    // Back key handling
    private boolean mIsBackKeyPressed = false;

    private boolean mGotOverlayPermission = false;
    private int mAppReloadAttempts = 0;

    private float mXTouchOffset;
    private float mYTouchOffset;

    private boolean mDragging = false;
    private boolean mFreeMove = false;
    private boolean mCPUEnabled = false;

    private int mMaxNumCounters = 5;
    private String[] mScopeStrings;
    private int[] mStandardCounterIndexes = new int[5];
    private int[] mStandardCounterCTVs = {R.id.show_fps,R.id.show_3d, R.id.show_ta, R.id.show_pixel, R.id.show_vertex};
    private int[] mStandardCounterSpinners = {R.id.select_FPS, R.id.select_3D, R.id.select_ta, R.id.select_pixel, R.id.select_vertex};
    String mStandardCounterNames[] = {"Frames per second (FPS)", "Renderer active (%)" , "Tiler active (%)", "Processing load: pixel (%)", "Processing load: vertex (%)"};

    private Bundle mInstanceState;


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
            mIsBackKeyPressed = false;
            super.onPostExecute(v);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((HUD.LocalBinder)service).getService();

            mBoundService.setCountersSelected(mCountersSelected, mCPUEnabled);
            mBoundService.showInTopRight(mToShowTopRight);
            mBoundService.setOpacity(mAlphaValue);
            mBoundService.setAverageCPUEnabled(mToAverageCPU);
            mBoundService.setAverageGPUEnabled(mToAverageGPU);

            //Check if PVRScope is enabled or not
            if (mBoundService.mIsPVRScopeDisabled) setPVRScopeDisabled();
            else {
                //get extended values strings
                getStrings();
            }

            mIsBound = true;

            //TODO: get saved instance state here?
            if(mInstanceState != null) {
                mAlphaValue = mInstanceState.getInt(EXTRA_ALPHA_VALUE, 100);
                mToShowTopRight = mInstanceState.getBoolean(EXTRA_SHOW_RIGHT, true);
                mToAverageCPU = mInstanceState.getBoolean(EXTRA_AVERAGE_CPU, true);
                mToAverageGPU = mInstanceState.getBoolean(EXTRA_AVERAGE_GPU, true);
                mIsBound = mInstanceState.getBoolean(EXTRA_IS_BOUND, false);

                int intArray[] = mInstanceState.getIntArray(EXTRA_SHOW_VALUES);
                int optionIntArray[] = mInstanceState.getIntArray(EXTRA_SHOW_VALUES_OPTIONS);
                if (intArray != null && optionIntArray != null) {
                    for (int i = 0; i < intArray.length; i++) {


                        boolean standardCounter = false;
                        for(int j = 0; j < mStandardCounterIndexes.length; j++)
                        {
                            boolean alreadyContained = false;
                            for(int k = 0; k < mCountersSelected.size(); k++)
                            {
                                if(mCountersSelected.get(k).mIndex == mStandardCounterIndexes[j]) {
                                    alreadyContained = true;
                                    standardCounter = true;
                                }
                            }

                            if(intArray[i] == mStandardCounterIndexes[j] && !alreadyContained){
                                CheckedTextView v = (CheckedTextView)findViewById(mStandardCounterCTVs[j]);
                                v.setChecked(false);
                                Spinner spinner = (Spinner)findViewById(mStandardCounterSpinners[j]);
                                spinner.setSelection(optionIntArray[i]);
                                setEnabled(v, mStandardCounterNames[j], mStandardCounterIndexes[j], optionIntArray[i], spinner.getId());
                                standardCounter = true;
                            }
                        }
                        if(!standardCounter)
                            onAddValue(new View(mContext), intArray[i], optionIntArray[i]);
                    }
                }
            } else{
                loadPreferences();
            }

        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            mIsBound = false;
        }
    };

    private void getStrings()
    {
        mScopeStrings = mBoundService.getStrings();

        for(int i = 0; i < mScopeStrings.length; i++){
            //get indexes for standard counters.
            for(int j = 0; j < mStandardCounterNames.length; j++){
                if(mScopeStrings[i].equals(mStandardCounterNames[j])){
                    mStandardCounterIndexes[j] = i;
                    continue;
                }
            }

        }
    }

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
        savedInstanceState.putBoolean(EXTRA_IS_BOUND, mIsBound);
        savedInstanceState.putBoolean(EXTRA_CPU_ENABLED, mCPUEnabled);

        int intArray[] = new int[mCountersSelected.size()];
        int optionIntArray[] = new int[mCountersSelected.size()];
        for(int i = 0; i < mCountersSelected.size(); i++){
            intArray[i] = mCountersSelected.elementAt(i).mIndex;
            optionIntArray[i] = mCountersSelected.elementAt(i).mOption;
        }
        savedInstanceState.putIntArray(EXTRA_SHOW_VALUES, intArray);
        savedInstanceState.putIntArray(EXTRA_SHOW_VALUES_OPTIONS, optionIntArray);

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

    private void setupSpinners()
    {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Spinner cpuSpinner = (Spinner) findViewById(R.id.select_CPU);
            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<CharSequence> cpuAdapter = ArrayAdapter.createFromResource(this, R.array.CPU_array, android.R.layout.simple_spinner_item);
            // Specify the layout to use when the list of choices appears
            cpuAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            cpuSpinner.setAdapter(cpuAdapter);

            cpuSpinner.setSelection(0, false);
            cpuSpinner.setOnItemSelectedListener(this);
        }


        int idArray [] = new int[]{R.id.select_FPS, R.id.select_3D,R.id.select_pixel,R.id.select_ta,R.id.select_vertex};

        for(int i = 0; i < idArray.length; i++)
        {
            Spinner spinner = (Spinner) findViewById(idArray[i]);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.load_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            spinner.setSelection(0, false);
            spinner.setOnItemSelectedListener(this);
        }
    }

    static int stringSpinnerIDStart = 221096;
    static int valueSpinnerIDStart = 961022;
    static int horizLayoutIDStart = 110296;
    static int checkBoxIDStart = 960211;

    int ID = 0;

    public void onAddValue(View v){
        for(int i = 0; i < mScopeStrings.length; i++){
            boolean contained = false;
            for(int j = 0; j < ID; j++){
                Spinner spinner = (Spinner)findViewById(j + stringSpinnerIDStart);
                if(spinner == null)
                    continue;
                if(spinner.getSelectedItemPosition() == i)
                    contained = true;
            }
            if(!contained) {
                onAddValue(v, i, 0);
                return;
            }
        }

        Toast toast = Toast.makeText(this, "No more counters to list.", Toast.LENGTH_LONG);
        toast.show();

    }

    public void onAddValue(View v, int defaultValue, int option) {
        //set up extended values.

        if(mCountersSelected.size() + (mCPUEnabled ? 1 : 0) >= mMaxNumCounters){
           Toast toast = Toast.makeText(this, "Reached maximum number of counters (" + mMaxNumCounters + ") disable other counters to enable this one.", Toast.LENGTH_LONG);
           toast.show();
           return;
        }
        //overall vertical layout.
        LinearLayout linearLayout = findViewById(R.id.extended_value_list);

        //child horizontal layout.
        LinearLayout horizLayout = new LinearLayout(getApplicationContext());

        horizLayout.setOrientation(LinearLayout.HORIZONTAL);

        linearLayout.addView(horizLayout);

        ViewGroup.LayoutParams horizParams = horizLayout.getLayoutParams();


        //get screen width in dp
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        float density  = getResources().getDisplayMetrics().density;
        float dpWidth  = outMetrics.widthPixels / density;
        //convert pixels to DIP
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dpWidth > 500 ? 432 : 332, getResources().getDisplayMetrics());
        horizParams.width = width;
        horizParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        horizLayout.setLayoutParams(horizParams);
        horizLayout.setId(horizLayoutIDStart + ID);


        final Spinner stringSpinner = new Spinner(getApplicationContext());
        final Spinner stringSpinnerOption = new Spinner(getApplicationContext());


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,   android.R.layout.simple_spinner_item, mScopeStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stringSpinner.setAdapter(adapter);
        stringSpinner.setId(stringSpinnerIDStart + ID);

        stringSpinner.setSelection(defaultValue, false);
        stringSpinner.setOnItemSelectedListener(this);
        stringSpinner.getBackground().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

        final CheckBox checkBox = new CheckBox(getApplicationContext());
        checkBox.setId(checkBoxIDStart + ID);
        checkBox.setChecked(true);
        final int checkBoxID = ID;
        checkBox.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                //send it a correct index
                setEnabledCheckBox(v,stringSpinner.getSelectedItem().toString(), stringSpinner.getSelectedItemPosition(), stringSpinnerOption.getSelectedItemPosition(), checkBoxID);
            }
        });
        horizLayout.addView(checkBox);
        horizLayout.addView(stringSpinner);

        ViewGroup.LayoutParams spParams;
        width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dpWidth > 500 ? 218 : 168, getResources().getDisplayMetrics());
        spParams = stringSpinner.getLayoutParams();
        spParams.width = width;
        spParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        stringSpinner.setLayoutParams(spParams);

        View view = new View(getApplicationContext());
        horizLayout.addView(view);

        LinearLayout.LayoutParams linearParams = new LinearLayout.LayoutParams(0, 0, 1.0f);
        view.setLayoutParams(linearParams);


        ArrayAdapter<CharSequence> adapterOption = ArrayAdapter.createFromResource(this, R.array.load_array, R.layout.simple_spinner_right_aligned);
        adapterOption.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stringSpinnerOption.setAdapter(adapterOption);
        stringSpinnerOption.setId(valueSpinnerIDStart + ID);
        stringSpinnerOption.getBackground().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        stringSpinnerOption.setSelection(option, false);
        stringSpinnerOption.setOnItemSelectedListener(this);

        horizLayout.addView(stringSpinnerOption);

        ViewGroup.LayoutParams spoParams;

        spoParams = stringSpinnerOption.getLayoutParams();
        width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpWidth > 500 ? 150 : 100, getResources().getDisplayMetrics());
        spoParams.width = width;
        spoParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        stringSpinnerOption.setLayoutParams(spoParams);


        Button button = new Button(getApplicationContext());
        button.setBackgroundResource(R.drawable.bin32);

        ViewGroup.LayoutParams buttonParams;

        horizLayout.addView(button);
        buttonParams = button.getLayoutParams();
        width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,32, getResources().getDisplayMetrics());
        buttonParams.width = width;
        buttonParams.height = width;
        button.setLayoutParams(buttonParams);

        //pass a final index to keep it constant for future click calls.
        final int clickIndex = ID;

        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                //send it a correct index
                onRemoveValue(v,  clickIndex);
            }
        });

        mCountersSelected.add(new CounterInUse(stringSpinner.getSelectedItem().toString(), stringSpinner.getSelectedItemPosition(), option, ID));
        ID++;

        if (mBoundService != null) {
            mBoundService.setCountersSelected(mCountersSelected, mCPUEnabled);
        }
    }

    public void onRemoveValue(View v, int counterNumber) {
        //remove counter from extended array
        for(int i = 0; i < mCountersSelected.size(); i++){
            if(mCountersSelected.elementAt(i).mId == counterNumber){
                mCountersSelected.remove(i);
                break;
            }
        }

        //remove horizontal linear layout containing counter.
        LinearLayout toDelete = findViewById(horizLayoutIDStart + counterNumber);
        LinearLayout parent = findViewById(R.id.extended_value_list);
        parent.removeView(toDelete);

        if (mBoundService != null) {
            mBoundService.setCountersSelected(mCountersSelected, mCPUEnabled);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

        boolean enableCounter = true;
        //disregard initial pass of this function.
        if(mCountersSelected.size() == 0){
            return;
        }


        for(int i = 0; i < mCountersSelected.size(); i++){
            if(mCountersSelected.elementAt(i).mIndex == parent.getSelectedItemPosition()) {
                Toast toast = Toast.makeText(this, "This counter is already enabled.", Toast.LENGTH_LONG);
                toast.show();
                enableCounter = false;
            }
        }

        for(int i = 0; i < mCountersSelected.size(); i++){
            //one of the standard counters
            if(parent.getId() == mCountersSelected.elementAt(i).mId) {
                mCountersSelected.get(i).mOption = parent.getSelectedItemPosition();
                mCountersSelected.get(i).ResetCounter();
                break;
            }

            if(parent.getId() == mCountersSelected.elementAt(i).mId + stringSpinnerIDStart) {
                //found one of the spinners - its an extended value name.
                if(enableCounter) {
                    mCountersSelected.get(i).mName = parent.getSelectedItem().toString();
                    mCountersSelected.get(i).mIndex = parent.getSelectedItemPosition();
                    mCountersSelected.get(i).ResetCounter();
                } else {
                    parent.setSelection(mCountersSelected.get(i).mIndex);
                }
                break;
            }

            if(parent.getId() == mCountersSelected.elementAt(i).mId + valueSpinnerIDStart){
                //found one of the spinners - its an extended value option.
                mCountersSelected.get(i).mOption = parent.getSelectedItemPosition();
                mCountersSelected.get(i).ResetCounter();
                break;
            }
        }

        if(mBoundService != null)
        mBoundService.updateUIOptions(mCountersSelected);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
                !mIsBackKeyPressed) {
            // Show the toast
            Toast myToast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.back_button_warning), Toast.LENGTH_LONG);
            myToast.show();

            // Flag back key as pressed
            mIsBackKeyPressed = true;

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


        boolean overlayRequested = false;
        mContext = this;
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mInstanceState = savedInstanceState;
        }
        else {
            // Initialize all the values
        }

        setContentView(R.layout.activity_pvrmonitor);

        if(!mGotOverlayPermission)
        {
            checkDrawOverlayPermission(overlayRequested);
            overlayRequested = true;

        }

        //If on this run we now have permission, perform initialisation.
        if(mGotOverlayPermission){
            restoreUI();

            //Start the background service
            doBindService();

            setupViews();

            setupSpinners();
        }


    }

    Handler handler = new Handler();
    Runnable checkOverlaySetting = new Runnable() {
        @Override
        @TargetApi(23)
        public void run() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return;
            }
            if (Settings.canDrawOverlays(PVRMonitor.this)) {
                //You have the permission, re-launch MainActivity
                Intent i = new Intent(PVRMonitor.this, PVRMonitor.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                return;
            }
            //try to re-load the app 20 times, if 20 seconds have passed stop in-case the user has decided not to allow permission.
            if(mAppReloadAttempts < 20)
            handler.postDelayed(this, 1000);
            mAppReloadAttempts++;
        }
    };

    static final int REQUEST_CODE = 22;
    @TargetApi(23)
    public void checkDrawOverlayPermission(boolean overlayRequested)
    {
        //if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            //Check if we have permission to draw over other apps already
            if (!Settings.canDrawOverlays(this)) {
                if(!overlayRequested) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));

                    startActivityForResult(intent, REQUEST_CODE);

                    handler.postDelayed(checkOverlaySetting, 1000);
                }
            } else {
                mGotOverlayPermission = true;
            }
        } else {
            if(Settings.canDrawOverlays(this))
                mGotOverlayPermission = true;
        }
    }

    @TargetApi(23)
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == REQUEST_CODE)
        {
            if(Settings.canDrawOverlays(this))
            {
                mGotOverlayPermission = true;
                finish();
                //Got permission to draw overlay.
            }
        }
    }


    private void restoreUI() {
        CheckedTextView ctvs[] = {findViewById(R.id.show_fps),findViewById(R.id.show_ta), findViewById(R.id.show_3d),  findViewById(R.id.show_pixel), findViewById(R.id.show_vertex)};

        CheckedTextView ctv;

        // Left and right corners
        ctv = (CheckedTextView) findViewById(R.id.position_left);
        ctv.setChecked(!mToShowTopRight);
        ctv = (CheckedTextView) findViewById(R.id.position_right);
        ctv.setChecked(mToShowTopRight);

    }

    @Override
    protected void onDestroy() {
        //Stop and unbind the service
        if(mIsBound)
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

        editor.putInt(EXTRA_SHOW_VALUES_LENGTH, mCountersSelected.size());
        // Save the values
        for (int i = 0; i < mCountersSelected.size(); ++i) {
            editor.putInt(EXTRA_SHOW_VALUES + "_" + i, mCountersSelected.elementAt(i).mIndex);
            editor.putInt(EXTRA_SHOW_VALUES_OPTIONS + "_" + i, mCountersSelected.elementAt(i).mOption);

        }

        editor.commit();
    }

    private void loadPreferences() {
        SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);

        // Load the UI states
        mToShowTopRight = prefs.getBoolean(EXTRA_SHOW_RIGHT, true);
        mAlphaValue = prefs.getInt(EXTRA_ALPHA_VALUE, 100);

        // Load the CPU averaging
        mToAverageCPU = prefs.getBoolean(EXTRA_AVERAGE_CPU, true);

        // Load the GPU averaging
        mToAverageGPU = prefs.getBoolean(EXTRA_AVERAGE_GPU, true);

        mCPUEnabled = prefs.getBoolean(EXTRA_CPU_ENABLED, false);

        int length = prefs.getInt(EXTRA_SHOW_VALUES_LENGTH, 0);
        // Set the defaults
        mCountersSelected.clear();

        for (int i = 0; i < length; i++) {
            int temp = prefs.getInt(EXTRA_SHOW_VALUES + "_" + i, -1);
            if (temp == -1 || temp > mScopeStrings.length)
                return;

            int option = prefs.getInt(EXTRA_SHOW_VALUES_OPTIONS + "_" + i, -1);
            if(option == -1)
                return;
            if(option > 1)
                option = 0;

            boolean standardCounter = false;
            for(int j = 0; j < mStandardCounterIndexes.length; j++)
            {
                if(temp == mStandardCounterIndexes[j]){
                    Spinner spinner = (Spinner)findViewById(mStandardCounterSpinners[j]);
                    spinner.setSelection(option);
                    setEnabled(findViewById(mStandardCounterCTVs[j]), mStandardCounterNames[j], mStandardCounterIndexes[j], option, spinner.getId());
                    standardCounter = true;
                }
            }
            if(!standardCounter)
                onAddValue(new View(mContext), temp, option);
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

    private void setEnabledCheckBox(View v, String name, int index, int option, int ID){
        CheckBox cb = (CheckBox) v;
        if(cb.isChecked()){

            // Check this counter is not already enabled somewhere
            for(int i = 0; i < mCountersSelected.size(); i++){
                if(mCountersSelected.elementAt(i).mIndex == index) {
                    Toast toast = Toast.makeText(this, "This counter is already enabled.", Toast.LENGTH_LONG);
                    toast.show();
                    cb.setChecked(false);
                    return;
                }
            }

            if(mCountersSelected.size() + (mCPUEnabled ? 1 : 0) < mMaxNumCounters) {
                mCountersSelected.add(new CounterInUse(name, index, option, ID));
            }
            else {
                Toast toast = Toast.makeText(this, "Reached maximum number of counters (" + mMaxNumCounters + ") disable other counters to enable this one.", Toast.LENGTH_LONG);
                toast.show();
            }
        } else {
            //need to remove the counter index appropriate to this ctv.
            for(int i = 0; i < mCountersSelected.size(); i++){
                if(mCountersSelected.elementAt(i).mIndex == index){
                    mCountersSelected.remove(i);
                    break;
                }
            }
        }


        if (mBoundService != null) {
            mBoundService.setCountersSelected(mCountersSelected, mCPUEnabled);
        }
    }

    private void setEnabled(View v, String name, int index, int option, int ID) {
       CheckedTextView ctv = (CheckedTextView) v;
        if(!ctv.isChecked()){
            ctv.setChecked(true);
            //Check if the counter is already enabled as an extended counter.
            for(int i = 0; i < mCountersSelected.size(); i++){
                if(mCountersSelected.elementAt(i).mIndex == index) {
                    Toast toast = Toast.makeText(this, "This counter is already enabled.", Toast.LENGTH_LONG);
                    toast.show();
                    ctv.setChecked(false);
                    return;
                }
            }

            if(mCountersSelected.size() + (mCPUEnabled ? 1 : 0) < mMaxNumCounters) {
                mCountersSelected.add(new CounterInUse(name, index, option, ID));
            }
            else {
                Toast toast = Toast.makeText(this, "Reached maximum number of counters (" + mMaxNumCounters + ") disable other counters to enable this one.", Toast.LENGTH_LONG);
                toast.show();
                ctv.setChecked(false);
            }
        } else {
            //need to remove the counter indice appropriate to this ctv.
            for(int i = 0; i < mCountersSelected.size(); i++){
                if(mCountersSelected.elementAt(i).mIndex == index){
                    mCountersSelected.remove(i);
                    break;
                }
            }
            ctv.setChecked(false);
        }


        if (mBoundService != null) {
            mBoundService.setCountersSelected(mCountersSelected, mCPUEnabled);
        }
    }

    public void onShowFPS(View v) {
        Spinner spinner = (Spinner)findViewById(R.id.select_FPS);
        setEnabled(v, "Frames per second (FPS)", mStandardCounterIndexes[0], spinner.getSelectedItemPosition(), spinner.getId());
    }

    public void onShowCPULoad(View v) {
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 && mCountersSelected.size() < mMaxNumCounters) {
            CheckedTextView ctv = (CheckedTextView) v;
            ctv.setChecked(!ctv.isChecked());
            mCPUEnabled = ctv.isChecked();
            mBoundService.setCountersSelected(mCountersSelected, mCPUEnabled);
        } else if (mCountersSelected.size() > (mMaxNumCounters - 1)) {
            Toast toast = Toast.makeText(this, "Reached maximum number of counters (" + mMaxNumCounters + ") disable other counters to enable this one.", Toast.LENGTH_LONG);
            toast.show();
        } else {
            Toast toast = Toast.makeText(this, "CPU counters currently non-functional above Android SDK 27 (Oreo_MR1) due to permission denial.", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void onShow3DLoad(View v) {
        Spinner spinner = (Spinner)findViewById(R.id.select_3D);
        setEnabled(v, "Renderer active (%)", mStandardCounterIndexes[1], spinner.getSelectedItemPosition(), spinner.getId());
    }

    public void onShowTALoad(View v) {
        Spinner spinner = (Spinner)findViewById(R.id.select_ta);
        setEnabled(v, "Tiler active (%)", mStandardCounterIndexes[2], spinner.getSelectedItemPosition(), spinner.getId());
    }

    public void onShowTSPLoad(View v) {
        // not implemented
        // setEnabled(v, HWCounter.LOAD_TSP.getPosition());
    }

    public void onShowPixelLoad(View v) {
        Spinner spinner = (Spinner)findViewById(R.id.select_pixel);
        setEnabled(v, "Processing load: pixel (%)", mStandardCounterIndexes[3], spinner.getSelectedItemPosition(), spinner.getId());
    }

    public void onShowVertexLoad(View v) {
        Spinner spinner = (Spinner)findViewById(R.id.select_vertex);
        setEnabled(v, "Processing load: vertex (%)", mStandardCounterIndexes[4], spinner.getSelectedItemPosition(), spinner.getId());
    }

    public void onPositionRight(View v) {
        CheckedTextView ctv = (CheckedTextView) v;
        ctv.setChecked(true);

        mFreeMove = false;

        CheckedTextView ctvLeft = (CheckedTextView) findViewById(R.id.position_left);
        ctvLeft.setChecked(false);

        CheckedTextView ctvFree = (CheckedTextView) findViewById(R.id.free_move);
        ctvFree.setChecked(false);

        if (mBoundService != null) {
            mBoundService.showInTopRight(true);
            mToShowTopRight = true;
        }
    }

    public void onPositionLeft(View v) {
        CheckedTextView ctv = (CheckedTextView) v;
        ctv.setChecked(true);

        mFreeMove = false;

        CheckedTextView ctvRight = (CheckedTextView) findViewById(R.id.position_right);
        ctvRight.setChecked(false);

        CheckedTextView ctvFree = (CheckedTextView) findViewById(R.id.free_move);
        ctvFree.setChecked(false);

        if (mBoundService != null) {
            mBoundService.showInTopRight(false);
            mToShowTopRight = false;
        }
    }

    public void onShowHUD(View v) {
        CheckedTextView ctv = (CheckedTextView) v;
        ctv.setChecked(!ctv.isChecked());

        if(ctv.isChecked()) {
            mBoundService.showHUD();
        } else {
            mBoundService.hideHUD();
        }
    }

    //Touch Handle for moving the overlay.
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){

        if(!mFreeMove)
            return super.dispatchTouchEvent(ev);

        int action = ev.getAction();

        switch(action) {
            case MotionEvent.ACTION_DOWN:
                float x = ev.getX();
                float y = ev.getY();
                Rect rect = mBoundService.getOverlay();
                //If within the bounding box of the overlay....
                if(rect.contains((int)x, (int)y)) {
                    mXTouchOffset = x - rect.left;
                    mYTouchOffset = y - rect.top;
                    mDragging = true;
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //If dragging
                if(mDragging) {
                    float x2 = ev.getX();
                    float y2 = ev.getY();

                    mBoundService.setViewPos(x2 - mXTouchOffset, y2 - mYTouchOffset);
                    return true;
                }

                break;
            case MotionEvent.ACTION_UP:
                mDragging = false;
                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    public void onFreeMove(View v) {
        CheckedTextView ctv = (CheckedTextView) v;
        ctv.setChecked(true);

        CheckedTextView ctvRight = (CheckedTextView) findViewById(R.id.position_right);
        CheckedTextView ctvLeft = (CheckedTextView) findViewById(R.id.position_left);

        ctvRight.setChecked(false);
        ctvLeft.setChecked(false);

        mFreeMove = true;
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

        //Disable the FPS Load counter
        CheckedTextView ctv = (CheckedTextView) findViewById(R.id.show_fps);
        ctv.setEnabled(false);

        //Disable the 3D Load counter
        ctv = (CheckedTextView) findViewById(R.id.show_3d);
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

        Button button = (Button) findViewById(R.id.add_value_button);
        button.setEnabled(false);
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
