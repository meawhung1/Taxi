package th.in.spksoft.taxi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private Location last_location;
    private LocationManager locationManager;
    private GoogleApiClient mGoogleApiClient;
    private Marker mkSource;
    private Marker mkDestination;
    int PLACE_PICKER_REQUEST_SOURCE = 1;
    int PLACE_PICKER_REQUEST_DESTINATION = 2;
    boolean first = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 5555);
            }
        }
        mMap.setMyLocationEnabled(true);
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    public void onBtnClicked(View v) throws GooglePlayServicesNotAvailableException, GooglePlayServicesRepairableException {
        if(v.getId() == R.id.btnRefresh) {
            new backLoadJson().execute();
        } else if(v.getId() == R.id.btnSource) {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST_SOURCE);
        } else if(v.getId() == R.id.btnDest) {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST_DESTINATION);
        }

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST_SOURCE) {
            if (resultCode == RESULT_OK) {
                if (mkSource != null) mkSource.remove();
                Place place = PlacePicker.getPlace(data, this);
                Toast.makeText(getApplicationContext(), place.getName(), Toast.LENGTH_LONG).show();
                mkSource = mMap.addMarker(new MarkerOptions().position(place.getLatLng()).title("Source : " + place.getName().toString()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                if(mkDestination != null) {
                    TextView tmp = (TextView) findViewById(R.id.txtPrice);
                    tmp.setText("Price : " + String.valueOf(calculatePriceFromMeter(getMeterFromLatLng(place.getLatLng(), mkDestination.getPosition()))));
                }
            }
        } else if(requestCode == PLACE_PICKER_REQUEST_DESTINATION) {
            if (resultCode == RESULT_OK) {
                if (mkDestination != null) mkDestination.remove();
                Place place = PlacePicker.getPlace(data, this);
                Toast.makeText(getApplicationContext(), place.getName(), Toast.LENGTH_LONG).show();
                mkDestination = mMap.addMarker(new MarkerOptions().position(place.getLatLng()).title("Destination : " + place.getName().toString()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                if(mkSource != null) {
                    TextView tmp = (TextView) findViewById(R.id.txtPrice);
                    tmp.setText("Price : " + String.valueOf(calculatePriceFromMeter(getMeterFromLatLng(mkSource.getPosition(), place.getLatLng()))));
                }
            }
        }
    }

    public static double getMeterFromLatLng(LatLng source, LatLng destination) {
        double lat1 = source.latitude;
        double lng1 = source.longitude;
        double lat2 = destination.latitude;
        double lng2 = destination.longitude;
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        float dist = (float) (earthRadius * c);
        return dist;
    }
    public static double calculatePriceFromMeter(double meter) {
        if(meter <= 1000) return 35;
        else if(meter>1000 && meter<=11000){
            double temp;
            temp = meter-1000;
            temp = (int)temp/1000;
            return 35+(temp*(5.5));
        }
        else if(meter>11000 && meter<=21000){
            double temp;
            temp = meter-11000;
            temp = (int)temp/1000;
            return 95.5+(temp*(6));
        }
        else if(meter>21000){
            double temp;
            temp = meter-21000;
            temp = (int)temp/1000;
            return 155.5+(temp*(6.5));
        }
        else return 0;
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    @Override
    public void onLocationChanged(Location location) {
        last_location = location;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(MapsActivity.this, "WTF", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 5555);
            }
        }
        LocationRequest mLocationRequest = createLocationRequest();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        last_location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(last_location.getLatitude(), last_location.getLongitude())));
        mMap.animateCamera( CameraUpdateFactory.zoomTo( 12.0f ) );
        if(first == false) {
            first = true;
            new backLoadJson().execute();

        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private class backLoadJson extends AsyncTask<Void, Void, String> {
        //private ProgressDialog pdia;

        protected void onPreExecute() {

        }

        protected String doInBackground(Void... params) {

            try {
                String responseText = GetResponseText("192.168.202.10=" + String.valueOf(last_location.getLatitude()) + "&lng=" + String.valueOf(last_location.getLongitude()));
                return responseText;
            } catch (IOException e) {

                return e.getMessage();
            }
        }

        protected void onProgressUpdate(Integer... values) {

        }

        protected void onPostExecute(String result) {
            Log.d("aaaaaaaaaaaaaa", result);
            mMap.clear();

            try {
                JSONObject jsonobject = new JSONObject(result);
                JSONArray list = jsonobject.getJSONArray("data");
                for(int i = 0;i < list.length();i++){
                    JSONObject t = list.getJSONObject(i);
                    double lat = t.getDouble("lat");
                    double lng = t.getDouble("lng");
                    String name = t.getString("name");
                    LatLng pin = new LatLng(lat, lng);
                    mMap.addMarker(new MarkerOptions().position(pin).title(name));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("bbbbbbbbbbbb", e.getMessage());
            }

        }

        private String GetResponseText(String stringUrl) throws IOException {
            StringBuilder response = new StringBuilder();
            URL url = new URL(stringUrl);
            HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();
            if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader input = new BufferedReader(new InputStreamReader(httpconn.getInputStream()), 8192);
                String strLine = null;
                while ((strLine = input.readLine()) != null) {
                    response.append(strLine);
                }
                input.close();
            } else {
                return null;
            }
            return response.toString();
        }

    }

}
