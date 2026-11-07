package com.example.expensetracker;

public class Transaction {

    public int id;
    public String type;
    public String account;
    public int amount;
    public String note;
    public String date;
    public int balance;
    public String status;
    public String createdAt;

    public Transaction(
            int id,
            String type,
            String account,
            int amount,
            String note,
            String date,
            int balance,
            String status,
            String createdAt) {

        this.id = id;
        this.type = type;
        this.account = account;
        this.amount = amount;
        this.note = note;
        this.date = date;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
    }
}