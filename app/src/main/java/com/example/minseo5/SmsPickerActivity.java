package com.example.minseo5;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minseo5.db.SpendingDao;
import com.example.minseo5.db.SpendingDatabase;
import com.example.minseo5.db.SpendingRecord;
import com.example.minseo5.ui.ParseResultAdapter;
import com.example.minseo5.util.SmsParser;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsPickerActivity extends AppCompatActivity {

    public static final String EXTRA_RAW_TEXT = "raw_text";
    public static final String EXTRA_FROM_SHARE = "from_share";
    public static final String RESULT_DATE = "result_date";

    // "추가 입력"으로 문자앱에 다녀오는 동안 보관할 기존 내용. 다음 공유 시 앞에 이어붙임.
    public static String pendingPrepend;

    private EditText etRaw;
    private TextView tvResultCount, tvTotals, tvUniqueTotal, tvUnparsed;
    private RecyclerView rvResults;
    private Button btnApply;
    private ParseResultAdapter adapter;
    private SpendingDao dao;
    private List<SmsParser.ParseResult> results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_picker);

        dao = SpendingDatabase.getInstance(this).spendingDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        etRaw = findViewById(R.id.et_raw);
        tvResultCount = findViewById(R.id.tv_result_count);
        tvTotals = findViewById(R.id.tv_totals);
        tvUniqueTotal = findViewById(R.id.tv_unique_total);
        tvUnparsed = findViewById(R.id.tv_unparsed);
        rvResults = findViewById(R.id.rv_results);
        btnApply = findViewById(R.id.btn_apply);
        Button btnAddMore = findViewById(R.id.btn_add_more);
        if (getIntent().getBooleanExtra(EXTRA_FROM_SHARE, false)) {
            btnAddMore.setVisibility(View.VISIBLE);
            btnAddMore.setOnClickListener(v -> onAddMore());
        }

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ParseResultAdapter();
        rvResults.setAdapter(adapter);

        View root = findViewById(R.id.sms_root);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets wi = getWindowManager().getCurrentWindowMetrics().getWindowInsets();
            int top = wi.getInsets(WindowInsets.Type.statusBars()).top;
            int bottom = wi.getInsets(WindowInsets.Type.navigationBars()).bottom;
            toolbar.setPadding(0, top, 0, 0);
            root.setPadding(0, 0, 0, bottom);
        }

        findViewById(R.id.btn_parse).setOnClickListener(v -> onParse());
        btnApply.setOnClickListener(v -> onApply());

        String raw = getIntent().getStringExtra(EXTRA_RAW_TEXT);
        if (raw != null) {
            etRaw.setText(raw);
        }
    }

    private void onAddMore() {
        pendingPrepend = etRaw.getText().toString();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            Toast.makeText(this, "메시지를 공유하면 기존 내용에 이어집니다", Toast.LENGTH_LONG).show();
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "메시지 앱을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            pendingPrepend = null;
        }
    }

    private void onParse() {
        String text = etRaw.getText().toString();
        if (text.trim().isEmpty()) {
            Toast.makeText(this, "문자 내용을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        results = SmsParser.parseAll(this, text);
        List<String> unparsed = SmsParser.findUnparsed(this, text);
        showUnparsed(unparsed);

        if (results.isEmpty()) {
            tvResultCount.setText("파싱 결과 없음 (룰과 일치하는 내용 없음)");
            tvResultCount.setVisibility(View.VISIBLE);
            tvTotals.setVisibility(View.GONE);
            tvUniqueTotal.setVisibility(View.GONE);
            rvResults.setVisibility(View.GONE);
            btnApply.setVisibility(View.GONE);
            return;
        }

        long total = 0, uniqueTotal = 0;
        int dupCount = 0;
        java.util.HashSet<String> batchKeys = new java.util.HashSet<>();
        for (SmsParser.ParseResult r : results) {
            total += r.amount;
            String purpose = r.purpose != null ? r.purpose : "";
            String key = r.usedDate + "|" + (r.usedTime != null ? r.usedTime : "")
                    + "|" + r.amount + "|" + purpose;
            boolean dbDup = dao.countDup(r.usedDate, r.usedTime, r.amount, purpose) > 0;
            boolean batchDup = !batchKeys.add(key);
            r.duplicate = dbDup || batchDup;
            if (r.duplicate) dupCount++;
            else uniqueTotal += r.amount;
        }

        adapter.setItems(results);
        tvResultCount.setText("파싱 결과 " + results.size() + "건 (중복 " + dupCount + "건)");
        tvTotals.setText(String.format(Locale.KOREA, "합계: %,d원", total));
        tvUniqueTotal.setText(String.format(Locale.KOREA, "중복 제외 최종: %,d원", uniqueTotal));
        tvResultCount.setVisibility(View.VISIBLE);
        tvTotals.setVisibility(View.VISIBLE);
        tvUniqueTotal.setVisibility(View.VISIBLE);
        rvResults.setVisibility(View.VISIBLE);
        btnApply.setVisibility(View.VISIBLE);
    }

    private void showUnparsed(List<String> unparsed) {
        if (unparsed.isEmpty()) {
            tvUnparsed.setVisibility(View.GONE);
            return;
        }
        StringBuilder sb = new StringBuilder("⚠ 파싱 안 된 항목 " + unparsed.size() + "건:");
        for (String u : unparsed) {
            sb.append("\n• ").append(u.replace("\n", " "));
        }
        tvUnparsed.setText(sb.toString());
        tvUnparsed.setVisibility(View.VISIBLE);
    }

    private void onApply() {
        if (results == null || results.isEmpty()) return;

        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA).format(new Date());
        int count = 0, skipped = 0;
        String latestDate = null;
        for (SmsParser.ParseResult r : results) {
            if (r.amount <= 0 || r.usedDate == null) continue;
            if (r.duplicate) {
                skipped++;
                continue;
            }
            SpendingRecord record = new SpendingRecord();
            record.usedDate = r.usedDate;
            record.usedTime = r.usedTime;
            record.amount = r.amount;
            record.purpose = r.purpose != null ? r.purpose : "";
            record.entryDate = now;
            dao.insert(record);
            count++;
            if (latestDate == null || r.usedDate.compareTo(latestDate) > 0) latestDate = r.usedDate;
        }

        Toast.makeText(this, count + "건 적용됨" + (skipped > 0 ? " (중복 " + skipped + "건 제외)" : ""),
                Toast.LENGTH_SHORT).show();
        if (latestDate != null) {
            Intent result = new Intent();
            result.putExtra(RESULT_DATE, latestDate);
            setResult(RESULT_OK, result);
        }
        finish();
    }
}
