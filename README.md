PVRMonitor
==========
![feature_pvrmonitor](https://user-images.githubusercontent.com/9431587/57515160-e4adb500-7309-11e9-835d-f9605abfd459.png)

PVRMonitor allows developers to view real-time hardware performance stats, giving information about processor usage on the CPU and PowerVR graphics hardware (Series5, Series5XT, Series6) with negligible impact on performance. The data can be presented as either a graph or plain text, and is updated in real time and overlaid on top of currently running applications.

PVRMonitor gives immediate feedback on an application’s performance, without having to connect to an external profiling tool such as PVRTune.

PVRMonitor now includes the full range of [PowerVR hardware counters](http://cdn.imgtec.com/sdk-documentation/PVRTune.Counter%20List%20and%20Description.pdf), some of the choices include:

- Frames per second
- CPU: Load
- GPU: Total pixel load
- GPU: Total vertex load
- Clock speed
- Cycles per pixel/vertex

and many more..

PVRScope
==========
PVRMonitor was created as a useful profiling tool, but also as an example of how to easily make use of the PVRScope hardware profiling library.

PVRScope is an unparalleled performance analysis library that your application can link to and retrieve GPU counter data. It exposes a user-friendly API for sending user-defined events and data to any application in its own separate timeline.

- **Direct access to PowerVR hardware counters**
Allows your applications to retrieve PowerVR GPU hardware counters. Have the freedom to manipulate counter data for your own analysis.
- **Customise data**
Create custom counters, annotated markers and editable data to send to an appplication, which are displayed in their own timeline for your convenience.
- **Speed up your workflow**
You can profile different variations of your application without needing to recompile each time.
- **Examples provideds**
Comes with example source code, showing you how to forward the data onto any application, and how to consume the data stream.

PVRScope is used to output data to our core hardware profiling tool, [PVRTune](https://www.imgtec.com/developers/powervr-sdk-tools/pvrtune/), which can be used for far more in-depth profiling and analysis.

Binaries
==========
The PVRMonitor binary can be downloaded from [Google Play](https://play.google.com/store/apps/details?id=com.powervr.PVRMonitor) or through the [PowerVR Graphics SDK installer](http://community.imgtec.com/developers/powervr/installers/).

Support
==========
If you have any questions about PVRMonitor, please contact us through our [public forum](http://forum.imgtec.com/categories/powervr-graphics). We also recommend checking out our [FAQ](http://forum.imgtec.com/categories/powervr-faq) to see if your question has already been answered. If you would prefer to contact us confidentially, you can file a support ticket [here](https://pvrsupport.imgtec.com/new-ticket).

License
==========
PVRMonitor is distributed under the same permissive license as our SDK so it can easily be integrated into commercial and non-commercial applications. You can find the license [here](https://github.com/powervr-graphics/PVRMonitor/blob/19.1/LICENSE.txt). To further clarify the terms, we also have an SDK license FAQ (available [here](http://community.imgtec.com/developers/powervr/faq-about-the-sdk-eula/)).
