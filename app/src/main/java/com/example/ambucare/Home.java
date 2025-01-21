package com.example.ambucare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.FrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Home extends AppCompatActivity {
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_home);

        imageView = findViewById(R.id.imageView);

        // Check if the app was launched with an intent
        Intent intent = getIntent();
        if (intent != null) {
            // Get the image URI from the intent
            Uri imageUri = intent.getData();

            // Check if the URI is not null
            if (imageUri != null) {
                // Display the image in the ImageView
                displayImage(imageUri);
            }
        }
    }

    private void displayImage(Uri imageUri) {
        try {
            // Get the bitmap from the image URI
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));

            // Display the bitmap in the ImageView
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            // Handle any exceptions that occur while displaying the image
            e.printStackTrace();
        }
    }
}

