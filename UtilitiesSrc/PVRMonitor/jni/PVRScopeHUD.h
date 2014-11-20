/*!****************************************************************************

 @file         PVRScopeHUD.h
 @copyright    Copyright (c) Imagination Technologies Limited.
 @brief        Retrieves counter data via PVRScope.

******************************************************************************/
#ifndef _PVRSCOPEHUD_H_
#define _PVRSCOPEHUD_H_

#include "PVRScopeStats.h"

#if defined(__cplusplus)
extern "C"
#endif
{

/*
	Counters on the active group 0:
		Frame time
		X Frames per second (FPS)
		X GPU task load: 2D core
		X GPU task load: 3D core
		GPU task load: CDM core
		X GPU task load: TA core
		TA load
		X Texture unit(s) load
		USSE clock cycles per pixel
		USSE clock cycles per vertex
		X USSE load: Pixel
		X USSE load: Vertex
		Vertices per frame
		Vertices per second
*/
enum EPVRScopeCounter {
	eFPS = 1,
	e2DLoad,
	e3DLoad,
	eTALoad,
	eTSPLoad,
	ePixelLoad,
	eVertexLoad
};

typedef struct _SPVRScopeCounterIndex {
	int FPS;
	int _2DLoad;
	int _3DLoad;
	int TALoad;
	int TSPLoad;
	int pixelLoad;
	int vertexLoad;
} SPVRScopeCounterIndex;

class PVRScopeHUD
{
public:
	PVRScopeHUD();
	~PVRScopeHUD();
	
	bool initialisePVRScope();
	void deinitialisePVRScope();
	bool readCounters(bool toReturnData);
	
	float getCounter(EPVRScopeCounter eCounter);

private:
	//Internal control data
	SPVRScopeImplData		*m_pPVRScopeData;
	
	//Counter information (set at init time)
	SPVRScopeCounterDef		*m_pPVRScopeCounterDef;
	unsigned int			m_nCounters;
	
	//Counter reading data
	unsigned int			m_nActiveGroupSelect;
	bool					m_bActiveGroupChanged;

	SPVRScopeCounterReading	m_sPVRScopeCounterReading;

	SPVRScopeCounterIndex m_sPVRScopeCounterIndex;
};

}

#endif /* _PVRSCOPEHUD_H_ */
