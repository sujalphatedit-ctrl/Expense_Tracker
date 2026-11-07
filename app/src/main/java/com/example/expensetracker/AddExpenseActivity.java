package com.example.expensetracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {

    EditText etAmount, etReason;
    RadioGroup rgAccount;
    Button btnAddExpense;
    DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        etAmount = findViewById(R.id.etAmount);
        etReason = findViewById(R.id.etReason);
        rgAccount = findViewById(R.id.rgAccount);
        btnAddExpense = findViewById(R.id.btnAddExpense);

        db = new DBHelper(this);

        btnAddExpense.setOnClickListener(v -> addExpense());
    }

    private void addExpense() {

        String amountStr = etAmount.getText().toString().trim();
        String reason = etReason.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Enter amount");
            return;
        }

        int amount = Integer.parseInt(amountStr);

        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
            return;
        }

        if (TextUtils.isEmpty(reason)) {
            etReason.setError("Enter expense reason");
            return;
        }

        int selectedId = rgAccount.getCheckedRadioButtonId();
        RadioButton rb = findViewById(selectedId);

        String account = rb.getText().toString().toUpperCase();

        int currentBalance = db.getBalance(account);

        if (amount > currentBalance) {
            Toast.makeText(this,
                    "Insufficient balance in " + account,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat(
                "dd-MM-yyyy HH:mm",
                Locale.getDefault()
        ).format(new Date());

        db.insert("EXPENSE", account, amount, reason, date);

        Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}