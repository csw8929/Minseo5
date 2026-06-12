package com.example.minseo5.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minseo5.R;
import com.example.minseo5.util.SmsParser;
import com.google.android.material.card.MaterialCardView;

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
        String purpose = r.purpose != null && !r.purpose.isEmpty() ? r.purpose : "(용도 없음)";
        holder.tvPurpose.setText(r.duplicate ? "[중복] " + purpose : purpose);
        String dateStr = r.usedTime != null ? r.usedDate + " " + r.usedTime : r.usedDate;
        holder.tvDate.setText(dateStr);
        holder.tvAmount.setText(String.format(Locale.KOREA, "%,d원", r.amount));
        if (r.duplicate) {
            holder.card.setCardBackgroundColor(Color.parseColor("#FFCDD2"));
        } else {
            holder.card.setCardBackgroundColor(holder.defaultCardColor);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvPurpose, tvDate, tvAmount;
        final MaterialCardView card;
        final ColorStateList defaultCardColor;

        VH(@NonNull View v) {
            super(v);
            card = (MaterialCardView) v;
            defaultCardColor = card.getCardBackgroundColor();
            tvPurpose = v.findViewById(R.id.tv_purpose);
            tvDate = v.findViewById(R.id.tv_date);
            tvAmount = v.findViewById(R.id.tv_amount);
        }
    }
}
