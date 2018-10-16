package com.example.phaniteja.friendfinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class friendSearch extends FragmentActivity implements OnMapReadyCallback,
        View.OnClickListener,
        LocationListener {

    private static final long timerTIME = 60000;
    private GoogleMap mMap;
    private LocationManager lManager = null;
    private Context mcontext = null;
    private LatLng currentLocation = null;
    private Handler handlerLocation = null;
    private Timer locationTimer = null;
    private Button buttonLogout = null;
    private boolean locationBool = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_search);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mcontext = this;
        lManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        buttonLogout = (Button)findViewById(R.id.btn_logout);
        buttonLogout.setOnClickListener(this);
        handlerLocation = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(mcontext);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mcontext,"Permissions need to be given for the application!",Toast.LENGTH_SHORT).show();
            return;
        }
        lManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        Location prevLocation = lManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(prevLocation!= null){
            currentLocation = new LatLng(prevLocation.getLatitude(),prevLocation.getLongitude());
        }else{
            currentLocation = new LatLng(39.2538094,-76.714081);
        }

        lManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);
        locTimer();
    }


    private void locTimer() {

        if(locationTimer == null){
            locationTimer = new Timer();
            TimerTask checkNearestFriends = new TimerTask() {
                @Override
                public void run() {
                    if(locationBool){
                        //invoke webservice for the list of friends near by
                        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(mcontext);
                        String[] contents = new String[4];
                        contents[0] = spref.getString("emailID","");
                        contents[1] = spref.getString("name","");
                        synchronized (currentLocation){
                            contents[2] = String.valueOf(currentLocation.latitude);
                            contents[3] = String.valueOf(currentLocation.longitude);
                            new findFriends().execute(contents);
                        }
                    }
                }
            };
            locationTimer.scheduleAtFixedRate(checkNearestFriends,20000, timerTIME);
        }


    }

    private void stopLocationTimer() {
        if(locationTimer != null){
            locationTimer.cancel();
            locationTimer.purge();
            locationTimer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lManager.removeUpdates(this);
        stopLocationTimer();
        locationBool = false;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }
    @Override
    public void onLocationChanged(Location location) {
        synchronized (currentLocation){
            locationBool = true;
            currentLocation = new LatLng(location.getLatitude(),location.getLongitude());
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_logout:
                SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(mcontext);
                spref.edit().remove("name").commit();
                spref.edit().remove("isSignedIN").commit();
                spref.edit().remove("emailID").commit();
                friendSearch.this.finish();
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);
        }
    }


    private class findFriends extends AsyncTask<String, Void, String> {

        String[] urlContents = null;
        @Override
        protected String doInBackground(String... params) {

            urlContents = params;
            URL url;
            String response = "";
            String requestURL = "http://ec2-52-14-81-211.us-east-2.compute.amazonaws.com/friendFinder.php?";
            try{

                StringBuilder sb = new StringBuilder();
                sb.append("emailID="+params[0]+"&"+"name="+params[1]+"&"+ "latitude="+params[2]+"&"+ "longitude="+params[3]+"&"+"radius=1&");

                requestURL= requestURL+sb.toString();
                url = new URL(requestURL);
                HttpURLConnection myconnection = (HttpURLConnection) url.openConnection();
                myconnection.setRequestMethod("GET");

                if(200 == HttpURLConnection.HTTP_OK){

                    InputStream in = url.openStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null) {

                        if(line.contains("Error")){
                            break;
                        }else {
                            result.append(line);
                        }
                    }
                    response = result.toString();

                }else{
                    Toast.makeText(mcontext,"Connection is not proper",Toast.LENGTH_SHORT).show();
                }

            }catch (Exception ex){
            }
            return response;
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);
            if(res!= null && !res.isEmpty()) {
                mMap.clear();
                try {
                    JSONObject jsonresponse = new JSONObject(res);
                    if(jsonresponse.has("friends")){
                        JSONArray frndsLocArr = (JSONArray) jsonresponse.get("friends");
                        for(int index =0; index<frndsLocArr.length(); index++){
                            JSONObject obj = (JSONObject) frndsLocArr.get(index);

                            LatLng frndlatlong = new LatLng(
                                    Float.valueOf((String)obj.get("latitude")),
                                    Float.valueOf((String)obj.get("longitude")));
                            mMap.addMarker(new MarkerOptions()
                                    .position(frndlatlong)
                                    .title((String)obj.get("name"))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        }
                    }
                    LatLng userlatlong = new LatLng(Float.valueOf(urlContents[2]),Float.valueOf(urlContents[3]));
                    mMap.addMarker(new MarkerOptions()
                            .position(userlatlong)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))).showInfoWindow();
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(userlatlong));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15),2000,null);
                } catch (Exception ex) {
                }
            }

        }
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}
