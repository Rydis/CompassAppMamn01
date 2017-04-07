package com.example.rydis.compassapp;


import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

// Code taken from the magnetic compass tutorial linked at live@lund
public class compassActivity extends Activity implements SensorEventListener {

    static final float ALPHA = 0.1f; // if ALPHA = 1 OR 0, no filter applies.
    private ImageView mPointer;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;
    private Vibrator vibrator;
    CheckBox checkBoxVibration;
    long lastCheckTime;
    int vibrationTime;
    long currentTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mPointer = (ImageView) findViewById(R.id.pointer);

        checkBoxVibration = (CheckBox) findViewById(R.id.checkBoxVibration);
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        lastCheckTime = 0;
        vibrationTime = 500;
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            // Lowpass filtered singal to reduce noise
            float[] filteredAccelerometerSignal = lowPass(event.values.clone(), mLastAccelerometer);
            System.arraycopy(filteredAccelerometerSignal, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            // Lowpass filtered singal to reduce noise
            float[] filteredMagnetometerSignal = lowPass(event.values.clone(), mLastMagnetometer);
            System.arraycopy(filteredMagnetometerSignal, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;
            RotateAnimation ra = new RotateAnimation(
                    mCurrentDegree,
                    -azimuthInDegress,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);

            ra.setDuration(250);
            ra.setFillAfter(true);

            mPointer.startAnimation(ra);
            mCurrentDegree = -azimuthInDegress;

            // vibrate if the phone points towards north
            if ((azimuthInDegress > 345 || azimuthInDegress < 15) && checkBoxVibration.isChecked()) {
                vibrate();
            }

            TextView axisView = (TextView) findViewById(R.id.axesView);
            axisView.setText("X:" + Float.toString(event.values[0]) + "\n" +
                    "Y:" + Float.toString(event.values[1]) + "\n" +
                    "Z:" + Float.toString(event.values[2])
            );
        }
    }

    public void vibrate() {
        currentTime = System.currentTimeMillis();

        if ((lastCheckTime + vibrationTime) < currentTime) {
            vibrator.vibrate(vibrationTime);
            lastCheckTime = System.currentTimeMillis();
        }


    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    // Code taken from the lowpass filter tutorial, linked at live@lund
    protected float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

}

