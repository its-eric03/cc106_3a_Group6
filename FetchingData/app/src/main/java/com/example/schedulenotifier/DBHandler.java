package com.example.schedulenotifier;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DBHandler extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "school.db";
    private static final int DATABASE_VERSION = 1;
    private static DBHandler instance;
    private final Context context;

    // Singleton constructor
    private DBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public static synchronized DBHandler getInstance(Context context) {
        if (instance == null) {
            instance = new DBHandler(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE student_data (" +
                "student_id INTEGER, " +
                "name TEXT, " +
                "subject_id TEXT, " +
                "subject TEXT, " +
                "day TEXT, " +
                "time_from TEXT, " +
                "time_to TEXT, " +
                "room_no TEXT, " +
                "PRIMARY KEY (student_id, subject_id))");
        Log.d("DBHandler", "Database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS student_data");
        onCreate(db);
        Log.d("DBHandler", "Database upgraded");
    }

    public void importExcelData(InputStream excelFile) {
        SQLiteDatabase db = null;
        Workbook workbook = null;

        try {
            workbook = new XSSFWorkbook(excelFile);
            db = this.getWritableDatabase();

            // Import data from the first sheet
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                ContentValues values = new ContentValues();

                // Get student_id
                if (row.getCell(0) != null && row.getCell(0).getCellType() == CellType.NUMERIC) {
                    values.put("student_id", (int) row.getCell(0).getNumericCellValue());
                }

                // Get name
                if (row.getCell(1) != null) {
                    values.put("name", getCellStringValue(row.getCell(1)));
                }

                // Get subject_id
                if (row.getCell(2) != null) {
                    values.put("subject_id", getCellStringValue(row.getCell(2)));
                }

                // Get subject
                if (row.getCell(3) != null) {
                    values.put("subject", getCellStringValue(row.getCell(3)));
                }

                // Get day
                if (row.getCell(4) != null) {
                    values.put("day", getCellStringValue(row.getCell(4)));
                }

                // Get time_from and format it
                if (row.getCell(5) != null) {
                    values.put("time_from", getFormattedTime(row.getCell(5)));
                }

                // Get time_to and format it
                if (row.getCell(6) != null) {
                    values.put("time_to", getFormattedTime(row.getCell(6)));
                }

                // Get room_no
                if (row.getCell(7) != null) { // Ensure the correct column index for room_no
                    values.put("room_no", getCellStringValue(row.getCell(7)));
                }

                // Insert into database
                long result = db.insert("student_data", null, values);
                if (result == -1) {
                    Log.e("DBHandler", "Failed to insert row: " + values);
                } else {
                    Log.d("DBHandler", "Row inserted: " + values);
                }
            }

            // Success message
            Toast.makeText(context, "Data imported successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("DBHandler", "Error importing data: " + e.getMessage());
            Toast.makeText(context, "Error importing data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (workbook != null) workbook.close();
                if (db != null) db.close();
            } catch (Exception e) {
                Log.e("DBHandler", "Error closing resources: " + e.getMessage());
            }
        }
    }

    // Helper method to format time cells
    private String getFormattedTime(Cell cell) {
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                DateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                return timeFormat.format(cell.getDateCellValue());
            } else {
                // If not a date-formatted cell, return the raw string value
                return cell.getStringCellValue();
            }
        } catch (Exception e) {
            Log.e("DBHandler", "Error formatting time: " + e.getMessage());
            return ""; // Return empty string if parsing fails
        }
    }

    // Helper method to get string values from cells
    private String getCellStringValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private int getNumericCellValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else {
            return 0; // Default value for numeric fields
        }
    }

    public List<String[]> getFormattedStudentRecords() {
        List<String[]> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Updated query to fetch all relevant fields
            String query = "SELECT student_id, subject_id, subject, day, time_from, time_to, room_no FROM student_data";
            cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    String[] record = new String[7]; // Adjust size to match the number of columns
                    record[0] = cursor.getString(cursor.getColumnIndexOrThrow("student_id"));  // Student ID
                    record[1] = cursor.getString(cursor.getColumnIndexOrThrow("subject_id"));  // Subject ID
                    record[2] = cursor.getString(cursor.getColumnIndexOrThrow("subject"));     // Subject
                    record[3] = cursor.getString(cursor.getColumnIndexOrThrow("day"));         // Day

                    // Parse and format `time_from`
                    String timeFromRaw = cursor.getString(cursor.getColumnIndexOrThrow("time_from"));
                    record[4] = formatTimeStandard(timeFromRaw);

                    // Parse and format `time_to`
                    String timeToRaw = cursor.getString(cursor.getColumnIndexOrThrow("time_to"));
                    record[5] = formatTimeStandard(timeToRaw);

                    record[6] = cursor.getString(cursor.getColumnIndexOrThrow("room_no"));     // Room No
                    records.add(record);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DBHandler", "Error retrieving records: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return records;
    }

    // Helper method to ensure time is formatted as `hh:mm a`
    private String formatTimeStandard(String time) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault()); // Assume 24-hour format in database
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault()); // Desired 12-hour format
            return outputFormat.format(inputFormat.parse(time));
        } catch (ParseException e) {
            Log.e("DBHandler", "Error parsing time: " + time, e);
            return time; // Return original if parsing fails
        }
    }







    public String[] getFirstStudentIdAndName() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String[] result = new String[2]; // Array to hold student_id and name

        try {
            // Query to get the first student record
            cursor = db.rawQuery("SELECT student_id, name FROM student_data LIMIT 1", null);

            // Check if a record is found
            if (cursor != null && cursor.moveToFirst()) {
                result[0] = cursor.getString(cursor.getColumnIndexOrThrow("student_id")); // Student ID
                result[1] = cursor.getString(cursor.getColumnIndexOrThrow("name"));       // Name
            } else {
                result[0] = "N/A"; // Default value if no student found
                result[1] = "N/A";
            }
        } catch (Exception e) {
            Log.e("DBHandler", "Error retrieving first student ID and Name: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close(); // Close cursor to prevent memory leaks
            db.close(); // Close the database connection
        }

        return result; // Return the first student's ID and Name
    }

    public List<String[]> getTodayClasses() {
        List<String[]> todayClasses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Get the current day (e.g., "Monday", "Tuesday")
            String currentDay = new SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(new Date());

            // Query to get today's classes
            String query = "SELECT subject, day, time_from, time_to, room_no FROM student_data WHERE day = ?";
            cursor = db.rawQuery(query, new String[]{currentDay});

            if (cursor.moveToFirst()) {
                do {
                    String[] record = new String[5];
                    record[0] = cursor.getString(cursor.getColumnIndexOrThrow("subject"));     // Subject
                    record[1] = cursor.getString(cursor.getColumnIndexOrThrow("day"));         // Day
                    record[2] = cursor.getString(cursor.getColumnIndexOrThrow("time_from"));   // Time From
                    record[3] = cursor.getString(cursor.getColumnIndexOrThrow("time_to"));     // Time To
                    record[4] = cursor.getString(cursor.getColumnIndexOrThrow("room_no"));     // Room No
                    todayClasses.add(record);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DBHandler", "Error retrieving today's classes: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return todayClasses;
    }

    public void deleteAllRecords() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("DELETE FROM student_data");
            // Show success message
            if (context != null) {
                Toast.makeText(context.getApplicationContext(), "All records deleted successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("DBHandler", "Error deleting records: " + e.getMessage());
            if (context != null) {
                Toast.makeText(context.getApplicationContext(), "Error deleting records: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } finally {
            db.close();
        }
    }


}
