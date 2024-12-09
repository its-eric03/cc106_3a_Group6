package com.example.schedulenotifier;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.shedulenotifer.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_EXCEL_FILE = 1;
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String MIME_TYPE_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private DBHandler dbHandler;
    private DrawerLayout drawerLayout;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Database Handler
        dbHandler = DBHandler.getInstance(this);

        // Set up the Toolbar
        setupToolbar();

        // Set up the Drawer Layout and Navigation View
        setupNavigationDrawer();

        // Set up the Bottom Navigation Bar
        setupBottomNavigationBar();

        setupUploadButton();
    }

    /**
     * Sets up the Toolbar and configures it as the ActionBar.
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // Set the Toolbar as the ActionBar
    }

    /**
     * Sets up the DrawerLayout and NavigationView, including the drawer toggle.
     */
    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        // Set up the Drawer Toggle (Hamburger Icon)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                findViewById(R.id.toolbar), // Pass the Toolbar
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Get Header View from NavigationView
        View headerView = navigationView.getHeaderView(0);

        // Find header TextViews
        TextView tvUserName = headerView.findViewById(R.id.tv_user_name);
        TextView tvUserEmail = headerView.findViewById(R.id.tv_user_email);

        // Fetch data from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "Your Name");
        String email = sharedPreferences.getString("email", "your.email@example.com");

        // Update TextViews with real data
        tvUserName.setText(username);
        tvUserEmail.setText(email);

        // Handle Navigation Item Selections
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                // Navigate to Profile Activity
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_about) {
                // Show an About App dialog
                new AlertDialog.Builder(this)
                        .setTitle("About App")
                        .setMessage("Schedule Notifier is an application designed to help manage and notify you about your daily schedules effectively.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
            } else if (id == R.id.nav_logout) {
                // Logout functionality
                logoutUser();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }




    /**
     * Sets up the Bottom Navigation Bar and handles its item selection.
     */
    private void setupBottomNavigationBar() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Handle button clicks
            if (itemId == R.id.nav_delete) {
                showDeleteConfirmationDialog();
                return true;
            } else if (itemId == R.id.nav_show_schedule) { // Replace `nav_show_schedule` with the actual ID of your "Schedule" button.
                showHeadsUpNotification(this); // Call the method here
                return true;
            } else if (itemId == R.id.nav_set_alarm) {
                scheduleAlarms(this);
                return true;
            } else if (itemId == R.id.nav_display_data) {
                displayImportedData();
                return true;
            } else {
                return false;
            }
        });
    }



    private void setupUploadButton() {
        Button uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v -> {
            if (hasPermissions()) {
                openFilePicker();
            } else {
                requestPermissions();
            }
        });
    }


    /**
     * Logs the user out and navigates back to the LoginActivity.
     */
    private void logoutUser() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Redirect to LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close MainActivity
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }


    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Records")
                .setMessage("Are you sure you want to delete all records?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    dbHandler.deleteAllRecords();
                    Toast.makeText(MainActivity.this, "All records deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void showHeadsUpNotification(Context context) {
        try {
            // Fetch today's classes from DBHandler
            List<String[]> todayClasses = dbHandler.getTodayClasses();

            if (todayClasses.isEmpty()) {
                Toast.makeText(context, "No classes scheduled for today!", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder scheduleBuilder = new StringBuilder();
            for (String[] classInfo : todayClasses) {
                String subject = classInfo[0]; // Subject
                String timeFrom = classInfo[2]; // Time From
                String timeTo = classInfo[3];   // Time To
                String roomNo = classInfo[4];  // Room No
                scheduleBuilder.append("Class: ").append(subject)
                        .append("\nTime: ").append(timeFrom).append(" - ").append(timeTo)
                        .append("\nRoom: ").append(roomNo)
                        .append("\n\n");
            }

            // Build and display the notification
            String scheduleText = scheduleBuilder.toString().trim();

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            String channelId = "heads_up_channel";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "Today's Schedule",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Shows your schedule for today");
                channel.enableVibration(true);
                channel.enableLights(true);
                notificationManager.createNotificationChannel(channel);
            }

            Notification notification = new NotificationCompat.Builder(context, channelId)
                    .setContentTitle("Today's Schedule")
                    .setContentText("Tap to view full schedule")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(scheduleText))
                    .setSmallIcon(R.drawable.notif_logo)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .build();

            notificationManager.notify(1, notification);
        } catch (Exception e) {
            Log.e("MainActivity", "Error displaying notification: " + e.getMessage());
            Toast.makeText(context, "Error showing notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void displayImportedData() {
        try {
            // Fetch updated student records from the database
            List<String[]> importedData = dbHandler.getFormattedStudentRecords();

            if (importedData.isEmpty()) {
                Toast.makeText(this, "No records found.", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "No data found in the database.");
                return;
            }

            // Log imported data for debugging
            for (String[] record : importedData) {
                Log.d("MainActivity", "Record: " + String.join(", ", record));
            }

            // Pass the imported data to the DisplayDataActivity
            Intent intent = new Intent(this, DisplayDataActivity.class);
            intent.putExtra("data", (Serializable) importedData);
            startActivity(intent);

            Toast.makeText(this, "Data displayed successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error displaying data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Exception while displaying data: " + e.getMessage(), e);
        }
    }


    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(MIME_TYPE_EXCEL);
        startActivityForResult(intent, PICK_EXCEL_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_EXCEL_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                Log.d("MainActivity", "File URI: " + uri.toString());
                importDataFromUri(uri);
            } else {
                Log.e("MainActivity", "File picker returned a null URI.");
                Toast.makeText(this, "Failed to select file.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void importDataFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                dbHandler.importExcelData(inputStream);
                Toast.makeText(this, "Data imported successfully!", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Data imported successfully from: " + uri.toString());
            } else {
                Toast.makeText(this, "Failed to open the file.", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "InputStream is null for URI: " + uri.toString());
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "File not found: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "FileNotFoundException: " + e.getMessage(), e);
        } catch (Exception e) {
            Toast.makeText(this, "Error importing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Exception while importing data: " + e.getMessage(), e);
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleAlarms(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "Exact alarms are not permitted for this app.", Toast.LENGTH_LONG).show();
                    Log.e("MainActivity", "Exact alarms permission not granted.");
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    return;
                }
            }

            List<String[]> todayClasses = dbHandler.getTodayClasses();

            if (todayClasses.isEmpty()) {
                Toast.makeText(context, "No classes scheduled for today!", Toast.LENGTH_SHORT).show();
                return;
            }

            for (String[] classDetails : todayClasses) {
                String subject = classDetails[0];
                String time = classDetails[2];
                String room_no = classDetails[3];

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                Date parsedTime;

                try {
                    parsedTime = sdf.parse(time);
                    if (parsedTime == null) throw new ParseException("Invalid time format", 0);

                    calendar.setTime(parsedTime);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH));
                    calendar.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

                } catch (ParseException e) {
                    Log.e("MainActivity", "Invalid time: " + time, e);
                    continue;
                }

                if (calendar.before(Calendar.getInstance())) {
                    Toast.makeText(context, "The time for " + subject + " at " + time + " has already passed.", Toast.LENGTH_SHORT).show();
                    continue;
                }

                int requestCode = (subject + time + room_no).hashCode();

                Intent intent = new Intent(context, AlarmReceiver.class);
                intent.putExtra("subject", subject);
                intent.putExtra("time", time);
                intent.putExtra("room_no", room_no);

                PendingIntent alarmIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);

                Toast.makeText(context, "Alarm set for: " + time, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("MainActivity", "Error scheduling alarms", e);
        }
    }



    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_PERMISSIONS);
            } else {
                openFilePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
            } else {
                openFilePicker();
            }
        }
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
