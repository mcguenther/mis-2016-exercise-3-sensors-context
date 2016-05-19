package de.volzo.sensors;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
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

    private float speed = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateFFTSize(QUEUE_SIZE);
        updateFrequency(500);

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

        createNotification("foo");
    }

    private void createNotification(String msg) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle(msg)
                                .setContentText("");

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(this);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(42, mBuilder.build());
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
            Double[] mArrayRealObjects = new Double[noElements];
            double[] mArrayImag = new double[noElements];
            double[] mArrayReal = ArrayUtils.toPrimitive(m.toArray(mArrayRealObjects));
            FFFobject.fft(mArrayReal, mArrayImag);
            Double[] mFFTMagnitude = new Double[noElements];

            for (int i = 0; i < noElements; ++i) {
                mFFTMagnitude[i] = Math.sqrt(Math.pow(mArrayReal[i], 2) + Math.pow(mArrayImag[i], 2));
            }
            FFTView myFFTView = (FFTView) findViewById(R.id.FFTview);
            myFFTView.setMagnitudes(mFFTMagnitude);
            myFFTView.invalidate();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar.getId() == R.id.seekBar) {
            mSensorManager.unregisterListener(this);
            int freq = (seekBar.getProgress() + 1) * 10;
            mSensorManager.registerListener(this, mSensor, freq);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.seekBar) {
            int freq = (progress + 1) * 10;
            updateFrequency(freq);
        }
        if (seekBar.getId() == R.id.sbFFTSize) {
            int size = (int) Math.round(Math.pow(2, progress + 1));
            updateFFTSize(size);
        }
    }

    private void updateFrequency(int freq) {
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Update Accelerometer data every " + freq + "ms.");
        
        this.frequency = freq;
        
        SeekBar sbF = (SeekBar) findViewById(R.id.seekBar);
        sbF.setProgress(Math.round((freq / 10) - 1));
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
