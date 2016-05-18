package de.volzo.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.ArrayUtils;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SeekBar.OnSeekBarChangeListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private static final String TAG = "MainActivity";

    private static FFT FFFobject = new FFT(32);

    private static final int QUEUE_SIZE = 512;
    public CircularFifoQueue<Double> x = new CircularFifoQueue<Double>(QUEUE_SIZE);
    public CircularFifoQueue<Double> y = new CircularFifoQueue<Double>(QUEUE_SIZE);
    public CircularFifoQueue<Double> z = new CircularFifoQueue<Double>(QUEUE_SIZE);
    public CircularFifoQueue<Double> m = new CircularFifoQueue<Double>(QUEUE_SIZE);

    public Double[] ax = new Double[QUEUE_SIZE];
    public Double[] ay = new Double[QUEUE_SIZE];
    public Double[] az = new Double[QUEUE_SIZE];
    public Double[] am = new Double[QUEUE_SIZE];

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
        x.add((double) event.values[0]);
        y.add((double) event.values[1]);
        z.add((double) event.values[2]);
        m.add(Math.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2)));

        x.toArray(ax);
        y.toArray(ay);
        z.toArray(az);
        m.toArray(am);

        View view = (AccelView) findViewById(R.id.view);
        view.invalidate();

        double mCalc = 7;
        int nCalc = (int) Math.round(Math.pow(2, mCalc));
        int noElements = x.size();
        FFT myFFT = new FFT(nCalc);
        if (noElements >= nCalc) {
            Double[] xArrayRealObjects = new Double[noElements];
            Double[] yArrayRealObjects = new Double[noElements];
            Double[] zArrayRealObjects = new Double[noElements];
            Double[] mArrayRealObjects = new Double[noElements];
            double[] xArrayImag = new double[noElements];
            double[] yArrayImag = new double[noElements];
            double[] zArrayImag = new double[noElements];
            double[] mArrayImag = new double[noElements];

            double[] xArrayReal = ArrayUtils.toPrimitive(x.toArray(xArrayRealObjects));
            double[] yArrayReal = ArrayUtils.toPrimitive(y.toArray(yArrayRealObjects));
            double[] zArrayReal = ArrayUtils.toPrimitive(z.toArray(zArrayRealObjects));
            double[] mArrayReal = ArrayUtils.toPrimitive(m.toArray(mArrayRealObjects));

            //ArrayUtils.toPrimitive(xArrayImag);

            Log.d(TAG, "Calculating FFT with - REAL: " + xArrayReal[0] + " IMAG: " + xArrayImag[0]);
            myFFT.fft(xArrayReal, xArrayImag);
            myFFT.fft(yArrayReal, yArrayImag);
            myFFT.fft(zArrayReal, zArrayImag);
            myFFT.fft(mArrayReal, mArrayImag);

            Log.d(TAG, "Got FFT - REAL: " + xArrayReal[0] + " IMAG: " + xArrayImag[0]);
        }
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
        textView.setText("Update Accelerometer data every " + freq + "ms.");
    }

}
