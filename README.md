# HelloRainbow
Simple AndroidThings / RainbowHat web UI

A really simple example of using the RainbowHat with Android Things for use with the starter kit:
https://shop.pimoroni.com/products/rainbow-hat-for-android-things

Instructions below assume you have set up your Raspberry Pi with Android Things and rainbow hat as described in 
https://developer.android.com/things/index.html , have set up your SDK, your device booted and connected to the same 
network, etc, etc... 

<h1>Quick Start</h2>
<pre>
git clone https://github.com/adamconnors/HelloRainbow.git
cd HelloRainbow/
export ANDROID_HOME='[path-to-your-android-sdk]'
./gradlew build
adb connect Android.local
adb install app/build/outputs/apk/app-debug.apk
adb shell am start -n com.shoeboxscientist.hellorainbow/com.shoeboxscientist.hellorainbow.MainActivity
</pre>

(If adb connect Android.local doesn't work try adb connect [raspberry-pi-ip-address]:5555) instead.

Assuming it worked, the rainbow hat will make a pleasant beep and the display will read: HLLO

Now point your Web browser to: <pre>http://[your-raspberry-ip-address]:8080</pre> for a handy-dandy web interface for controlling your Rainbow Hat.
Useless, but oh, so satisfying.
