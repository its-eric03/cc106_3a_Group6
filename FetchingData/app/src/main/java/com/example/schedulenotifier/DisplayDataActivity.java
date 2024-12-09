package com.example.schedulenotifier;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


import com.example.shedulenotifer.R;

import java.util.List;

public class DisplayDataActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the activity to landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(com.example.shedulenotifer.R.layout.activity_display_data);

        TableLayout tableLayout = findViewById(R.id.tableLayout);

        // Get the DBHandler instance
        DBHandler dbHandler = DBHandler.getInstance(this);

        // Retrieve the first student's ID and Name
        String[] firstStudent = dbHandler.getFirstStudentIdAndName(); // Fetch Student ID and Name

        // Set the Student Name and ID in the header
        TextView nameTextView = findViewById(R.id.nameTextView);
        TextView idTextView = findViewById(R.id.idTextView);

        if (firstStudent != null) {
            nameTextView.setText(firstStudent[1]); // Display Name
            idTextView.setText(firstStudent[0]);  // Display Student ID
        } else {
            nameTextView.setText("N/A");
            idTextView.setText("N/A");
        }

        // Retrieve formatted student records
        List<String[]> records = dbHandler.getFormattedStudentRecords();

        if (records != null && !records.isEmpty()) {
            // Create header row for table
            TableRow headerRow = new TableRow(this);
            headerRow.setBackgroundColor(Color.LTGRAY); // Set header background color

            // Define column headers
            String[] columnHeaders = {"Subject ID", "Subject", "Day", "Time From", "Time To", "Room No"};

            // Add TextViews for column headers
            for (String column : columnHeaders) {
                TextView header = new TextView(this);
                header.setText(column.toUpperCase());
                header.setPadding(16, 16, 16, 16); // Padding for better spacing
                header.setTextColor(Color.BLACK); // Black text color
                header.setTypeface(null, android.graphics.Typeface.BOLD); // Bold text
                header.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)); // Equal column widths
                headerRow.addView(header);
            }

            // Add header row to TableLayout
            tableLayout.addView(headerRow);

            // Populate table rows with data
            for (String[] record : records) {
                TableRow dataRow = new TableRow(this);
                dataRow.setBackgroundColor(Color.WHITE); // Set row background color

                // Skip Student ID (index 0) in table rows
                for (int i = 1; i < record.length; i++) {
                    TextView data = new TextView(this);
                    data.setText(record[i]); // Populate table cells
                    data.setPadding(16, 16, 16, 16); // Padding for better spacing
                    data.setTextColor(Color.BLACK); // Black text color
                    data.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)); // Equal column widths
                    dataRow.addView(data);
                }

                // Add data row to TableLayout
                tableLayout.addView(dataRow);
            }
        } else {
            // Handle case where no data is available
            TableRow noDataRow = new TableRow(this);
            TextView noDataText = new TextView(this);
            noDataText.setText("No data available.");
            noDataText.setPadding(16, 16, 16, 16);
            noDataText.setTextColor(Color.BLACK);
            noDataRow.addView(noDataText);
            tableLayout.addView(noDataRow);
        }
    }
}
