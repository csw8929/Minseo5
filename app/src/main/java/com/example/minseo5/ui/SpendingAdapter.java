package com.example.minseo5.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minseo5.R;
import com.example.minseo5.db.SpendingRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpendingAdapter extends RecyclerView.Adapter<SpendingAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(SpendingRecord record);
    }

    private List<SpendingRecord> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public SpendingAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<SpendingRecord> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_spending, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SpendingRecord r = items.get(position);
        holder.tvDate.setText(r.usedDate);
        holder.tvAmount.setText(String.format(Locale.KOREA, "%,d원", r.amount));
        holder.tvPurpose.setText(r.purpose != null ? r.purpose : "");
        holder.itemView.setOnClickListener(v -> listener.onItemClick(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvDate, tvAmount, tvPurpose;

        VH(@NonNull View v) {
            super(v);
            tvDate = v.findViewById(R.id.tv_date);
            tvAmount = v.findViewById(R.id.tv_amount);
            tvPurpose = v.findViewById(R.id.tv_purpose);
        }
    }
}
