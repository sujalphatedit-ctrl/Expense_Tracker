package com.example.expensetracker;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthlyAnalysis extends AppCompatActivity {

    private Spinner spMonthPicker;
    private TextView tvTotalIncome, tvTotalExpense, tvDailyAverage;
    private TextView tvLentToFriends, tvReturnedByFriends, tvPendingFriends;
    private TextView tvSavingsRate, tvHighestTransaction, tvFrequentTransaction;

    // Month-over-Month Side-by-Side Graph Views
    private View vIncBar1, vIncBar2, vIncBar3;
    private View vExpBar1, vExpBar2, vExpBar3;
    private TextView tvIncGraphAmt1, tvIncGraphAmt2, tvIncGraphAmt3;
    private TextView tvExpGraphAmt1, tvExpGraphAmt2, tvExpGraphAmt3;
    private TextView tvIncGraphLabel1, tvIncGraphLabel2, tvIncGraphLabel3;

    private DBHelper dbHelper;
    private List<String> monthList;
    private List<String> queryFilterList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_analysis);

        dbHelper = new DBHelper(this);

        // Bind Base Metric Views
        spMonthPicker = findViewById(R.id.spMonthPicker);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvDailyAverage = findViewById(R.id.tvDailyAverage);
        tvLentToFriends = findViewById(R.id.tvLentToFriends);
        tvReturnedByFriends = findViewById(R.id.tvReturnedByFriends);
        tvPendingFriends = findViewById(R.id.tvPendingFriends);
        tvSavingsRate = findViewById(R.id.tvSavingsRate); // Will now hold the direct total savings amount string
        tvHighestTransaction = findViewById(R.id.tvHighestTransaction);
        tvFrequentTransaction = findViewById(R.id.tvFrequentTransaction);

        // Bind New Side-by-Side Graph Views
        vIncBar1 = findViewById(R.id.vIncBar1);
        vIncBar2 = findViewById(R.id.vIncBar2);
        vIncBar3 = findViewById(R.id.vIncBar3);

        vExpBar1 = findViewById(R.id.vExpBar1);
        vExpBar2 = findViewById(R.id.vExpBar2);
        vExpBar3 = findViewById(R.id.vExpBar3);

        tvIncGraphAmt1 = findViewById(R.id.tvIncGraphAmt1);
        tvIncGraphAmt2 = findViewById(R.id.tvIncGraphAmt2);
        tvIncGraphAmt3 = findViewById(R.id.tvIncGraphAmt3);

        tvExpGraphAmt1 = findViewById(R.id.tvExpGraphAmt1);
        tvExpGraphAmt2 = findViewById(R.id.tvExpGraphAmt2);
        tvExpGraphAmt3 = findViewById(R.id.tvExpGraphAmt3);

        tvIncGraphLabel1 = findViewById(R.id.tvIncGraphLabel1);
        tvIncGraphLabel2 = findViewById(R.id.tvIncGraphLabel2);
        tvIncGraphLabel3 = findViewById(R.id.tvIncGraphLabel3);

        setupMonthSpinner();
    }

    private void setupMonthSpinner() {

        monthList = new ArrayList<>();
        queryFilterList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        SimpleDateFormat queryFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());

        for (int i = 0; i < 6; i++) {
            monthList.add(displayFormat.format(cal.getTime()));
            queryFilterList.add(queryFormat.format(cal.getTime()));
            cal.add(Calendar.MONTH, -1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, monthList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMonthPicker.setAdapter(adapter);

        spMonthPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
                updateAnalysisDashboard(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateAnalysisDashboard(int selectedIndex) {
        String currentMonthTarget = queryFilterList.get(selectedIndex);

        // 1. Calculate historical trend metrics (Both Income and Expense sets)
        double[] m1Data = getMonthFinancials(currentMonthTarget);
        double[] m2Data = (selectedIndex + 1 < queryFilterList.size()) ? getMonthFinancials(queryFilterList.get(selectedIndex + 1)) : new double[]{0, 0};
        double[] m3Data = (selectedIndex + 2 < queryFilterList.size()) ? getMonthFinancials(queryFilterList.get(selectedIndex + 2)) : new double[]{0, 0};

        renderDualTrendGraph(m1Data, m2Data, m3Data, selectedIndex);

        // 2. Compute advanced tracking breakdowns for the main dashboard metrics
        double totalMoneyAdded = m1Data[0];
        double totalExpenses = 0;
        double totalSentToFriends = 0;
        double totalReturnedBack = 0;
        double totalStillOwed = 0;
        double targetedSavingsAccountTotal = 0;

        double highestAmount = 0;
        String highestTxDetails = "None";
        HashMap<Integer, Integer> amountMap = new HashMap<>();
        int distinctDaysCount = 1;
        List<Integer> parsedDays = new ArrayList<>();

        Cursor cursor = dbHelper.getRawMonthlyTransactions(currentMonthTarget);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String type = cursor.getString(0);
                double amount = cursor.getString(1) != null ? cursor.getDouble(1) : 0;
                String note = cursor.getString(2) != null ? cursor.getString(2) : "Uncategorized";
                String status = cursor.getString(3) != null ? cursor.getString(3) : "";
                String rawDate = cursor.getString(4) != null ? cursor.getString(4) : "";

                // Safe fallback evaluation logic for schema implementations targeting savings-specific variables
                String accountIdentifier = "";
                if (cursor.getColumnCount() > 5) {
                    accountIdentifier = cursor.getString(5) != null ? cursor.getString(5) : "";
                }

                // Calculate distinct active spending days safely
                try {
                    if (rawDate.length() >= 2) {
                        int dayVal = Integer.parseInt(rawDate.substring(0, 2));
                        if (!parsedDays.contains(dayVal)) {
                            parsedDays.add(dayVal);
                        }
                    }
                } catch (Exception ignored) {}

                if (type.equalsIgnoreCase("ADD")) {
                    // Check if entry target maps as direct additions toward saving / savings targets
                    String noteLower = note.toLowerCase();
                    String accountLower = accountIdentifier.toLowerCase();
                    if (noteLower.contains("saving") || accountLower.contains("saving")) {
                        targetedSavingsAccountTotal += amount;
                    }

                    if (noteLower.contains("returned money")) {
                        totalReturnedBack += amount;
                    }
                } else if (type.equalsIgnoreCase("FRIEND")) {
                    totalSentToFriends += amount;
                    if (!status.equalsIgnoreCase("PAID")) {
                        totalStillOwed += amount;
                    }
                } else {
                    totalExpenses += amount;
                }

                if (amount > highestAmount) {
                    highestAmount = amount;
                    highestTxDetails = String.format(Locale.getDefault(), "₹ %.2f (%s - %s)", amount, type, note);
                }

                int amt = (int) amount;

                int count = amountMap.containsKey(amt)
                        ? amountMap.get(amt)
                        : 0;

                amountMap.put(amt, count + 1);
            }
            cursor.close();
        }

        distinctDaysCount = Math.max(1, parsedDays.size());
        double dailyPace = totalExpenses / distinctDaysCount;

        // Set metrics to views
        tvTotalIncome.setText("₹ " + String.format(Locale.getDefault(), "%.2f", totalMoneyAdded));
        tvTotalExpense.setText("₹ " + String.format(Locale.getDefault(), "%.2f", totalExpenses));
        tvDailyAverage.setText(String.format(Locale.getDefault(), "₹ %.2f / active day", dailyPace));
        tvLentToFriends.setText("₹ " + String.format(Locale.getDefault(), "%.2f", totalSentToFriends));
        tvReturnedByFriends.setText("₹ " + String.format(Locale.getDefault(), "%.2f", totalReturnedBack));
        tvPendingFriends.setText("₹ " + String.format(Locale.getDefault(), "%.2f", totalStillOwed));

        // Displays the direct absolute amount of processed savings entries safely
        tvSavingsRate.setText("₹ " + String.format(Locale.getDefault(), "%.2f", targetedSavingsAccountTotal));
        tvHighestTransaction.setText(highestTxDetails);

        int mostFrequentAmount = 0;
        int maxFrequency = 0;

        for (Map.Entry<Integer, Integer> entry : amountMap.entrySet()) {

            if (entry.getValue() > maxFrequency) {

                maxFrequency = entry.getValue();
                mostFrequentAmount = entry.getKey();
            }
        }

        String frequentDetails;

        if (maxFrequency > 1) {

            frequentDetails =
                    "₹ " +
                            mostFrequentAmount +
                            " (" +
                            maxFrequency +
                            " times)";

        } else if (highestAmount > 0) {

            frequentDetails =
                    "Highest Amount ₹ " +
                            String.format(
                                    Locale.getDefault(),
                                    "%.0f",
                                    highestAmount
                            );

        } else {

            frequentDetails = "No Transactions";
        }

        tvFrequentTransaction.setText(frequentDetails);
        }

    private double[] getMonthFinancials(String monthYear) {
        double income = 0;
        double outflow = 0;
        Cursor cursor = dbHelper.getRawMonthlyTransactions(monthYear);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String type = cursor.getString(0);
                double amount = cursor.getString(1) != null ? cursor.getDouble(1) : 0;
                if (type.equalsIgnoreCase("ADD")) {
                    income += amount;
                } else if (type.equalsIgnoreCase("EXPENSE") || type.equalsIgnoreCase("FRIEND")) {
                    outflow += amount;
                }
            }
            cursor.close();
        }
        return new double[]{income, outflow};
    }

    private void renderDualTrendGraph(double[] m1, double[] m2, double[] m3, int selectedIndex) {
        double maxAmt = Math.max(m1[0], Math.max(m1[1], Math.max(m2[0], Math.max(m2[1], Math.max(m3[0], m3[1])))));
        if (maxAmt == 0) maxAmt = 1;

        int maxPixelHeight = dpToPx(80);

        setBarHeight(vIncBar1, (int) ((m1[0] / maxAmt) * maxPixelHeight));
        setBarHeight(vExpBar1, (int) ((m1[1] / maxAmt) * maxPixelHeight));

        setBarHeight(vIncBar2, (int) ((m2[0] / maxAmt) * maxPixelHeight));
        setBarHeight(vExpBar2, (int) ((m2[1] / maxAmt) * maxPixelHeight));

        setBarHeight(vIncBar3, (int) ((m3[0] / maxAmt) * maxPixelHeight));
        setBarHeight(vExpBar3, (int) ((m3[1] / maxAmt) * maxPixelHeight));

        tvIncGraphAmt1.setText("₹" + (int) m1[0]);
        tvExpGraphAmt1.setText("₹" + (int) m1[1]);

        tvIncGraphAmt2.setText("₹" + (int) m2[0]);
        tvExpGraphAmt2.setText("₹" + (int) m2[1]);

        tvIncGraphAmt3.setText("₹" + (int) m3[0]);
        tvExpGraphAmt3.setText("₹" + (int) m3[1]);

        tvIncGraphLabel1.setText(getShortMonthLabel(selectedIndex));
        tvIncGraphLabel2.setText(getShortMonthLabel(selectedIndex + 1));
        tvIncGraphLabel3.setText(getShortMonthLabel(selectedIndex + 2));
    }

    private String getShortMonthLabel(int index) {
        if (index >= queryFilterList.size()) return "-";
        String filter = queryFilterList.get(index);
        String[] parts = filter.split("-");
        String[] monthsShort = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        try {
            int monthIdx = Integer.parseInt(parts[0]) - 1;
            return monthsShort[monthIdx] + " '" + parts[1].substring(2);
        } catch (Exception e) {
            return filter;
        }
    }

    private void setBarHeight(View bar, int heightPx) {
        ViewGroup.LayoutParams params = bar.getLayoutParams();
        params.height = Math.max(dpToPx(4), heightPx);
        bar.setLayoutParams(params);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}