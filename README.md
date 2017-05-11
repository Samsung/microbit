micro:bit Android application
=============================

**Build instructions**

* Install needed tools to build the project:
    
    * [Android SDK](http://developer.android.com/sdk/index.html)
    
    * [Gradle](https://gradle.org/gradle-download/) (Minimum version [2.14.1+](https://developer.android.com/studio/releases/gradle-plugin.html#updating-gradle))

* Go to root directory and run `gradle build`. After build is finished, apk file can be found under `~/app/build/outputs/apk/app-debug.apk`

* Or run `gradle installDebug` to build and install app on plugged android device


## Libraries

 * [Android-DFU-Library](https://github.com/NordicSemiconductor/Android-DFU-Library)
 * [android-gif-drawable](https://github.com/koral--/android-gif-drawable)
