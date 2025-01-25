package com.example.ambucare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class login extends AppCompatActivity {

    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        final EditText email = findViewById(R.id.edit1); // Username or email field
        final EditText password = findViewById(R.id.edit2); // Password field
        final Button btn = findViewById(R.id.button2);
        final TextView registernowbtn = findViewById(R.id.registerbtn);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String emailTxt = email.getText().toString();
                final String passwordTxt = password.getText().toString();

                if (emailTxt.isEmpty() || passwordTxt.isEmpty()) {
                    Toast.makeText(login.this, "Please enter your email and password", Toast.LENGTH_SHORT).show();
                } else {
                    // Query the database for the user
                    databaseReference.child(emailTxt).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // User exists, check password
                                String getPassword = snapshot.child("password").getValue(String.class);
                                if (getPassword != null && getPassword.equals(passwordTxt)) {
                                    // Retrieve user details
                                    String fullName = snapshot.child("fullName").getValue(String.class);
                                    String password = snapshot.child("password").getValue(String.class);
                                    String licenseNumber = snapshot.child("licenseNumber").getValue(String.class);
                                    String licenseExpiryDate = snapshot.child("licenseExpiryDate").getValue(String.class);
                                    String vehicleRegistrationNumber = snapshot.child("vehicleRegistrationNumber").getValue(String.class);
                                    String vehicleType = snapshot.child("vehicleType").getValue(String.class);

                                    // Pass data to Home activity
                                    Intent homeIntent = new Intent(login.this, Home.class);
                                    homeIntent.putExtra("name", fullName);
                                    homeIntent.putExtra("email", emailTxt);
                                    homeIntent.putExtra("password", passwordTxt);
                                    homeIntent.putExtra("license", licenseNumber);
                                    homeIntent.putExtra("expiry", licenseExpiryDate);
                                    homeIntent.putExtra("vehicleRegistrationNumber", vehicleRegistrationNumber);
                                    homeIntent.putExtra("vehicleType", vehicleType);

                                    startActivity(homeIntent);
                                    finish(); // Close login activity
                                } else {
                                    Toast.makeText(login.this, "Wrong password", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(login.this, "User does not exist", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(login.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        registernowbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(login.this, HomeFragment.class));
            }
        });
    }
}
