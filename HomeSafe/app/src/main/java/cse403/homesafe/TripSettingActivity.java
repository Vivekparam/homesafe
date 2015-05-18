package cse403.homesafe;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cse403.homesafe.Data.Destination;
import cse403.homesafe.Data.Destinations;
import cse403.homesafe.Util.DistanceAndTime;
import cse403.homesafe.Util.GoogleMapsUtils;
import cse403.homesafe.Util.GoogleMapsUtilsCallback;


public class TripSettingActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMapsUtilsCallback {

    Button destinations;
    TimePicker ETA;
    Button startTrip;
    Location destination;
    Button newLocation;
    DistanceAndTime distAndTime;
    int PLACE_PICKER_REQUEST = 1;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    String TAG = "TripSettingActivity";

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_setting);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.setTitle("Trip Settings");
        Destination defaultDest = new Destination("No Available Destinations");

        Spinner input = (Spinner) findViewById(R.id.favoriteLocationSpinner);
        List<Destination> destinationList = Destinations.getInstance().getDestinations();
        if(destinationList.isEmpty()){
            destinationList.add(defaultDest);
        }
        final Map<String, Location> nameToLocation = new HashMap<String, Location>();
        ArrayList<String> stringList = new ArrayList<String>();
        for (Destination dest : destinationList) {
            stringList.add(dest.getName());
            nameToLocation.put(dest.getName(), dest.getLocation());
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spinner_item, stringList);
        dataAdapter.setDropDownViewResource(R.layout.spinner_drop_down);
        input.setAdapter(dataAdapter);
        input.setOnItemSelectedListener(new CustomOnItemSelectedListener(nameToLocation));

        newLocation = (Button) findViewById(R.id.button4);
        newLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int PLACE_PICKER_REQUEST = 1;
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                Context context = getApplicationContext();
                try {
                    startActivityForResult(builder.build(context), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        ETA = (TimePicker) findViewById(R.id.spinnerETA);
        ETA.setIs24HourView(true);
        ETA.setCurrentHour(0);
        ETA.setCurrentMinute(0);

        startTrip = (Button) findViewById(R.id.startTripButton);
        startTrip.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View w) {
                        long time = 1000 * (ETA.getCurrentHour() * 3600 + ETA.getCurrentMinute() * 60);

                        if (time == 0) {
                            Toast.makeText(TripSettingActivity.this,
                                    "You cannot start a trip with no time set",
                                    Toast.LENGTH_SHORT).show();
                        } else if (destination == null) {
                            Toast.makeText(TripSettingActivity.this,
                                    "You cannot start a trip with no destination set",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Intent i = new Intent(getApplicationContext(), HSTimerActivity.class);
                            i.putExtra("timefromuser", time);
                            startActivity(i);
                        }
                    }
                }
        );
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                //TODO actually do something with the
                Place place = PlacePicker.getPlace(data, this);
                destination = new Location("New Destination");
                destination.setLatitude(place.getLatLng().latitude);
                destination.setLongitude(place.getLatLng().longitude);
                TextView currentDestinationText = (TextView) findViewById(R.id.currentDestinationText);
                currentDestinationText.setText("Destination: " + place.getName());
                if (checkPlayServices()) {
                    Log.e(TAG, "Google Play Services is installed");
                    buildGoogleApiClient();
                    onStart();
                } else {
                    Log.e(TAG, "Google Play Services is not installed");
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_trip_setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        if (mGoogleApiClient != null) {
            Log.e(TAG, "Build Complete");
        } else {
            Log.e(TAG, "Build Incomplete");
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        Log.e(TAG, "Connected!");
        if (mLastLocation != null) {
            GoogleMapsUtils.getDistanceAndTime(mLastLocation, destination, this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            Log.e(TAG, "Connection Started");
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Connection Suspended");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection Failed");
    }

    @Override
    public void onGetDistanceAndTime(final Object obj) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "onGetDistanceAndTime was called within TripSettingActivity");
                if (obj instanceof DistanceAndTime) {
                    distAndTime = (DistanceAndTime) obj;
                    int hours = (int) distAndTime.getTime() / 3600;
                    int minutes = ((int) distAndTime.getTime() - hours * 3600) / 60;
                    ETA.setCurrentHour(hours);
                    ETA.setCurrentMinute(minutes);

                    if (distAndTime != null && mLastLocation != null) {
                        startTrip.setEnabled(true);
                        String estimationMsg = "Your estimated time of arrival is " + hours +
                                " hours and " + minutes + " minutes.";
                        Toast.makeText(TripSettingActivity.this, estimationMsg, Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
    }

    @Override
    public void onAddressToLocation(Object obj) { }


    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    public class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener{
        Map<String, Location> nameToLocation;
        public CustomOnItemSelectedListener(Map<String, Location> nameToLocation){
            this.nameToLocation = nameToLocation;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String nameOfDest = parent.getItemAtPosition(position).toString() + "";
            destination = nameToLocation.get(nameOfDest);
            if (checkPlayServices()) {
                Log.e(TAG, "Google Play Services is installed");
                buildGoogleApiClient();
                onStart();
            } else {
                Log.e(TAG, "Google Play Services is not installed");
            }

            TextView currentDestinationText = (TextView) findViewById(R.id.currentDestinationText);
            currentDestinationText.setText("Destination: " + nameOfDest);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
}
