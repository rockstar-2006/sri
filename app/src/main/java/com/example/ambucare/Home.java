package com.example.ambucare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Home extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottomnavigation);
        frameLayout = findViewById(R.id.frame);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navHome) {
                    loadFragment(new HomeFragment(), false);
                } else if (itemId == R.id.navmap) {
                    loadFragment(new mapFragment(), false);
                } else if (itemId == R.id.navprofile) {
                    loadProfileFragment(); // Load profile fragment
                } else {
                    loadFragment(new chatbotFragment(), false);
                }
                return false;
            }
        });

        loadFragment(new HomeFragment(), true);
    }

    private void loadProfileFragment() {
        // Retrieve data from Intent
        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String email = intent.getStringExtra("email");
        String phoneNumber = intent.getStringExtra("phoneNumber");
        String license = intent.getStringExtra("license");
        String expiry = intent.getStringExtra("expiry");
        String vehicleRegistrationNumber = intent.getStringExtra("vehicleRegistrationNumber");
        String vehicleType = intent.getStringExtra("vehicleType");

        // Create a Bundle with the data
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("email", email);
        bundle.putString("phoneNumber", phoneNumber);
        bundle.putString("license", license);
        bundle.putString("expiry", expiry);
        bundle.putString("vehicleRegistrationNumber", vehicleRegistrationNumber);
        bundle.putString("vehicleType", vehicleType);

        // Pass the Bundle to the profileFragment
        profileFragment profileFragment = new profileFragment();
        profileFragment.setArguments(bundle);

        loadFragment(profileFragment, true);
    }

    private void loadFragment(Fragment fragment, boolean isAppInitialized) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (isAppInitialized) {
            fragmentTransaction.add(R.id.frame, fragment);
        } else {
            fragmentTransaction.replace(R.id.frame, fragment);
        }
        fragmentTransaction.commit();
    }
}
