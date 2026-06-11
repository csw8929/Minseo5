package com.example.minseo5.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minseo5.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.VH> {

    public interface OnSmsClickListener {
        void onSmsClick(SmsItem item);
    }

    public static class SmsItem {
        public final String address;
        public final String body;
        public final long timestamp;

        public SmsItem(String address, String body, long timestamp) {
            this.address = address;
            this.body = body;
            this.timestamp = timestamp;
        }
    }

    private static final SimpleDateFormat FMT = new SimpleDateFormat("MM/dd HH:mm", Locale.KOREA);

    private List<SmsItem> items = new ArrayList<>();
    private final OnSmsClickListener listener;

    public SmsAdapter(OnSmsClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<SmsItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sms, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SmsItem item = items.get(position);
        holder.tvSender.setText(item.address != null ? item.address : "");
        holder.tvDate.setText(FMT.format(new Date(item.timestamp)));
        holder.tvBody.setText(item.body != null ? item.body : "");
        holder.itemView.setOnClickListener(v -> listener.onSmsClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvSender, tvDate, tvBody;

        VH(@NonNull View v) {
            super(v);
            tvSender = v.findViewById(R.id.tv_sender);
            tvDate = v.findViewById(R.id.tv_sms_date);
            tvBody = v.findViewById(R.id.tv_body);
        }
    }
}
