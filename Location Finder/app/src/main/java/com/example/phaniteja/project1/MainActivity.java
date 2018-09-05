package com.example.phaniteja.project1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import java.util.Timer;

public class MainActivity extends AppCompatActivity implements LocationListener,SensorEventListener {

    public static final String Logger = " ";
    private LocationManager locationManager;
    private EditText longitude1Text;
    private EditText latitude1Text;
    private EditText longitude2Text;
    private EditText latitude2Text;
    private EditText providerText;
    private EditText distanceText;
    private Handler method1Handler = null;
    private Handler accHandler=new Handler();
    private boolean networkLocation = false;
    private boolean gpsLocation = false;
    private boolean networkEnabled = false;
    private boolean gpsEnabled = false;
    private Location initLocation = null;
    private Timer method1Timer = null;
    private SensorManager sensorManagerAcc,sensorManagerMagnetic;
    private Sensor accelerometer,magnetometer;
    private float[] gravity = {0,0,0};
    private double timeStart=0;
    private double timeEnd=0;
    private boolean accListener= false,magListener= false;
    private double i =1;
    private int mAzimuth;
    private float[] rotationMatrix = new float[9];
    private float[] angle = new float[3];
    private float[] prevAccelorometerValues = new float[3];
    private float[] prevMagnetometerValues = new float[3];
    private boolean accelerometerBool = false;
    private boolean magnetometerBool = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        latitude1Text = (EditText) findViewById(R.id.Latitude1);
        longitude1Text = (EditText) findViewById(R.id.Longitude1);

        latitude2Text = (EditText) findViewById(R.id.Latitude2);
        longitude2Text = (EditText) findViewById(R.id.Longitude2);

        providerText = (EditText) findViewById(R.id.provider);
        distanceText = (EditText) findViewById(R.id.Distance);

        method1Handler = new Handler();


        sensorManagerAcc = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManagerAcc.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accListener = sensorManagerAcc.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManagerMagnetic = (SensorManager) getSystemService(SENSOR_SERVICE);
        magnetometer = sensorManagerMagnetic.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        magListener = sensorManagerAcc.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gpsLocation = false;
        networkLocation = false;
        startProvider();
    }

    private void startProvider(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            Log.d(Logger,"GPS permission needs to be granted");
            return;
        }
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if(gpsEnabled){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,gpsListener);
        }

        if(networkEnabled){
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,networkListener);
        }

    }
    LocationListener gpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude1 = location.getLatitude();
            double longitude1 = location.getLongitude();
            double acc = location.getAccuracy();
            gpsLocation = true;

            if(chooseProvider(location)){
                method1Handler.post(new LocationWork(latitude1,longitude1,"OUTDOOR"));
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    LocationListener networkListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude1 = location.getLatitude();
            double longitude1 = location.getLongitude();
            double acc = location.getAccuracy();
            networkLocation = true;
            if(chooseProvider(location)){
                method1Handler.post(new LocationWork(latitude1,longitude1,"INDOOR"));
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private class LocationWork implements Runnable{
        private double latitude1, longitude1;
        private String provider1;
        public LocationWork(double latitude_, double longitude_,String provider_){
            latitude1 = latitude_;
            longitude1 = longitude_;
            provider1= provider_;
        }
        public void run(){
            latitude1Text.setText(new Double(latitude1).toString());
            latitude1Text.setActivated(true);
            longitude1Text.setText(new Double(longitude1).toString());
            providerText.setText((new String(provider1)));
        }
    }

    private synchronized boolean chooseProvider(Location loc) {

        if(initLocation== null && loc!= null){
            initLocation = loc;
            method2(initLocation.getLatitude(),initLocation.getLongitude());
            return true;
        }
        if(initLocation!= null && loc!= null) {
            if (initLocation.getProvider().contentEquals(loc.getProvider())) {
                initLocation = loc;
                return true;
            }else if(initLocation.getAccuracy() >= loc.getAccuracy()){
                initLocation = loc;
                return true;
            }

        }else{
            return false;
        }
        return false;
    }

    private void method2(double latitude2,double longitude2){
        latitude2Text.setText(new Double(latitude2).toString());
        longitude2Text.setText(new Double(longitude2).toString());
        latitude2Text.setActivated(true);
        longitude2Text.setActivated(true);

        double lat2,long2;
        lat2 = latitude2;
        long2 = longitude2;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double dist=0;
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, prevAccelorometerValues, 0, sensorEvent.values.length);
            accelerometerBool = true;
            timeStart = (double) System.currentTimeMillis() / 500;
            if (i != 1) {
                float x_ = sensorEvent.values[0];
                float y_ = sensorEvent.values[1];
                float z_ = sensorEvent.values[2];
                float alpha = (float) 0.5;

                // Isolate the force of gravity with the low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * x_;
                gravity[1] = alpha * gravity[1] + (1 - alpha) * y_;
                gravity[2] = alpha * gravity[2] + (1 - alpha) * z_;

                // Remove the gravity contribution with the high-pass filter.
                x_ = x_ - gravity[0];
                y_ = y_ - gravity[1];
                z_ = z_ - gravity[2];

                double dt = timeStart - timeEnd;
                double vx = x_ * dt;
                double vy = y_ * dt;
                double vz = z_ * dt;

                double dx = vx * dt;
                double dy = vy * dt;
                double dz = vz * dt;

                dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
            i=2;
            timeEnd = (double) System.currentTimeMillis() / 500;
        }else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(sensorEvent.values, 0, prevMagnetometerValues, 0, sensorEvent.values.length);
                magnetometerBool = true;
            }
            if (accelerometerBool && magnetometerBool) {
                SensorManager.getRotationMatrix(rotationMatrix, null, prevAccelorometerValues, prevMagnetometerValues);
                SensorManager.getOrientation(rotationMatrix, angle);
                mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rotationMatrix, angle)[0]) + 360) % 360;
                mAzimuth = Math.round(mAzimuth);
            }
            if (latitude2Text.isActivated() && longitude2Text.isActivated() && latitude1Text.isActivated()){
                if (accelerometerBool == true) {
                    double vd = dist * Math.sin(Math.toRadians(mAzimuth)); //Vertical component of distance
                    double hd = dist * Math.cos(Math.toRadians(mAzimuth)); //Horizontal component
                    double c_lat = Double.parseDouble(String.valueOf(latitude2Text.getText()));
                    double c_long = Double.parseDouble((String.valueOf(longitude2Text.getText())));
                    double delta_longitude = vd / (111320 * Math.cos(Math.toRadians(c_lat)));
                    double delta_latitude = hd / 110540;


                    latitude2Text.setText(String.format("%.10f", delta_latitude + c_lat));
                    longitude2Text.setText(String.format("%.10f", delta_longitude + c_long));

                    float d = 0;
                    float dlon = (float) Math.toRadians(Double.parseDouble(String.valueOf(latitude2Text.getText()))) - (float) Math.toRadians(Double.parseDouble(String.valueOf(latitude1Text.getText())));
                    float dlat = (float) Math.toRadians(Double.parseDouble(String.valueOf(longitude2Text.getText()))) - (float) Math.toRadians(Double.parseDouble(String.valueOf(longitude1Text.getText())));
                    double tmp1 = Math.pow(Math.sin(dlat / 2), 2);
                    double tmp2 = Math.pow(Math.sin(dlon / 2), 2);
                    float a = (float) (tmp1 + Math.cos(Double.parseDouble(String.valueOf(latitude1Text.getText()))) * Math.cos(Double.parseDouble(String.valueOf(latitude2Text.getText()))) * tmp2);
                    float c = (float) (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
                    d = 6378 * c * 1000;
                    distanceText.setText(String.format("%.10f", d));
                    accelerometerBool = false;
                }
            }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {
    }

}
