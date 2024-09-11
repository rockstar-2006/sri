package com.example.ambucare;

import static com.example.ambucare.Common.currentUser;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class mapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private SupportMapFragment mapFragment;
    private GeoFire geoFire;
    private DatabaseReference onlineRef, driversLocationRef;
    private Marker driverMarker;
    private ValueEventListener onlineValueEventListener;
    private FirebaseDatabase database; // Initialize FirebaseDatabase reference here
    private String userId; // Declare userId as a class-level variable

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        FirebaseApp.initializeApp(getContext());

        // Initialize Firebase references and user ID
        initFirebase();

        // Initialize the map fragment
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        listenForRiderLocation();


        return rootView;
    }

    private String getUserId() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("user_id", null);

        if (userId == null) {
            userId = UUID.randomUUID().toString(); // Generate a new unique ID
            sharedPreferences.edit().putString("user_id", userId).apply(); // Store the ID locally
        }
        return userId;
    }

    private void clearUserId() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().remove("user_id").apply(); // Clear the stored ID
    }

    private void initFirebase() {
        // Initialize Firebase
        database = FirebaseDatabase.getInstance();

        // Get the user ID
        userId = getUserId();

        // Make sure userId is not null before proceeding
        if (userId != null && !userId.isEmpty()) {
            driversLocationRef = database.getReference("users").child(userId).child("driverLocation");

            // Initialize GeoFire with the correct reference
            geoFire = new GeoFire(driversLocationRef);

            // Reference for online status
            onlineRef = database.getReference().child("driverlocation");

            // Remove the driver location on disconnect
            driversLocationRef.onDisconnect().removeValue();

            // Clear any existing data for the user
            driversLocationRef.removeValue();
        } else {
            Log.e("FirebaseError", "User ID is null or empty. Cannot initialize Firebase references.");
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        requestLocationPermission();

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_maps_style));
            if (!success) {
                Log.e("EMDT_ERROR", "Style parsing error");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("EMDT_ERROR", e.getMessage());
        }
    }

    private void requestLocationPermission() {
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            mMap.setMyLocationEnabled(true);
                            mMap.getUiSettings().setMyLocationButtonEnabled(true);
                            startLocationUpdates();

                            Snackbar.make(getView(), getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(getContext(), "Permission " + response.getPermissionName() + " was denied!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest request, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void startLocationUpdates() {
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(15000)
                .setFastestInterval(10000)
                .setSmallestDisplacement(50f);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));

                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addressList;
                try {
                    addressList = geocoder.getFromLocation(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude(), 1);

                    if (addressList != null && addressList.size() > 0) {
                        String cityName = addressList.get(0).getLocality();

                        // Update location in Firebase using GeoFire
                        geoFire.setLocation(userId, new GeoLocation(newPosition.latitude, newPosition.longitude), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                if (error != null) {
                                    Snackbar.make(mapFragment.getView(), error.getMessage(), Snackbar.LENGTH_LONG).show();
                                } else {
                                    Snackbar.make(mapFragment.getView(), "You're online", Snackbar.LENGTH_LONG).show();
                                }
                            }
                        });

                        // Save the city name to Firebase Realtime Database
                        driversLocationRef.child("cityName").setValue(cityName);
                    }

                } catch (IOException e) {
                    Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                }

                // Update only necessary data once
                driversLocationRef.child("latitude").setValue(locationResult.getLastLocation().getLatitude());
                driversLocationRef.child("longitude").setValue(locationResult.getLastLocation().getLongitude());
            }
        };

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(getView(), getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    public void onResume() {
        super.onResume();
        registerOnlineSystem();
    }

    private void registerOnlineSystem() {
        onlineValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && driversLocationRef != null) {
                    // Ensure the driver location is removed when offline
                    driversLocationRef.child(userId).child("driverLocation").onDisconnect().removeValue();
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (mapFragment.getView() != null) {
                    Snackbar.make(mapFragment.getView(), error.getMessage(), Snackbar.LENGTH_LONG).show();
                }
            }

        };
        if (onlineRef != null) {
            onlineRef.addValueEventListener(onlineValueEventListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        if (geoFire != null) {
            geoFire.removeLocation(userId);
        }
        if (onlineRef != null && onlineValueEventListener != null) {
            onlineRef.removeEventListener(onlineValueEventListener);
        }
        // Remove the driver location from the database
        if (driversLocationRef != null) {
            driversLocationRef.removeValue();
        }
        FirebaseDatabase.getInstance().goOffline();
    }
    private void listenForRiderLocation() {
        DatabaseReference riderLocationRef = FirebaseDatabase.getInstance().getReference("Riders").child("Rider").child("location");
        riderLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                    double longitude = dataSnapshot.child("longitude").getValue(Double.class);

                    LatLng riderLatLng = new LatLng(latitude, longitude);
                    moveDriverToRider(riderLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", error.getMessage());
            }
        });
    }


    private void moveDriverToRider(LatLng riderLatLng) {
        if (driverMarker == null) {
            // Add the driver marker to the map if it does not exist yet
            driverMarker = mMap.addMarker(new MarkerOptions().position(riderLatLng).title("Driver"));
        } else {
            // Smoothly animate the driver marker to the rider's location
            animateMarker(driverMarker, riderLatLng);
        }
    }

    private void animateMarker(final Marker marker, final LatLng toPosition) {
        final LatLng startPosition = marker.getPosition();
        final long duration = 2000; // Duration of the animation in milliseconds
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * toPosition.longitude + (1 - t) * startPosition.longitude;
                double lat = t * toPosition.latitude + (1 - t) * startPosition.latitude;

                marker.setPosition(new LatLng(lat, lng));

                // Repeat till the animation duration completes
                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }
}
