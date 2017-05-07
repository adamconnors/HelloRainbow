package com.shoeboxscientist.hellorainbow;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class RainbowHatConnector {

    private static final String TAG = "RainbowHat";

    private AlphanumericDisplay mDisplay;
    private Apa102 mLedstrip;
    private Gpio mLed;

    private Speaker mSpeaker;
    private ValueAnimator mSpeakerAnimator;

    private static final int BRIGHTNESS = 1;
    private int SPEAKER_READY_DELAY_MS = 300;

    public void init() {
        try {
            mDisplay = new AlphanumericDisplay(BoardDefaults.getI2cBus());
            mDisplay.setEnabled(true);
            mDisplay.setBrightness(BRIGHTNESS);
            mDisplay.clear();
            Log.d(TAG, "Initialized I2C Display");
        } catch (IOException e) {
            Log.e(TAG, "Error initializing display", e);
            Log.d(TAG, "Display disabled");
            mDisplay = null;
        }

        // SPI ledstrip
        try {
            mLedstrip = new Apa102(BoardDefaults.getSpiBus(), Apa102.Mode.BGR);
            mLedstrip.setBrightness(BRIGHTNESS);
        } catch (IOException e) {
            mLedstrip = null; // Led strip is optional.
        }

        // GPIO led
        try {
            PeripheralManagerService pioService = new PeripheralManagerService();
            mLed = pioService.openGpio(BoardDefaults.getLedGpioPin());
            mLed.setEdgeTriggerType(Gpio.EDGE_NONE);
            mLed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLed.setActiveType(Gpio.ACTIVE_HIGH);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing led", e);
        }

        // PWM speaker
        try {
            mSpeaker = new Speaker(BoardDefaults.getSpeakerPwmPin());
        } catch (IOException e) {
            throw new RuntimeException("Error initializing speaker", e);
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
                        mSpeaker.play(v);
                    } catch (IOException e) {
                        throw new RuntimeException("Error sliding speaker", e);
                    }
                }
            });

        mSpeakerAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    try {
                        mSpeaker.stop();
                    } catch (IOException e) {
                        throw new RuntimeException("Error sliding speaker", e);
                    }
                }
            });
    }

    public void cleanup() {
        if (mDisplay != null) {
            try {
                Log.d(TAG, "Closing display peripheral.");
                mDisplay.clear();
                mDisplay.setEnabled(false);
                mDisplay.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling display", e);
            } finally {
                mDisplay = null;
            }
        }

        if (mLedstrip != null) {
            try {
                Log.d(TAG, "Closing led strip peripheral.");
                mLedstrip.write(new int[7]);
                mLedstrip.setBrightness(0);
                mLedstrip.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling ledstrip", e);
            } finally {
                mLedstrip = null;
            }
        }

        if (mLed != null) {
            try {
                Log.d(TAG, "Closing GPIO peripheral.");
                mLed.setValue(false);
                mLed.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling led", e);
            } finally {
                mLed = null;
            }
        }
    }

    public void makeNoise() {
        mSpeakerAnimator.start();
    }

    public void updateDisplay(String value) {
        if (mDisplay != null) {
            try {
                mDisplay.display(value);
            } catch (IOException e) {
                Log.e(TAG, "Error setting display", e);
            }
        }
    }

    public void updateLedStrip(int[] colours) {
        // Update led strip.
        if (mLedstrip == null) {
            return;
        }

        try {
            mLedstrip.write(colours);
        } catch (IOException e) {
            Log.e(TAG, "Error setting ledstrip", e);
        }
    }

    public void setLED(boolean on) {
        try {
            mLed.setValue(on);
        } catch (IOException e) {
            Log.e(TAG, "Error setting ledstrip", e);
        }
    }
}
