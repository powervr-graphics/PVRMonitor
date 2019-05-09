/******************************************************************************

 @File         CPUMetrics.cpp

 @Title        CPUMetrics

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  Collects CPU metrics.

******************************************************************************/
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <sys/stat.h>

#include "common.h"
#include "CPUMetrics.h"

unsigned int getNumberOfCores()
{
    FILE * fp = fopen("/sys/devices/system/cpu/possible", "r");
    char buffer[1024];
    int nNumberProcessors = 1;

    if(fp)
    {
        size_t numBytesRead = fread(buffer, sizeof(char), 1024, fp);

        if(numBytesRead >= 3 && feof( fp ) &&
           buffer[0] == '0' && buffer[1] == '-')
        {
            buffer[numBytesRead] = 0;
            nNumberProcessors = atoi(buffer + 2) + 1;
        }

        fclose(fp);
    }

    return nNumberProcessors;
}

CPUMetrics::CPUMetrics()
{
    m_nProcessors = getNumberOfCores();
}

unsigned int CPUMetrics::getNumCores()
{
    return m_nProcessors;
}

float CPUMetrics::getCPULoad(unsigned int index)
{
    if (index < CPUMETRICS_MAXCORES)
        return m_CPUUsage[index];
    return 0;
}

void CPUMetrics::updateCPU()
{
    const int iMax = m_nProcessors > CPUMETRICS_MAXCORES ? CPUMETRICS_MAXCORES : m_nProcessors;
    // Check cpu freq and if they are enabled
    for(int i = 0; i < iMax; i++)
    {
        struct stat statBuff;

        char buffer[1024] = {0};
        sprintf(buffer, "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_cur_freq", i);

        if(stat(buffer, &statBuff) == 0 && S_ISREG(statBuff.st_mode))
        {
            FILE * fp = fopen(buffer, "r");

            m_isCoreActive[i] = (fp != 0);
            if(m_isCoreActive[i])
            {
                memset(buffer, 0, 1024);

                size_t numBytesRead = fread(buffer, sizeof(char), 1024, fp);

                if(feof(fp) && numBytesRead >= 1)
                {
                    buffer[numBytesRead] = 0;
                    buffer[numBytesRead-1] = 0;

                    m_isCoreActive[i] = atoi(buffer);

                }
                fclose(fp);
            }
            else
            {
                // Set to zero if the cpu is not found
                m_CPUUsage[i] = 10.0f;
            }
        }
        else
        {
            // Set to zero if the cpu is not found
            m_CPUUsage[i] = 10.0f;
        }
    }

    //Open the file to read the ticks of the processor cores
    //Find the lines with cpu0 a b c in them
    //Parse these into integers and compare to the previous values
    FILE * fp = fopen("/proc/stat", "r");
    if (fp)
    {
        char lineBuffer[1024];

        while(fgets(lineBuffer, 1024, fp) && (lineBuffer[0] == 'c'))
        {
            size_t len = strlen(lineBuffer);
            int cpuIndex = -1;

            if((len > 3 && lineBuffer[0] == 'c') &&
               (lineBuffer[1] == 'p') && (lineBuffer[2] == 'u') && (lineBuffer[3] != ' '))
            {
                cpuIndex = atoi(lineBuffer + 3);
            }

            float totalUsage = 0.0f;

            if((cpuIndex > -1) && (cpuIndex < CPUMETRICS_MAXCORES))
            {
                unsigned int processorTicks[num_lpsvs];
                char buffer[10];
                sscanf(lineBuffer, "%s %u %u %u %u %u %u %u",
                       buffer, &processorTicks[lpsv_user],
                       &processorTicks[lpsv_nice], &processorTicks[lpsv_system],
                       &processorTicks[lpsv_idle], &processorTicks[lpsv_iowait],
                       &processorTicks[lpsv_irq], &processorTicks[lpsv_softirq] );

                float delta[num_lpsvs];

                for(int lpsvindex = 0; lpsvindex < num_lpsvs; ++lpsvindex)
                {
                    delta[lpsvindex] =
                            (float)processorTicks[lpsvindex] - (float)m_procStatTicks[cpuIndex * num_lpsvs + lpsvindex];
                    totalUsage += delta[lpsvindex];

                    // Now store the ticks for later
                    m_procStatTicks[cpuIndex * num_lpsvs + lpsvindex] = processorTicks[lpsvindex];
                }

                m_CPUUsage[cpuIndex] = (delta[lpsv_user] * 100.0f)/totalUsage;
            }
        }

        fclose(fp);
    }
    else{
        m_CPUUsage[0] = 100.0f;
    }
#if DUMP_CPU_DATA
    const int iMax = m_nProcessors > CPUMETRICS_MAXCORES ? CPUMETRICS_MAXCORES : m_nProcessors;
	for(unsigned int i = 0; i < iMax; i++)
	{
		LOGI("CPU Usage core %d: %f", i, m_CPUUsage[i]);
	}
#endif
}
