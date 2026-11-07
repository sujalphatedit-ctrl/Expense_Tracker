package com.example.expensetracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddMoneyActivity extends AppCompatActivity {

    EditText etAmount, etNote;
    RadioGroup rgAccount;
    Button btnAddMoney;
    DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_money);

        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        rgAccount = findViewById(R.id.rgAccount);
        btnAddMoney = findViewById(R.id.btnAddMoney);

        db = new DBHelper(this);

        btnAddMoney.setOnClickListener(v -> addMoney());
    }

    private void addMoney() {

        String amountStr = etAmount.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Enter amount");
            return;
        }

        int amount = Integer.parseInt(amountStr);

        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
            return;
        }

        int selectedId = rgAccount.getCheckedRadioButtonId();
        RadioButton rb = findViewById(selectedId);

        String account = rb.getText().toString().toUpperCase();
        String note = etNote.getText().toString().trim();

        if (note.isEmpty()) {
            note = "Money Added";
        }

        String date = new SimpleDateFormat(
                "dd-MM-yyyy HH:mm",
                Locale.getDefault()
        ).format(new Date());

        db.insert("ADD", account, amount, note, date);

        Toast.makeText(this, "Money added successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}