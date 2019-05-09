/*!****************************************************************************

 @file         CPUMetrics.h
 @copyright    Copyright (c) Imagination Technologies Limited.
 @brief        Collects CPU metrics.

******************************************************************************/
#ifndef _CPUMETRICS_H_
#define _CPUMETRICS_H_

#if defined(__cplusplus)
extern "C"
#endif
{

#define CPUMETRICS_MAXCORES	(20)

enum LinuxProcStatVariables
{
    lpsv_cpuid, lpsv_user, lpsv_nice, lpsv_system, lpsv_idle,
    lpsv_iowait, lpsv_irq, lpsv_softirq, num_lpsvs
};

class CPUMetrics
{
public:
    CPUMetrics();
    void updateCPU();
    unsigned int getNumCores();
    float getCPULoad(unsigned int index);

private:
    unsigned int	m_nProcessors;
    bool			m_isCoreActive[CPUMETRICS_MAXCORES];
    float			m_CPUUsage[CPUMETRICS_MAXCORES];
    unsigned int 	m_procStatTicks[CPUMETRICS_MAXCORES * num_lpsvs];
};

}

#endif
