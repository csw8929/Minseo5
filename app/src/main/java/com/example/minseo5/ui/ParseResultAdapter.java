package com.example.minseo5.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minseo5.R;
import com.example.minseo5.util.SmsParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ParseResultAdapter extends RecyclerView.Adapter<ParseResultAdapter.VH> {

    private List<SmsParser.ParseResult> items = new ArrayList<>();

    public void setItems(List<SmsParser.ParseResult> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parse_result, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SmsParser.ParseResult r = items.get(position);
        holder.tvPurpose.setText(r.purpose != null && !r.purpose.isEmpty() ? r.purpose : "(용도 없음)");
        holder.tvDate.setText(r.usedDate);
        holder.tvAmount.setText(String.format(Locale.KOREA, "%,d원", r.amount));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvPurpose, tvDate, tvAmount;

        VH(@NonNull View v) {
            super(v);
            tvPurpose = v.findViewById(R.id.tv_purpose);
            tvDate = v.findViewById(R.id.tv_date);
            tvAmount = v.findViewById(R.id.tv_amount);
        }
    }
}
