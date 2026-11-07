package com.example.expensetracker;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter
        extends RecyclerView.Adapter<TransactionAdapter.ViewHolder>
        implements Filterable {

    private final List<Transaction> transactionList;
    private final List<Transaction> transactionListFull;
    private final Context context;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactionList = new ArrayList<>(transactions);
        this.transactionListFull = new ArrayList<>(transactions);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = transactionList.get(position);
        DBHelper dbHelper = new DBHelper(context);


        holder.btnDelete.setVisibility(View.GONE);


        if (position == 0 && t.createdAt != null && dbHelper.canModify(t.createdAt)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete")
                    .setMessage("Delete this transaction?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DBHelper db = new DBHelper(context);
                        db.deleteTransaction(t.id);

                        int pos = holder.getAdapterPosition();

                        transactionListFull.remove(transactionList.get(pos));
                        transactionList.remove(pos);

                        notifyItemRemoved(pos);


                        notifyItemRangeChanged(0, transactionList.size());

                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        holder.tvType.setText(t.type);
        holder.tvAccount.setText(t.account);
        holder.tvNote.setText(t.note);
        holder.tvDate.setText(t.date);
        holder.tvBalance.setText("Balance: ₹ " + t.balance);

        String today = new SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
        ).format(new Date());


        if (t.type.equalsIgnoreCase("ADD")) {
            holder.tvAmount.setText("+ ₹ " + t.amount);
            holder.tvAmount.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            holder.tvAmount.setText("- ₹ " + t.amount);
            holder.tvAmount.setTextColor(Color.parseColor("#C62828"));
        }


        holder.btnPaid.setVisibility(View.GONE);
        holder.tvStatus.setVisibility(View.GONE);

        if (t.type.equalsIgnoreCase("FRIEND")) {
            holder.tvStatus.setVisibility(View.VISIBLE);


            if (t.status.equalsIgnoreCase("PENDING")) {
                holder.tvStatus.setText("Status: Pending");
                holder.tvStatus.setTextColor(Color.parseColor("#E65100"));

                holder.btnPaid.setVisibility(View.VISIBLE);
                holder.btnPaid.setText("PAID");
                holder.btnPaid.setEnabled(true);

                holder.btnPaid.setOnClickListener(v -> {
                    new AlertDialog.Builder(context)
                            .setTitle("Confirm")
                            .setMessage("Money received from " + t.note + "?")
                            .setPositiveButton("YES", (dialog, which) -> {
                                DBHelper db = new DBHelper(context);


                                db.markPaid(t.id);

                                String date = new SimpleDateFormat(
                                        "dd-MM-yyyy HH:mm",
                                        Locale.getDefault()
                                ).format(new Date());

                             
                                db.insert(
                                        "ADD",
                                        t.account,
                                        t.amount,
                                        t.note + " returned money",
                                        date
                                );

                                holder.tvStatus.setText("Status: Paid");
                                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                                holder.btnPaid.setVisibility(View.GONE);

                                Toast.makeText(
                                        context,
                                        "Payment marked paid",
                                        Toast.LENGTH_SHORT
                                ).show();
                            })
                            .setNegativeButton("NO", null)
                            .show();
                });

            } else {
                // Paid
                holder.tvStatus.setText("Status: Paid");
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                holder.btnPaid.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    @Override
    public Filter getFilter() {
        return transactionFilter;
    }

    private final Filter transactionFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Transaction> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(transactionListFull);
            } else {
                String filterPattern = constraint.toString()
                        .toLowerCase(Locale.getDefault())
                        .trim();

                for (Transaction t : transactionListFull) {
                    if (String.valueOf(t.amount).contains(filterPattern)
                            || t.type.toLowerCase().contains(filterPattern)
                            || t.account.toLowerCase().contains(filterPattern)
                            || t.note.toLowerCase().contains(filterPattern)
                            || t.date.toLowerCase().contains(filterPattern)
                            || t.status.toLowerCase().contains(filterPattern)) {

                        filteredList.add(t);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            transactionList.clear();
            transactionList.addAll((List<Transaction>) results.values);
            notifyDataSetChanged();
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvType, tvAccount, tvAmount, tvNote, tvDate, tvBalance, tvStatus;
        Button btnPaid;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            tvType = itemView.findViewById(R.id.tvType);
            tvAccount = itemView.findViewById(R.id.tvAccount);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnPaid = itemView.findViewById(R.id.btnPaid);
        }
    }
}