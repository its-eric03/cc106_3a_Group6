package com.example.schedulenotifier;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shedulenotifer.R;

public class ProfileActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Retrieve the username from SharedPreferences
        String username = getSharedPreferences("UserSession", MODE_PRIVATE).getString("username", null);

        if (username != null) {
            // Fetch user details from the database
            Cursor cursor = dbHelper.getUserDetails(username);
            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") String fetchedUsername = cursor.getString(cursor.getColumnIndex("username"));
                @SuppressLint("Range") String email = cursor.getString(cursor.getColumnIndex("email"));
                @SuppressLint("Range") String password = cursor.getString(cursor.getColumnIndex("password"));
                cursor.close();

                // Display user details
                TextView tvUsername = findViewById(com.example.shedulenotifer.R.id.tv_username);
                TextView tvEmail = findViewById(R.id.tv_email);
                TextView tvPassword = findViewById(R.id.tv_password);

                tvUsername.setText("Username: " + fetchedUsername);
                tvEmail.setText("Email: " + email);
                tvPassword.setText("Password: " + password);
            } else {
                Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No username found in session!", Toast.LENGTH_SHORT).show();
        }
    }
}
