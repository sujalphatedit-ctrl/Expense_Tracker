package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private EditText etSearch;
    private ImageView ivDownload;

    // Sunday Summary UI Variables
    private LinearLayout llSundaySummary;
    private TextView tvWeeklyExpenses;
    private TextView tvWeeklyAdded;
    private TextView tvWeeklyPending;

    private DBHelper db;
    private List<Transaction> transactionList;
    private TransactionAdapter adapter;

    private String startDate = "";
    private String endDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rvHistory = findViewById(R.id.rvHistory);
        etSearch = findViewById(R.id.etSearch);
        ivDownload = findViewById(R.id.ivDownload);

        // Bind Sunday Summary UI components
        llSundaySummary = findViewById(R.id.llSundaySummary);
        tvWeeklyExpenses = findViewById(R.id.tvWeeklyExpenses);
        tvWeeklyAdded = findViewById(R.id.tvWeeklyAdded);
        tvWeeklyPending = findViewById(R.id.tvWeeklyPending);

        db = new DBHelper(this);
        transactionList = new ArrayList<>();

        loadTransactions();
        checkAndDisplayWeeklySummary();

        adapter = new TransactionAdapter(this, transactionList);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ivDownload.setOnClickListener(v -> pickStartDate());
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadTransactions();
        checkAndDisplayWeeklySummary();

        adapter = new TransactionAdapter(this, transactionList);
        rvHistory.setAdapter(adapter);
    }

    private void loadTransactions() {

        transactionList.clear();

        Cursor cursor = db.getReadableDatabase()
                .rawQuery("SELECT * FROM transactions ORDER BY id DESC", null);

        if (cursor.moveToFirst()) {

            do {

                transactionList.add(
                        new Transaction(
                                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                                cursor.getString(cursor.getColumnIndexOrThrow("type")),
                                cursor.getString(cursor.getColumnIndexOrThrow("account")),
                                cursor.getInt(cursor.getColumnIndexOrThrow("amount")),
                                cursor.getString(cursor.getColumnIndexOrThrow("note")),
                                cursor.getString(cursor.getColumnIndexOrThrow("date")),
                                cursor.getInt(cursor.getColumnIndexOrThrow("balance")),
                                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                                cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                        )
                );

            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    // Calculate and reveal past 7 days summary metrics if today is Sunday
    private void checkAndDisplayWeeklySummary() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.SUNDAY) {
            int totalExpenses = 0;
            int totalAdded = 0;
            int totalPendingToFriends = 0;

            // Queries past 7 days from the transaction database schema
            Cursor cursor = db.getReadableDatabase().rawQuery(
                    "SELECT type, status, amount FROM transactions WHERE date >= date('now', '-7 days')", null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int typeIdx = cursor.getColumnIndexOrThrow("type");
                    int statusIdx = cursor.getColumnIndexOrThrow("status");
                    int amountIdx = cursor.getColumnIndexOrThrow("amount");

                    do {
                        String type = cursor.getString(typeIdx).toLowerCase();
                        String status = cursor.getString(statusIdx).toLowerCase();
                        int amount = cursor.getInt(amountIdx);

                        // 1. Accumulate Total Expenses
                        if (type.equals("expense")) {
                            totalExpenses += amount;
                        }
                        // 2. Accumulate Total Added Money
                        else if (type.equals("income") || type.equals("added")) {
                            totalAdded += amount;
                        }

                        // 3. Accumulate Pending Dues to Friends
                        if (status.equals("pending")) {
                            totalPendingToFriends += amount;
                        }

                    } while (cursor.moveToNext());
                }
                cursor.close();
            }

            // Update UI elements and toggle summary panel visibility
            tvWeeklyExpenses.setText("Total Expenses: " + totalExpenses);
            tvWeeklyAdded.setText("Total Added Money: " + totalAdded);
            tvWeeklyPending.setText("Pending to Friends: " + totalPendingToFriends);
            llSundaySummary.setVisibility(View.VISIBLE);
        } else {
            llSundaySummary.setVisibility(View.GONE);
        }
    }

    // START DATE
    private void pickStartDate() {

        Calendar c = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {

                    startDate = String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            day,
                            month + 1,
                            year
                    );

                    pickEndDate();

                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );

        dialog.setTitle("Select Start Date");
        dialog.show();
    }

    // END DATE
    private void pickEndDate() {

        Calendar c = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {

                    endDate = String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            day,
                            month + 1,
                            year
                    );

                    downloadHistory();

                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );

        dialog.setTitle("Select End Date");
        dialog.show();
    }

    // SAVE IN PHONE STORAGE / DOWNLOADS
    private void downloadHistory() {

        try {

            String fileName =
                    "History_" + System.currentTimeMillis() + ".csv";

            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                ContentValues values = new ContentValues();
                values.put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        fileName
                );
                values.put(
                        MediaStore.Downloads.MIME_TYPE,
                        "text/csv"
                );
                values.put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                );

                Uri uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                );

                outputStream =
                        getContentResolver().openOutputStream(uri);

            } else {

                File file = new File(
                        Environment
                                .getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOWNLOADS
                                ),
                        fileName
                );

                outputStream = new FileOutputStream(file);
            }

            StringBuilder builder = new StringBuilder();

            builder.append(
                    "Type,Account,Amount,Note,Date,Balance,Status\n"
            );

            Cursor cursor =
                    db.getTransactionsByDate(startDate, endDate);

            if (cursor.moveToFirst()) {

                do {

                    builder.append(
                            cursor.getString(
                                    cursor.getColumnIndexOrThrow("type")
                            )
                    ).append(",");

                    builder.append(
                            cursor.getString(
                                    cursor.getColumnIndexOrThrow("account")
                            )
                    ).append(",");

                    builder.append(
                            cursor.getInt(
                                    cursor.getColumnIndexOrThrow("amount")
                            )
                    ).append(",");

                    builder.append(
                            cursor.getString(
                                    cursor.getColumnIndexOrThrow("note")
                            )
                    ).append(",");

                    builder.append(
                            cursor.getString(
                                    cursor.getColumnIndexOrThrow("date")
                            )
                    ).append(",");

                    builder.append(
                            cursor.getInt(
                                    cursor.getColumnIndexOrThrow("balance")
                            )
                    ).append(",");

                    builder.append(
                            cursor.getString(
                                    cursor.getColumnIndexOrThrow("status")
                            )
                    ).append("\n");

                } while (cursor.moveToNext());
            }

            cursor.close();

            outputStream.write(
                    builder.toString().getBytes()
            );

            outputStream.flush();
            outputStream.close();

            Toast.makeText(
                    this,
                    "Saved in Downloads Folder",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Save Failed",
                    Toast.LENGTH_LONG
            ).show();

            e.printStackTrace();
        }
    }
}