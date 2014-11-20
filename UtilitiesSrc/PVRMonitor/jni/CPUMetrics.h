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
	bool			m_isCoreActive[8];
	float			m_CPUUsage[8];
	unsigned int 	procStatTicks[8 * num_lpsvs];
};

}

#endif
