package com.shoeboxscientist.hellorainbow;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.animation.AnimatorListenerAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

// import the RainbowHat driver
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;

// import the devices on the RainbowHat
import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.pio.Gpio;

// For the Speaker
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private NanoHttpWebServer mServer;
    private String mHtmlTemplate;
    private int[] mRainbow = new int[7];
    private static final int BRIGHTNESS = 1;
    private int SPEAKER_READY_DELAY_MS = 300;
    private Speaker mSpeaker;
    private ValueAnimator mSpeakerAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Started HelloRainbow");
        setContentView(R.layout.activity_main);

        // Set up the rainbow hat
        // Setup the alphanumeric display
        try {
            AlphanumericDisplay segment = RainbowHat.openDisplay();
            segment.setBrightness(BRIGHTNESS);
            segment.display("HLLO");
            segment.setEnabled(true);
            // Close the device when done.
            segment.close();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing alphanumeric display", e);
        }

        // Setup the rainbow
        try {
            Apa102 ledstrip = RainbowHat.openLedStrip();
            ledstrip.setBrightness(BRIGHTNESS);
            ledstrip.write(new int[] { Color.BLACK, Color.BLACK, Color.BLACK,
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK});
            // Close the device when done.
            ledstrip.close();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing apa102 rainbow", e);
        }

        // Setup the Red LED
        try {
            Gpio Led = RainbowHat.openLedRed();
            Led.setValue(false);
            // Close the device when done.
            Led.close();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing Red LED", e);
        }

        // Setup the Red LED
        try {
            Gpio Led = RainbowHat.openLedGreen();
            Led.setValue(false);
            // Close the device when done.
            Led.close();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing Green LED", e);
        }

        // Setup the Blue LED
        try {
            Gpio Led = RainbowHat.openLedBlue();
            Led.setValue(false);
            // Close the device when done.
            Led.close();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing Blue LED", e);
        }

        // Animator to make the speaker sound pretty.
        mSpeakerAnimator = ValueAnimator.ofFloat(440, 440 * 4);
        mSpeakerAnimator.setDuration(50);
        mSpeakerAnimator.setRepeatCount(5);
        mSpeakerAnimator.setInterpolator(new LinearInterpolator());
        mSpeakerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                try {
                    float v = (float) animation.getAnimatedValue();
                    // Open the device
                    mSpeaker = RainbowHat.openPiezo();
                    // Make noise
                    mSpeaker.play(v);
                    // Close the device when done.
                    mSpeaker.close();
                } catch (IOException e) {
                    throw new RuntimeException("Error sliding speaker", e);
                }
            }
        });

        mSpeakerAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    // Open the device
                    mSpeaker = RainbowHat.openPiezo();
                    // Stop the buzzer.
                    mSpeaker.stop();
                    // Close the device when done.
                    mSpeaker.close();
                } catch (IOException e) {
                    throw new RuntimeException("Error sliding speaker", e);
                }
            }
        });

        // Play a note on the buzzer.
        mSpeakerAnimator.start();

        // Set up the Http server
        mHtmlTemplate = readHTMLFileTemplate();
        mServer = new NanoHttpWebServer(this);
        try {
            mServer.start();
        } catch(IOException e) {
            Log.e(TAG, "Couldn't start Http Server", e);
        }
        Log.d(TAG, "Http Server started and listening on port: " + mServer.getListeningPort());

        // Set up the rainbow colours
        for (int i = 0; i < mRainbow.length; i++) {
            float[] hsv = {i * 360.f / mRainbow.length, 1.0f, 1.0f};
            mRainbow[i] = Color.HSVToColor(255, hsv);
        }
    }

    private String readHTMLFileTemplate() {
        InputStream stream = null;
        try {

            stream = getAssets().open("www/index.html");
            InputStreamReader r = new InputStreamReader(stream);
            BufferedReader br = new BufferedReader(r);

            String line;
            StringBuilder resp = new StringBuilder();
            while ( (line = br.readLine()) != null)
                resp.append(line);

            return resp.toString();
        } catch (IOException e) {
            Log.e(TAG, "Couldn't read asset.", e);
            return null;
        } finally {
            if (stream != null) {
                try { stream.close(); } catch (IOException e) { /**/ }
            }
        }
    }

    private String convertNullToEmpty(String val) {
        if (val == null) {
            return "";
        } else {
            return val;
        }
    }

    private String convertNullToZero(String val) {
        if (val == null) {
            return "0";
        } else {
            return val;
        }
    }

    /**
     * Called by NanoHttpWebServer when there is a request, the params are used to update the state
     * of the rainbow hat. The html template is also updated and returned to keep the values if
     * the form actually gets submitted (e.g. if the async request doesn't work).
     */
    public String onHttpRequest(Map<String, String> params) {
        String resp = mHtmlTemplate;
        boolean ledstate;

        if (params == null) {
            return resp;
        }

        resp = resp.replace("$display", convertNullToEmpty(params.get("display")));
        resp = resp.replace("$rainbow", convertNullToZero(params.get("rainbow")));

        if (params.containsKey("display")) {
            // Display a string on the segment display.
            try {
                AlphanumericDisplay segment = RainbowHat.openDisplay();
                segment.setBrightness(BRIGHTNESS);
                segment.display(params.get("display"));
                // Close the device when done.
                segment.close();
            } catch (IOException e) {
                throw new RuntimeException("Error initializing alphanumeric display", e);
            }

            if (params.containsKey("rainbow")) {
                // Light up the rainbow.
                try {
                    Apa102 ledstrip = RainbowHat.openLedStrip();
                    ledstrip.setBrightness(BRIGHTNESS);
                    ledstrip.write(getColours(Integer.parseInt(params.get("rainbow"))));
                    // Do It Twice since something w/ Android Things does not work consistently.
                    ledstrip.write(getColours(Integer.parseInt(params.get("rainbow"))));
                    // Close the device when done.
                    ledstrip.close();
                } catch (IOException e) {
                    throw new RuntimeException("Error initializing apa102 rainbow", e);
                }
            }

            // Get the Red LED state.
            if (params.containsKey("redled")) {
                resp = resp.replace("$red", "checked");
                ledstate = true;
            }
            else {
                resp = resp.replace("$red", "");
                ledstate = false;
            }

            // Set the Red LED.
            try {
                Gpio Led = RainbowHat.openLedRed();
                Led.setValue(ledstate);
                // Close the device when done.
                Led.close();
            } catch (IOException e) {
                throw new RuntimeException("Error initializing Red LED", e);
            }

            // Get the Green LED state.
            if (params.containsKey("greenled")) {
                resp = resp.replace("$green", "checked");
                ledstate = true;
            }
            else {
                resp = resp.replace("$green", "");
                ledstate = false;
            }

            // Set the Green LED.
            try {
                Gpio Led = RainbowHat.openLedGreen();
                Led.setValue(ledstate);
                // Close the device when done.
                Led.close();
            } catch (IOException e) {
                throw new RuntimeException("Error initializing Green LED", e);
            }

            // Get the Blue LED state.
            if (params.containsKey("blueled")) {
                resp = resp.replace("$blue", "checked");
                ledstate = true;
            }
            else {
                resp = resp.replace("$blue", "");
                ledstate = false;
            }

            // Setup the Blue LED.
            try {
                Gpio Led = RainbowHat.openLedBlue();
                Led.setValue(ledstate);
                // Close the device when done.
                Led.close();
            } catch (IOException e) {
                throw new RuntimeException("Error initializing Blue LED", e);
            }
        }

        return resp;
    }

    // Converts the val into a position on the rainbow. The colors array is just to build up the
    // list of pretty colors.
    private int[] getColours(int n) {
        n = Math.max(0, Math.min(n, mRainbow.length));
        int[] colors = new int[mRainbow.length];
        for (int i = 0; i < n; i++) {
            int ri = mRainbow.length - 1 - i;
            colors[ri] = mRainbow[ri];
        }

        return colors;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServer != null) {
            mServer.stop();
        }
    }
}
