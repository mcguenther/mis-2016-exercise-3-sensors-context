package de.volzo.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SeekBar.OnSeekBarChangeListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            List<Sensor> accelSensors = mSensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
            for (int i = 0; i < accelSensors.size(); i++) {
                // if ((accelSensors.get(i).getVendor().contains("Google Inc.")) &&
                //        (accelSensors.get(i).getVersion() == 3)) {
                mSensor = accelSensors.get(i);
                Log.i(TAG, "Found sensor " + mSensor.getName().toString());
                break;
                //}
            }
        } else {
            Log.i(TAG, "No Accelerometer found, sry.");
        }

        SeekBar sBar = (SeekBar) findViewById(R.id.seekBar);
        sBar.setOnSeekBarChangeListener(this);


    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, 100000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.

        double accX = event.values[0];
        double accY = event.values[0];
        double accZ = event.values[0];

        String strX = Double.valueOf(Math.round(accX * 100) / 100f).toString();
        String strY = Double.valueOf(Math.round(accY * 100) / 100f).toString();
        String strZ = Double.valueOf(Math.round(accZ * 100) / 100f).toString();

        //textView.setText("x: " + strX + "; y: " + strY + "; z:" + strZ);


    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mSensorManager.unregisterListener(this);
        mSensorManager.registerListener(this, mSensor, seekBar.getProgress());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int freq = (progress + 1) * 10;
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Update Acelerometer data every " + freq + "ms.");
    }

}
