package com.shoeboxscientist.hellorainbow;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RainbowHatConnector mRainbowHat;
    private NanoHttpWebServer mServer;
    private String mHtmlTemplate;
    private int[] mRainbow = new int[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Started HelloRainbow");
        setContentView(R.layout.activity_main);

        // Set up the rainbow hat
        mRainbowHat = new RainbowHatConnector();
        mRainbowHat.init();
        mRainbowHat.makeNoise();
        mRainbowHat.updateDisplay("HLLO");
        mRainbowHat.updateLedStrip(new int[] { Color.BLACK, Color.BLACK, Color.BLACK,
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK});

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
        if (params == null) {
            return resp;
        }

        resp = resp.replace("$display", convertNullToEmpty(params.get("display")));
        resp = resp.replace("$rainbow", convertNullToZero(params.get("rainbow")));

        if (params.containsKey("display")) {
            mRainbowHat.updateDisplay(params.get("display"));

            if (params.containsKey("rainbow")) {
                mRainbowHat.updateLedStrip(getColours(Integer.parseInt(params.get("rainbow"))));
                // Do It Twice since something w/ Android Things does not work consistently
                mRainbowHat.updateLedStrip(getColours(Integer.parseInt(params.get("rainbow"))));
            }

            if (params.containsKey("redled")) {
                mRainbowHat.setLED(true);
                resp = resp.replace("$red", "checked");
            }
            else {
                mRainbowHat.setLED(false);
                resp = resp.replace("$red", "");
            }

            if (params.containsKey("greenled")) {
                mRainbowHat.setGreenLED(true);
                resp = resp.replace("$green", "checked");
            }
            else {
                mRainbowHat.setGreenLED(false);
                resp = resp.replace("$green", "");
            }

            if (params.containsKey("blueled")) {
                mRainbowHat.setBlueLED(true);
                resp = resp.replace("$blue", "checked");
            }
            else {
                mRainbowHat.setBlueLED(false);
                resp = resp.replace("$blue", "");
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
        mRainbowHat.cleanup();

        if (mServer != null) {
            mServer.stop();
        }
    }
}
