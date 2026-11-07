package com.example.expensetracker;

import static java.time.MonthDay.now;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "expense_tracker.db";
    private static final int DB_VERSION = 5;
    private static final String TABLE = "transactions";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE " + TABLE + "(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "type TEXT," +
                        "account TEXT," +
                        "amount INTEGER," +
                        "note TEXT," +
                        "date TEXT," +
                        "balance INTEGER DEFAULT 0," +
                        "status TEXT DEFAULT 'DONE'," +
                        "created_at TEXT" +
                        ")"
        );



    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion,
                          int newVersion) {

        if (oldVersion < 2) {
            db.execSQL(
                    "ALTER TABLE " + TABLE +
                            " ADD COLUMN balance INTEGER DEFAULT 0"
            );
        }

        if (oldVersion < 3) {
            db.execSQL(
                    "ALTER TABLE " + TABLE +
                            " ADD COLUMN status TEXT DEFAULT 'DONE'"
            );
        }

        if (oldVersion < 5) {
            db.execSQL(
                    "ALTER TABLE " + TABLE +
                            " ADD COLUMN created_at TEXT"
            );
        }
    }

    public void insert(String type,
                       String account,
                       int amount,
                       String note,
                       String date) {

        SQLiteDatabase db = getWritableDatabase();

        int currentBalance = getLastBalance(account);
        int newBalance;

        if (type.equalsIgnoreCase("ADD")) {
            newBalance = currentBalance + amount;
        } else {
            newBalance = currentBalance - amount;
        }

        String createdAt =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                ).format(new Date());

        ContentValues cv = new ContentValues();
        cv.put("type", type);
        cv.put("account", account);
        cv.put("amount", amount);
        cv.put("note", note);
        cv.put("date", date);
        cv.put("balance", newBalance);
        cv.put("status", "DONE");
        cv.put("created_at", createdAt);

        db.insert(TABLE, null, cv);
    }

    public void insert(String type,
                       String account,
                       int amount,
                       String note,
                       String date,
                       String status) {

        SQLiteDatabase db = getWritableDatabase();

        int currentBalance = getLastBalance(account);
        int newBalance;

        if (type.equalsIgnoreCase("ADD")) {
            newBalance = currentBalance + amount;
        } else {
            newBalance = currentBalance - amount;
        }

        String createdAt =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                ).format(new Date());

        ContentValues cv = new ContentValues();
        cv.put("type", type);
        cv.put("account", account);
        cv.put("amount", amount);
        cv.put("note", note);
        cv.put("date", date);
        cv.put("balance", newBalance);
        cv.put("status", status);
        cv.put("created_at", createdAt);

        db.insert(TABLE, null, cv);
    }

    public void markPaid(int id) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("status", "PAID");

        db.update(
                TABLE,
                cv,
                "id=?",
                new String[]{String.valueOf(id)}
        );
    }



    public void deleteTransaction(int id) {

        SQLiteDatabase db = getWritableDatabase();

        db.delete(
                TABLE,
                "id=?",
                new String[]{String.valueOf(id)}
        );
    }


    public int getLastBalance(String account) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT balance FROM " + TABLE +
                        " WHERE account=? " +
                        "ORDER BY id DESC LIMIT 1",
                new String[]{account}
        );

        if (cursor.moveToFirst()) {
            int balance = cursor.getInt(0);
            cursor.close();
            return balance;
        }

        cursor.close();
        return 0;
    }

    public int getBalance(String account) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor add = db.rawQuery(
                "SELECT SUM(amount) FROM " + TABLE +
                        " WHERE account=? AND type='ADD'",
                new String[]{account}
        );

        Cursor sub = db.rawQuery(
                "SELECT SUM(amount) FROM " + TABLE +
                        " WHERE account=? AND type!='ADD'",
                new String[]{account}
        );

        int added = 0;
        int deducted = 0;

        if (add.moveToFirst())
            added = add.getInt(0);

        if (sub.moveToFirst())
            deducted = sub.getInt(0);

        add.close();
        sub.close();

        return added - deducted;
    }

    public Cursor getTransactionsByDate(
            String startDate,
            String endDate) {

        SQLiteDatabase db =
                getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM " + TABLE +
                        " WHERE date >= ? AND date <= ?" +
                        " ORDER BY id DESC",
                new String[]{
                        startDate,
                        endDate
                }
        );
    }


    public Cursor getRawMonthlyTransactions(String monthYear) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT type, amount, note, status, date FROM " + TABLE + " WHERE date LIKE ?",
                new String[]{"%" + monthYear + "%"}
        );
    }

    public boolean canModify(String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) {
            return false;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(created_at) FROM " + TABLE, null);

        String newestCreatedAt = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                newestCreatedAt = cursor.getString(0);
            }
            cursor.close();
        }

        return createdAt.equals(newestCreatedAt);
    }
}