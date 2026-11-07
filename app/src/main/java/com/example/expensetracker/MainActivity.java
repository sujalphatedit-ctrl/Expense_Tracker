package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    TextView tvPocketBalance, tvSavingBalance, tvCashBalance;
    ImageView ivEye;
    DBHelper db;

    boolean isVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvPocketBalance = findViewById(R.id.tvPocketBalance);
        tvSavingBalance = findViewById(R.id.tvSavingBalance);
        tvCashBalance = findViewById(R.id.tvCashBalance);
        ivEye = findViewById(R.id.ivEye);

        Button btnAddMoney = findViewById(R.id.btnAddMoney);
        Button btnAddExpense = findViewById(R.id.btnAddExpense);
        Button btnAddFriend = findViewById(R.id.btnAddFriend);
        Button btnHistory = findViewById(R.id.btnHistory);
        Button btnMonthlyAnalysis = findViewById(R.id.btnMonthlyAnalysis);
        db = new DBHelper(this);

        btnAddMoney.setOnClickListener(v ->
                startActivity(new Intent(this, AddMoneyActivity.class)));

        btnAddExpense.setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));

        btnAddFriend.setOnClickListener(v ->
                startActivity(new Intent(this, AddFriendActivity.class)));

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        btnMonthlyAnalysis.setOnClickListener(view ->
                startActivity(new Intent(this, MonthlyAnalysis.class)));

        ivEye.setOnClickListener(v -> {
            if (isVisible) {
                hideBalance();
            } else {
                showBalance();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBalance();
    }

    private void hideBalance() {
        tvPocketBalance.setText("₹ ****");
        tvSavingBalance.setText("₹ ****");
        tvCashBalance.setText("₹ ****");
        ivEye.setImageResource(android.R.drawable.ic_menu_view);
        isVisible = false;
    }

    private void showBalance() {
        int pocket = db.getBalance("POCKET");
        int saving = db.getBalance("SAVING");
        int cash = db.getBalance("CASH");

        tvPocketBalance.setText("₹ " + pocket);
        tvSavingBalance.setText("₹ " + saving);
        tvCashBalance.setText("₹ " + cash);

        ivEye.setImageResource(android.R.drawable.presence_online);
        isVisible = true;
    }
}