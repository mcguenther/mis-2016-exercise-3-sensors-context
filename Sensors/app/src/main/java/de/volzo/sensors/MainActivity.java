package de.volzo.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
    private static final double FREQ_MS_PER_STEP = 1;
    private static final int FREQ_OFFSET = 1;

    private FFT FFFobject = null;

    private static final int QUEUE_SIZE = 512;
    public CircularFifoQueue<Double> x = new CircularFifoQueue<Double>(QUEUE_SIZE);
    public CircularFifoQueue<Double> y = new CircularFifoQueue<Double>(QUEUE_SIZE);
    public CircularFifoQueue<Double> z = new CircularFifoQueue<Double>(QUEUE_SIZE);
    public CircularFifoQueue<Double> m = new CircularFifoQueue<Double>(QUEUE_SIZE);

    public Double[] ax = new Double[QUEUE_SIZE];
    public Double[] ay = new Double[QUEUE_SIZE];
    public Double[] az = new Double[QUEUE_SIZE];
    public Double[] am = new Double[QUEUE_SIZE];
    private double frequencyInSeconds;

    private int fftSize;


    private float speed = 0f;
    private double frequency = 0d;
    private double magnitude = 0d;
    private static List<Double> thresholdedFrequencies = new ArrayList<>();

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
            Log.i(TAG, "No Accelerometer found.");
        }

        // GPS / Speed

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 23);
        }

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {

            public void onLocationChanged(Location location) {
                location.getLatitude();
                speed = location.getSpeed();
                ((TextView) findViewById(R.id.textView4)).setText("speed: " + speed);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i(TAG, "location status changed: " + status);
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, locationListener);
        } catch (SecurityException se) {
            Log.e(TAG, se.toString());
        }


        // Seek bars

        SeekBar sBar = (SeekBar) findViewById(R.id.seekBar);
        sBar.setOnSeekBarChangeListener(this);

        SeekBar sbFFTSize = (SeekBar) findViewById(R.id.sbFFTSize);
        sbFFTSize.setOnSeekBarChangeListener(this);

        updateFFTSize(QUEUE_SIZE);
        updateFrequency(0.01);

    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, (int) Math.round(this.frequencyInSeconds * 1000 * 1000));
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

            for (int n = 0; n < this.fftSize; ++n) {
                mArrayReal[n] = (double) it.next();
            }

            FFFobject.fft(mArrayReal, mArrayImag);

            // only use first half of data owing to symmetry of fft data
            // in addition drop 0-element which is 0 Hz and could be interpreted as offset
            double maxMagn = 0;
            int maxFreqI = 0;
            Double[] mFFTMagnitude = new Double[(int) (Math.round(this.fftSize / 2.0) - 1)];
            for (int i = 1; i < Math.round(this.fftSize / 2); ++i) {
                double curMagn = Math.sqrt(Math.pow(mArrayReal[i], 2) + Math.pow(mArrayImag[i], 2));
                if (curMagn > maxMagn) {
                    maxMagn = curMagn;
                    maxFreqI = i;
                }

                mFFTMagnitude[i - 1] = curMagn;
            }


            FFTView myFFTView = (FFTView) findViewById(R.id.FFTview);
            myFFTView.setMagnitudes(mFFTMagnitude);

            double fInHz = 1 / this.frequencyInSeconds;
            double maxFreq = maxFreqI * (fInHz) / this.fftSize;
            //TextView tvSize = (TextView) findViewById(R.id.tvFFTSize);
            //tvSize.setText("Found strongest frequency: " + maxFreq);
            myFFTView.invalidate();

            this.frequency = maxFreq;
            this.magnitude = maxMagn;
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar.getId() == R.id.seekBar) {
            double freq = getFrequencyFromProgress(seekBar.getProgress());
            updateFrequency(freq);
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.seekBar) {
            double freq = getFrequencyFromProgress(progress);
            updateFrequencyUIOnly(freq);
        }
        if (seekBar.getId() == R.id.sbFFTSize) {
            int size = (int) Math.round(Math.pow(2, progress + 1));
            updateFFTSize(size);
        }
    }

    private void updateFrequency(double freq) {
        updateFrequencyUIOnly(freq);
        mSensorManager.unregisterListener(this);
        mSensorManager.registerListener(this, mSensor, (int) Math.round(freq * 1000 * 1000));
    }

    private int getProgressFromFrequency(double freq) {
        return (int) Math.round((freq * 1000 / FREQ_MS_PER_STEP) - FREQ_OFFSET);
    }


    private double getFrequencyFromProgress(int progress) {
        return (progress + FREQ_OFFSET) * FREQ_MS_PER_STEP / 1000d;
    }

    private void updateFrequencyUIOnly(double freq) {
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Update Accelerometer data every " + Math.round(freq * 1000) + "ms.");

        this.frequencyInSeconds = freq;

        SeekBar sbF = (SeekBar) findViewById(R.id.seekBar);
        sbF.setProgress(getProgressFromFrequency(freq));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
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
