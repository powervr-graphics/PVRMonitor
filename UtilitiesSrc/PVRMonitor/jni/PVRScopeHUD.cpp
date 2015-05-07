/******************************************************************************

 @File         PVRScopeHUD.cpp

 @Title        PVRScopeHUD

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Retrieves counter data via PVRScope.

******************************************************************************/
#include <unistd.h>
#include <string.h>

#include "common.h"
#include "PVRScopeHUD.h"

PVRScopeHUD::PVRScopeHUD()
{
	m_pPVRScopeCounterDef = NULL;
	m_pPVRScopeData = NULL;

	m_nCounters = 0;

	m_nActiveGroupSelect = 0;
	m_bActiveGroupChanged = true;

	//Set indexes to -1
	m_sPVRScopeCounterIndex.FPS = -1;
	m_sPVRScopeCounterIndex._2DLoad = -1;
	m_sPVRScopeCounterIndex._3DLoad = -1;
	m_sPVRScopeCounterIndex.TALoad = -1;
	m_sPVRScopeCounterIndex.TSPLoad = -1;
	m_sPVRScopeCounterIndex.pixelLoad = -1;
	m_sPVRScopeCounterIndex.vertexLoad = -1;
}

PVRScopeHUD::~PVRScopeHUD() {}

bool PVRScopeHUD::initialisePVRScope()
{
	//Initialise PVRScope
	const EPVRScopeInitCode eInitCode = PVRScopeInitialise(&m_pPVRScopeData);

	if(ePVRScopeInitCodeOk == eInitCode)
	{
		LOGI("Initialised services connection.\n");
	}
	else
	{
		LOGE("Error: failed to initialise services connection.\n");
		m_pPVRScopeData = NULL;
		return false;
	}
    
    //Initialise the counter data structures.
    if (PVRScopeGetCounters(m_pPVRScopeData, &m_nCounters, &m_pPVRScopeCounterDef, &m_sPVRScopeCounterReading))
    {
		LOGI("Total counters enabled: %d.", m_nCounters);

		//Assign the counter index to the struct
		m_sPVRScopeCounterIndex.FPS			= PVRScopeFindStandardCounter(m_nCounters, m_pPVRScopeCounterDef, m_nActiveGroupSelect, ePVRScopeStandardCounter_FPS);
		m_sPVRScopeCounterIndex._2DLoad		= PVRScopeFindStandardCounter(m_nCounters, m_pPVRScopeCounterDef, m_nActiveGroupSelect, ePVRScopeStandardCounter_Load_2D);
		m_sPVRScopeCounterIndex._3DLoad		= PVRScopeFindStandardCounter(m_nCounters, m_pPVRScopeCounterDef, m_nActiveGroupSelect, ePVRScopeStandardCounter_Load_Renderer);
		m_sPVRScopeCounterIndex.TALoad		= PVRScopeFindStandardCounter(m_nCounters, m_pPVRScopeCounterDef, m_nActiveGroupSelect, ePVRScopeStandardCounter_Load_Tiler);
		m_sPVRScopeCounterIndex.pixelLoad	= PVRScopeFindStandardCounter(m_nCounters, m_pPVRScopeCounterDef, m_nActiveGroupSelect, ePVRScopeStandardCounter_Load_Shader_Pixel);
		m_sPVRScopeCounterIndex.vertexLoad	= PVRScopeFindStandardCounter(m_nCounters, m_pPVRScopeCounterDef, m_nActiveGroupSelect, ePVRScopeStandardCounter_Load_Shader_Vertex);

		/* DEBUG only */
		for(int i = 0; i < m_nCounters; ++i)
		{
			LOGI("    Group %d %s", m_pPVRScopeCounterDef[i].nGroup, m_pPVRScopeCounterDef[i].pszName);
		}
	}
	
	return true;
}

void PVRScopeHUD::deinitialisePVRScope()
{
	if (m_pPVRScopeData != NULL)
	{
		PVRScopeDeInitialise(&m_pPVRScopeData, &m_pPVRScopeCounterDef, &m_sPVRScopeCounterReading);
	}
	
	m_nCounters = 0;

	m_nActiveGroupSelect = 0;
	m_bActiveGroupChanged = true;
}

bool PVRScopeHUD::readCounters(bool toReturnData)
{
	//Check that PVRScope has been initialized
	if (m_pPVRScopeData == NULL) return false;

	//Ask for the active group 0 only on the first run.
	if(m_bActiveGroupChanged)
	{
		PVRScopeSetGroup(m_pPVRScopeData, m_nActiveGroupSelect);
		m_bActiveGroupChanged = false;
	}

	//Read the counter information
	SPVRScopeCounterReading *psReading = NULL;
	if (toReturnData)
	{
		psReading = &m_sPVRScopeCounterReading;

	}

	if(PVRScopeReadCounters(m_pPVRScopeData, psReading))
	{
		if (psReading == NULL) return false;
#if DUMP_PVRSCOPE_DATA
		/* DEBUG only */
		int index = 0;
		for(int i = 0; i < m_nCounters; ++i)
		{
			if ((m_pPVRScopeCounterDef[i].nGroup == 0) ||
				(m_pPVRScopeCounterDef[i].nGroup == 0xFFFFFFFF))
			{
				if(index < m_sPVRScopeCounterReading.nValueCnt)
				{
					//If it is a percentage output the % symbol:
					if(m_pPVRScopeCounterDef[i].nBoolPercentage)
						LOGI("%s : %f\%", m_pPVRScopeCounterDef[i].pszName, m_sPVRScopeCounterReading.pfValueBuf[index]);
					else
						LOGI("%s : %f", m_pPVRScopeCounterDef[i].pszName, m_sPVRScopeCounterReading.pfValueBuf[index]);
				}
				++index;
			}
		}
		if(index != m_sPVRScopeCounterReading.nValueCnt)
		{
			LOGI("Expected %u results, got %u.", index, m_sPVRScopeCounterReading.nValueCnt);
		}
#endif
		return true;
	}
	return false;
}

float PVRScopeHUD::getCounter(EPVRScopeCounter eCounter)
{
	float retVal = 0;
	int index = -1;
	switch (eCounter)
	{
		case eFPS:
			index = m_sPVRScopeCounterIndex.FPS;
			break;
		case e2DLoad:
			index = m_sPVRScopeCounterIndex._2DLoad;
			break;
		case e3DLoad:
			index = m_sPVRScopeCounterIndex._3DLoad;
			break;
		case eTALoad:
			index = m_sPVRScopeCounterIndex.TALoad;
			break;
		case eTSPLoad:
			index = m_sPVRScopeCounterIndex.TSPLoad;
			break;
		case ePixelLoad:
			index = m_sPVRScopeCounterIndex.pixelLoad;
			break;
		case eVertexLoad:
			index = m_sPVRScopeCounterIndex.vertexLoad;
			break;
		default:
			break;
	}
	if (index >= 0 && index < m_sPVRScopeCounterReading.nValueCnt)
	{
		retVal = m_sPVRScopeCounterReading.pfValueBuf[index];
	}

	// Sanitize the value
	if (retVal > 100) retVal = 0;

	return retVal;
}
