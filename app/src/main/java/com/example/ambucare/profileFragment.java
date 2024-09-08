package com.example.ambucare;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.Manifest;


import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class profileFragment extends Fragment {

    TextView editTextFullName, editTextLastName, profileUsername, editTextPhoneNumber, editTextLicenseNumber, editTextLicenseExpiryDate;
    TextView titleName;
    Button signOutButton;
    ImageView profileImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize your views here
        editTextFullName = view.findViewById(R.id.editTextFullName);
        editTextLastName = view.findViewById(R.id.editTextLastName);
        profileUsername = view.findViewById(R.id.profileUsername);
        editTextPhoneNumber = view.findViewById(R.id.editTextPhoneNumber);
        editTextLicenseNumber = view.findViewById(R.id.editTextLicenseNumber);
        editTextLicenseExpiryDate = view.findViewById(R.id.editTextLicenseExpiryDate);
        titleName = view.findViewById(R.id.titleName);
        signOutButton = view.findViewById(R.id.editButton); // Initialize the sign-out button
        profileImageView = view.findViewById(R.id.profileImg);

        // Display user data
        Bundle bundle = getArguments();
        if (bundle != null) {
            titleName.setText(bundle.getString("name"));
            editTextFullName.setText(bundle.getString("name"));
            editTextLastName.setText(bundle.getString("email"));
            profileUsername.setText(bundle.getString("username"));
            editTextPhoneNumber.setText(bundle.getString("password"));
            editTextLicenseNumber.setText(bundle.getString("license"));
            editTextLicenseExpiryDate.setText(bundle.getString("expiry"));
        }

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle selecting an image (from gallery or camera)
                checkStoragePermission();
                selectProfileImage();
            }
        });

        // Set the click listener for the sign-out button
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle sign out logic here
                signOut();
            }
        });

        return view;
    }

    // Method to handle image selection
    private void selectProfileImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }

    // Handle the result of the image selection
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK && requestCode == 1 && data != null) {
            Uri selectedImage = data.getData();
            profileImageView.setImageURI(selectedImage); // Display the selected image in the ImageView
        }
    }

    // Method to handle sign out
    private void signOut() {
        // Clear user session data or authentication tokens
        // For example, if using Firebase Authentication:
        // FirebaseAuth.getInstance().signOut();

        // Redirect to the login or welcome screen
        Intent intent = new Intent(getActivity(), login.class); // Change LoginActivity to your login activity
        startActivity(intent);

        // Optional: close the current activity if this is part of an activity
        getActivity().finish();
    }

    // Check for storage permission
    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
        }
    }
}