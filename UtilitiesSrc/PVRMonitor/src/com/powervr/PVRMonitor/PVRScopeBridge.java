/******************************************************************************

 @File         PVRScopeBridge.java

 @Title        PVRScopeBridge

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  The bridge between Java and native code.

******************************************************************************/
package com.powervr.PVRMonitor;

enum HWCounterType {
	TEXT,
	BAR,
	COMBINED
}

enum HWCounter {
	FPS ("FPS", 0, HWCounterType.TEXT),
	LOAD_CPU ("CPU", 1, HWCounterType.BAR),
	LOAD_CPU_C0 ("CPU 0", 2, HWCounterType.COMBINED),
	LOAD_CPU_C1 ("CPU 1", 3, HWCounterType.COMBINED),
	LOAD_CPU_C2 ("CPU 2", 4, HWCounterType.COMBINED),
	LOAD_CPU_C3 ("CPU 3", 5, HWCounterType.COMBINED),
	LOAD_CPU_C4 ("CPU 4", 6, HWCounterType.COMBINED),
	LOAD_CPU_C5 ("CPU 5", 7, HWCounterType.COMBINED),
	LOAD_CPU_C6 ("CPU 6", 8, HWCounterType.COMBINED),
	LOAD_CPU_C7 ("CPU 7", 9, HWCounterType.COMBINED),
	LOAD_2D ("2D", 10, HWCounterType.BAR),
	LOAD_3D ("Total Pixel", 11, HWCounterType.BAR),  //3D
	LOAD_TA ("Total Vertex", 12, HWCounterType.BAR), //TA
	LOAD_TSP ("TSP", 13, HWCounterType.BAR),
	LOAD_PIXEL ("ALU: Pixel", 14, HWCounterType.BAR),
	LOAD_VERTEX ("ALU: Vertex", 15, HWCounterType.BAR);
	
	private final String mName;
	private final int mPosition;
	private final HWCounterType mType;
	
	private HWCounter(String name, int position, HWCounterType type) {
		mName = name;
		mPosition = position;
		mType = type;
	}
	
	public int getPosition() { return mPosition; }
	public String getName() { return mName; }
	public HWCounterType getType() { return mType; }
	
	public static HWCounter getByIndex(int i) {
		switch (i) {
			case 0:
				return FPS;
			case 1:
				return LOAD_CPU;
			case 2:
				return LOAD_CPU_C0;
			case 3:
				return LOAD_CPU_C1;
			case 4:
				return LOAD_CPU_C2;
			case 5:
				return LOAD_CPU_C3;
			case 6:
				return LOAD_CPU_C4;
			case 7:
				return LOAD_CPU_C5;
			case 8:
				return LOAD_CPU_C6;
			case 9:
				return LOAD_CPU_C7;
			case 10:
				return LOAD_2D;
			case 11:
				return LOAD_3D;
			case 12:
				return LOAD_TA;
			case 13:
				return LOAD_TSP;
			case 14:
				return LOAD_PIXEL;
			case 15:
				return LOAD_VERTEX;
		}
		return null;
	}
}

public class PVRScopeBridge {
	public static final int TYPE_CPU_READING = 0;
	public static final int TYPE_PVRSCOPE_READING = 1;
	
	// Native functions
    public native boolean initPVRScope();
    public native boolean deinitPVRScope();
    
    private native byte[] returnCPUMetrics();
    private native byte[] returnPVRScope();

    static {
        System.loadLibrary("PVRScopeHUD");
    }

    public void readData(HUDView mView) {
		if (mView == null) return;
		
		byte dataSubset[] = null;
		PVRScopeReading pvrScopeReading = null;
		CPUMetricsReading cpuMetricsReading = null;
		
		// Get the CPU metrics
		dataSubset = returnCPUMetrics();
		if (dataSubset.length > 0) {
			cpuMetricsReading = new CPUMetricsReading(dataSubset, dataSubset.length);
			mView.addCPUMetricsReading(cpuMetricsReading);
		}
		
		// Get the PVRScope data
		dataSubset = returnPVRScope();
		if (dataSubset.length == 8) {
			pvrScopeReading = new PVRScopeReading(dataSubset, 8);
			mView.addPVRScopeReading(pvrScopeReading);
		}

		if (pvrScopeReading != null && cpuMetricsReading != null) { 
			mView.postInvalidate();
		}

    }
}
