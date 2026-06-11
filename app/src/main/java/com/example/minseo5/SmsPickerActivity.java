package com.example.minseo5;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minseo5.db.SpendingDao;
import com.example.minseo5.db.SpendingDatabase;
import com.example.minseo5.ui.SmsAdapter;
import com.example.minseo5.ui.SpendingEntryDialog;
import com.example.minseo5.util.SmsParser;

import java.util.ArrayList;
import java.util.List;

public class SmsPickerActivity extends AppCompatActivity {

    private SmsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_picker);

        RecyclerView rv = findViewById(R.id.rv_sms);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SmsAdapter(this::onSmsSelected);
        rv.setAdapter(adapter);

        loadSms();
    }

    private void loadSms() {
        List<SmsAdapter.SmsItem> items = new ArrayList<>();
        Uri uri = Uri.parse("content://sms/inbox");
        String[] projection = {"address", "body", "date"};
        try (Cursor cursor = getContentResolver().query(
                uri, projection, null, null, "date DESC LIMIT 200")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(0);
                    String body = cursor.getString(1);
                    long date = cursor.getLong(2);
                    items.add(new SmsAdapter.SmsItem(address, body, date));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "SMS 읽기 실패", Toast.LENGTH_SHORT).show();
        }
        adapter.setItems(items);
    }

    private void onSmsSelected(SmsAdapter.SmsItem item) {
        SmsParser.ParseResult parsed = SmsParser.parse(item.address, item.body, item.timestamp);

        SpendingEntryDialog dialog = SpendingEntryDialog.newAddInstance(
                parsed.usedDate, parsed.amount, parsed.purpose);
        dialog.setOnSaveListener(() -> {
            Toast.makeText(this, "저장됨", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "entry");
    }
}
