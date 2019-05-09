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
#include <string>

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

    m_sPVRScopeCounterReading = {0};

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
    } else{
        LOGE("Failed to get counters");
        return false;
    }

    return true;
}

unsigned int PVRScopeHUD::getNumCounters()
{
    if (PVRScopeGetCounters(m_pPVRScopeData, &m_nCounters, &m_pPVRScopeCounterDef, &m_sPVRScopeCounterReading)) {

        if (m_sPVRScopeCounterReading.nValueCnt < m_nCounters && m_sPVRScopeCounterReading.nValueCnt > 0)
            return m_sPVRScopeCounterReading.nValueCnt;

        unsigned int num = 0;
        for(int i = 0; i < m_nCounters; i++){
            if(m_pPVRScopeCounterDef[i].nGroup == m_nActiveGroupSelect)
                num++;
        }
        return num;
    }
    else{
        return 0;
    }
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

        /* DEBUG only */
    }else{
        return false;
    }
    return true;
}


float PVRScopeHUD::getCounter(unsigned int index)
{
    float retVal = m_sPVRScopeCounterReading.pfValueBuf[index];

    if(m_pPVRScopeCounterDef[index].nBoolPercentage)
    {
        if(retVal > 100 || retVal < 0)
            return 0;
    }

    return retVal;
}

std::string PVRScopeHUD::getName(unsigned int index)
{
    std::string name = m_pPVRScopeCounterDef[index].pszName;
    if(m_pPVRScopeCounterDef[index].nBoolPercentage)
        name += " (%)";

    return name;
}