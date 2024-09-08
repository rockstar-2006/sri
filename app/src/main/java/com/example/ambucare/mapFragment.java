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

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        database = FirebaseDatabase.getInstance(); // Initialize Firebase database reference here
        driversLocationRef = database.getReference("users"); // Initialize reference to users node
        geoFire = new GeoFire(driversLocationRef.child("driverLocation"));
        onlineRef = database.getReference().child("driverlocation");
        userId = getUserId(); // Initialize userId here
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
                .setInterval(5000)
                .setFastestInterval(3000)
                .setSmallestDisplacement(10f);

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
                        geoFire.setLocation(userId,
                                new GeoLocation(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()),
                                new GeoFire.CompletionListener() {
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
                        driversLocationRef.child(userId).child("driverLocation").child("cityName").setValue(cityName);
                    }

                } catch (IOException e) {
                    Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                }

                // Update additional data for each user
                driversLocationRef.child(userId).child("driverLocation").child("latitude").setValue(locationResult.getLastLocation().getLatitude());
                driversLocationRef.child(userId).child("driverLocation").child("longitude").setValue(locationResult.getLastLocation().getLongitude());
                driversLocationRef.child(userId).child("fullName").setValue("John Doe");
                driversLocationRef.child(userId).child("licenseExpiryDate").setValue("2025-12-31");
                driversLocationRef.child(userId).child("licenseNumber").setValue("123456");
                driversLocationRef.child(userId).child("password").setValue("securepassword");
                driversLocationRef.child(userId).child("phoneNumber").setValue("9876543210");
                driversLocationRef.child(userId).child("vehicleRegistrationNumber").setValue("AB123CD");
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
                    driversLocationRef.child(userId).onDisconnect().removeValue();
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
    }
}
