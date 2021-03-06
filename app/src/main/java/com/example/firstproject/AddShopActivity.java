package com.example.firstproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddShopActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks {

    private EditText shopName;
    private EditText shopDesc;
    private EditText shopRad;
    private static final int PERMISSION_ID = 44;
    private FusedLocationProviderClient mFusedLocationClient;
    private FirebaseFirestore db;
    private GeofencingClient gc;
    private static final long UPDATE_INTERVAL = 10 * 1000;
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;
    private static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent in = getIntent();
        Boolean str = in.getBooleanExtra("darkMode", false);
        changeTheme(str);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_shop);
        db = FirebaseFirestore.getInstance();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        gc = LocationServices.getGeofencingClient(this);

        shopName = findViewById(R.id.etShopName);
        shopDesc = findViewById(R.id.etShopDesc);
        shopRad = findViewById(R.id.etShopRad);
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted. Start getting the location information
            }
        }
    }

    private void changeTheme(boolean dark){
        if(dark){
            setTheme(R.style.AppTheme2);
        }else{
            setTheme(R.style.AppTheme);
        }
    }

    public void saveShop(View view) {

        if (!validateForm(shopName.getText().toString(), shopDesc.getText().toString(), shopRad.getText().toString())) {
            return;
        }

        final String name = shopName.getText().toString();
        final String desc = shopDesc.getText().toString();
        final int radius = Integer.parseInt(shopRad.getText().toString());

        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if(location != null){
                            Shop newShop = new Shop(name, desc, radius, location.getLongitude(), location.getLatitude(), FirebaseAuth.getInstance().getCurrentUser().getUid());
                            addShopToDb(newShop);

                            Geofence geo = new Geofence.Builder().setRequestId(name)
                                    .setCircularRegion(location.getLatitude(), location.getLongitude(), radius)
                                    .setExpirationDuration(1000*60*60)
                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                                    .build();

                            gc.addGeofences(getGeofencingRequest(geo),getGeofencePendingIntent());
                        }
                    }
                });

            } else {
                Toast.makeText(this, R.string.turnOnLoc, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }

    finish();

    }

    private GeofencingRequest getGeofencingRequest(Geofence geo) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geo);
        createLocationRequest();
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceBroadcast.class);
        intent.setAction(GeofenceBroadcast.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void addShopToDb(Shop shop){

        db.collection("shops")
                .add(shop)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
    }

    private void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setMaxWaitTime(MAX_WAIT_TIME);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private boolean validateForm(String name, String desc, String rad) {
        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            shopName.setError(getString(R.string.textRequire));
            valid = false;
        } else {
            shopName.setError(null);
        }

        if (TextUtils.isEmpty(desc)) {
            this.shopDesc.setError(getString(R.string.textRequire));
            valid = false;
        } else {
            this.shopDesc.setError(null);
        }

        if (TextUtils.isEmpty(rad)) {
            this.shopRad.setError(getString(R.string.textRequire));
            valid = false;
        } else {
            this.shopRad.setError(null);
        }

        return valid;
    }
}
