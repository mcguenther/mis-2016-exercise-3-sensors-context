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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SeekBar.OnSeekBarChangeListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private static final String TAG = "MainActivity";

    private static FFT FFFobject = null;

    private static final int QUEUE_SIZE = 512;
    public CircularFifoQueue<Double> x = new CircularFifoQueue<Double>(QUEUE_SIZE);
    public CircularFifoQueue<Double> y = new CircularFifoQueue<Double>(QUEUE_SIZE);
    public CircularFifoQueue<Double> z = new CircularFifoQueue<Double>(QUEUE_SIZE);
    public CircularFifoQueue<Double> m = new CircularFifoQueue<Double>(QUEUE_SIZE);

    public Double[] ax = new Double[QUEUE_SIZE];
    public Double[] ay = new Double[QUEUE_SIZE];
    public Double[] az = new Double[QUEUE_SIZE];
    public Double[] am = new Double[QUEUE_SIZE];
    private int frequency;
    private int fftSize;
    private double maxMagn = 0;
    private int maxFreqI = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateFFTSize(QUEUE_SIZE);
        updateFrequency(500 * 10);

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

        SeekBar sbFFTSize = (SeekBar) findViewById(R.id.sbFFTSize);
        sbFFTSize.setOnSeekBarChangeListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, this.frequency);
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

        AccelView view = (AccelView) findViewById(R.id.view);
        view.invalidate();

        /*double mCalc = 7;
        int nCalc = (int) Math.round(Math.pow(2, mCalc));*/
        int noElements = x.size();

        if (noElements >= this.fftSize) {
            Double[] mArrayRealObjects = new Double[this.fftSize];
            double[] mArrayImag = new double[this.fftSize];
            double[] mArrayReal = new double[this.fftSize]; //ArrayUtils.toPrimitive(m.toArray(mArrayRealObjects));

            Iterator<Double> it = m.iterator();

            for(int n = 0; n < this.fftSize; ++n) {
                mArrayReal[n] = (double) it.next();
            }

            FFFobject.fft(mArrayReal, mArrayImag);

            // only use first half of data owing to symmetry of fft data
            // in addition drop 0-element which is 0 Hz and could be interpreted as offset

            Double[] mFFTMagnitude = new Double[Math.round(this.fftSize/2)-1];
            for (int i = 1; i < Math.round(this.fftSize/2); ++i) {
                double curMagn = Math.sqrt(Math.pow(mArrayReal[i], 2) + Math.pow(mArrayImag[i], 2));
                if (curMagn > maxMagn) {
                    maxMagn = curMagn;
                    maxFreqI = i;
                }
                mFFTMagnitude[i-1] = curMagn;
            }



            FFTView myFFTView = (FFTView) findViewById(R.id.FFTview);
            myFFTView.setMagnitudes(mFFTMagnitude);
            Log.d(TAG, "Maximum FFT value: " + maxMagn + " at i = " + maxFreqI);
            myFFTView.invalidate();


        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar.getId() == R.id.seekBar) {
            mSensorManager.unregisterListener(this);
            int freq = (seekBar.getProgress() + 1) * 100;
            mSensorManager.registerListener(this, mSensor, freq);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.seekBar) {
            int freq = (progress + 1) * 100;
            updateFrequency(freq);
        }
        if (seekBar.getId() == R.id.sbFFTSize) {
            int size = (int) Math.round(Math.pow(2, progress + 1));
            updateFFTSize(size);
        }
    }

    private void updateFrequency(int freq) {
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Update Accelerometer data every " + freq / 100 + "ms.");
        
        this.frequency = freq;
        
        SeekBar sbF = (SeekBar) findViewById(R.id.seekBar);
        sbF.setProgress(Math.round((freq / 100) - 1));
    }

    private void updateFFTSize(int size) {
        TextView textView = (TextView) findViewById(R.id.tvFFTSize);
        textView.setText("Use FFT size of " + size + ".");

        this.fftSize = size;

        SeekBar sbFFT = (SeekBar) findViewById(R.id.sbFFTSize);
        int seekbarProgress = (int) Math.round(Math.log(size) / Math.log(2)) - 1;
        sbFFT.setProgress(seekbarProgress);
        FFFobject = new FFT(size);
    }
}
