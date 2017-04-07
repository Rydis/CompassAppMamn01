# CompassApp

This application consist of three Java classes (`AccelerometerActivity`,`CompassActivity` and `MainActivity`), which all extends the Android `Activity` class. The source code can be found at: [/CompassApp/app/src/main/java/com/example/rydis/compassapp/](/CompassApp/app/src/main/java/com/example/rydis/compassapp/).


## MainActivity

The MainActivity is just a simple class containing two methods for starting new activities. It works by defining `Intent` objects, which is used as input to the method `startActivity()`. The class looks like the following: 
 
```Java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** Called when the user taps the accelerometer button */
    public void accelerometerView(View view) {
        Intent intent = new Intent(this, AccelerometerActivity.class);
        startActivity(intent);
    }
    /** Called when the user taps the compass button */
    public void compassView(View view){
        Intent intent = new Intent(this, compassActivity.class);
        startActivity(intent);
    }
}
```
## CompassActivity

I created the `CompassActivity` class by following the provided tutorial at [live@lund](http://www.techrepublic.com/article/pro-tip-create-your-own-magnetic-compass-using-androids-internal-sensors/), I also implemented the low pass filter found at [live@lund](https://www.built.io/blog/applying-low-pass-filter-to-android-sensor-s-readings) to reduce the noise related to the sensor measuring. I found that `ALPHA = 0.1f;` were the optimal settings for my test enviroment (OnePlus ONE A2003(Android 6.0.1, API 23)), which resulted in least distortion. 

I also implemented a feature that vibrates when the phone is pointed towards north. I noticed that the GUI would freeze if the `Vibrator.vibrate()` were called to often, which is the case because the method call is placed inside the `onSensorChanged(SensorEvent event)` method. Therefore i implemented the `vibrate()` method which only calls the `Vibrator.vibrate()` when the phone isn't currently vibrating. The class looks like the following:

```Java
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

    // Code taken from the low pass filter tutorial, linked at live@lund
    protected float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

}
```
## AccelerometerActivity
The AccelerometerActivity is done completely by following [this](https://examples.javacodegeeks.com/android/core/hardware/sensor/android-accelerometer-example/) tutorial. The code looks like:

```Java
package com.example.rydis.compassapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.widget.TextView;

public class AccelerometerActivity extends AppCompatActivity implements SensorEventListener {

    private float lastX, lastY, lastZ;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float deltaXMax = 0;
    private float deltaYMax = 0;
    private float deltaZMax = 0;
    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;
    private float vibrateThreshold = 0;
    private TextView currentX, currentY, currentZ, maxX, maxY, maxZ;
    public Vibrator v;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer);
        initializeViews();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            vibrateThreshold = accelerometer.getMaximumRange() / 2;
        } else {
            // fai! we dont have an accelerometer!
        }
        //initialize vibration
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
    }
    public void initializeViews() {

        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);

        maxX = (TextView) findViewById(R.id.maxX);
        maxY = (TextView) findViewById(R.id.maxY);
        maxZ = (TextView) findViewById(R.id.maxZ);
    }
    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // clean current values
        displayCleanValues();
        // display the current x,y,z accelerometer values
        displayCurrentValues();
        // display the max x,y,z accelerometer values
        displayMaxValues();

        // get the change of the x,y,z values of the accelerometer
        deltaX = Math.abs(lastX - event.values[0]);
        deltaY = Math.abs(lastY - event.values[1]);
        deltaZ = Math.abs(lastZ - event.values[2]);

        // if the change is below 2, it is just plain noise
        if (deltaX < 2)
            deltaX = 0;
        if (deltaY < 2)
            deltaY = 0;
        if ((deltaZ > vibrateThreshold) || (deltaY > vibrateThreshold) || (deltaZ > vibrateThreshold)) {
            v.vibrate(50);
        }
    }

    public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        currentX.setText(Float.toString(deltaX));
        currentY.setText(Float.toString(deltaY));
        currentZ.setText(Float.toString(deltaZ));
    }

    // display the max x,y,z accelerometer values
    public void displayMaxValues() {
        if (deltaX > deltaXMax) {
            deltaXMax = deltaX;
            maxX.setText(Float.toString(deltaXMax));
        }
        if (deltaY > deltaYMax) {
            deltaYMax = deltaY;
            maxY.setText(Float.toString(deltaYMax));
        }
        if (deltaZ > deltaZMax) {
            deltaZMax = deltaZ;
            maxZ.setText(Float.toString(deltaZMax));
        }
    }

}

```
