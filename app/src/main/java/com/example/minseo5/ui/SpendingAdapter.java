package com.example.minseo5.ui;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minseo5.R;
import com.example.minseo5.db.SpendingRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SpendingAdapter extends RecyclerView.Adapter<SpendingAdapter.VH> {

    public interface Listener {
        void onItemClick(SpendingRecord record);

        void onItemLongClick(SpendingRecord record);
    }

    private static final int SELECTED_COLOR = Color.parseColor("#E3F2FD");

    private List<SpendingRecord> items = new ArrayList<>();
    private final Listener listener;
    private boolean selectionMode = false;
    private final Set<Integer> selectedIds = new HashSet<>();

    public SpendingAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<SpendingRecord> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(boolean mode) {
        this.selectionMode = mode;
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public void select(SpendingRecord record) {
        selectedIds.add(record.id);
        notifyDataSetChanged();
    }

    public void toggle(SpendingRecord record) {
        if (!selectedIds.add(record.id)) selectedIds.remove(record.id);
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public List<SpendingRecord> getSelectedRecords() {
        List<SpendingRecord> out = new ArrayList<>();
        for (SpendingRecord r : items) {
            if (selectedIds.contains(r.id)) out.add(r);
        }
        return out;
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
        String date = r.usedDate != null ? r.usedDate : "";
        if (r.usedTime != null) date += " " + r.usedTime;
        holder.tvDate.setText(date);
        holder.tvAmount.setText(String.format(Locale.KOREA, "%,d원", r.amount));
        holder.tvPurpose.setText(r.purpose != null ? r.purpose : "");

        if (selectionMode && selectedIds.contains(r.id)) {
            holder.itemView.setBackgroundColor(SELECTED_COLOR);
        } else {
            TypedValue tv = new TypedValue();
            holder.itemView.getContext().getTheme()
                    .resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            holder.itemView.setBackgroundResource(tv.resourceId);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(r));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(r);
            return true;
        });
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
