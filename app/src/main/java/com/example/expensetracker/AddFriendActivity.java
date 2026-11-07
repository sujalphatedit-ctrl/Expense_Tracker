package com.example.expensetracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddFriendActivity extends AppCompatActivity {

    EditText etAmount, etFriendName;
    RadioGroup rgAccount;
    Button btnAddFriendMoney;

    DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        etAmount = findViewById(R.id.etAmount);
        etFriendName = findViewById(R.id.etFriendName);
        rgAccount = findViewById(R.id.rgAccount);
        btnAddFriendMoney = findViewById(R.id.btnAddFriendMoney);

        db = new DBHelper(this);

        btnAddFriendMoney.setOnClickListener(v -> addFriendMoney());
    }

    private void addFriendMoney() {

        String amountStr = etAmount.getText().toString().trim();
        String friendName = etFriendName.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Enter amount");
            return;
        }

        int amount = Integer.parseInt(amountStr);

        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
            return;
        }

        if (TextUtils.isEmpty(friendName)) {
            etFriendName.setError("Enter friend name");
            return;
        }

        int selectedId = rgAccount.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this,
                    "Select account",
                    Toast.LENGTH_SHORT).show();
            return;
        }

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

        db.insert(
                "FRIEND",
                account,
                amount,
                friendName,
                date,
                "PENDING"
        );

        Toast.makeText(this,
                "Money sent to " + friendName,
                Toast.LENGTH_SHORT).show();

        finish();
    }
}