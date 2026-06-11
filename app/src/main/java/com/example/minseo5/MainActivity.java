package com.example.minseo5;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minseo5.db.SpendingDao;
import com.example.minseo5.db.SpendingDatabase;
import com.example.minseo5.db.SpendingRecord;
import com.example.minseo5.ui.SpendingAdapter;
import com.example.minseo5.ui.SpendingEntryDialog;
import com.example.minseo5.util.JsonExportImport;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SpendingDao dao;
    private SpendingAdapter adapter;
    private Calendar currentMonth;
    private TextView tvMonth, tvTotal;

    private final ActivityResultLauncher<String> smsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startSmsPickerActivity();
                } else {
                    Toast.makeText(this, "SMS 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) exportData(uri);
            });

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importData(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_main);

        dao = SpendingDatabase.getInstance(this).spendingDao();
        currentMonth = Calendar.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        tvMonth = findViewById(R.id.tv_month);
        tvTotal = findViewById(R.id.tv_total);

        RecyclerView rv = findViewById(R.id.rv_spending);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SpendingAdapter(this::onSpendingClicked);
        rv.setAdapter(adapter);

        findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            loadData();
        });
        findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            loadData();
        });

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddOptions());

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        String prefix = new SimpleDateFormat("yyyy-MM", Locale.KOREA).format(currentMonth.getTime());
        tvMonth.setText(new SimpleDateFormat("yyyy년 MM월", Locale.KOREA).format(currentMonth.getTime()));

        List<SpendingRecord> records = dao.getByMonth(prefix);
        long total = dao.getMonthTotal(prefix);
        adapter.setItems(records);
        tvTotal.setText(String.format(Locale.KOREA, "합계: %,d원", total));
    }

    private void showAddOptions() {
        new AlertDialog.Builder(this)
                .setTitle("추가 방법 선택")
                .setItems(new CharSequence[]{"문자에서 선택", "직접 입력"}, (dialog, which) -> {
                    if (which == 0) requestSmsAndPick();
                    else openManualEntry();
                })
                .show();
    }

    private void requestSmsAndPick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            startSmsPickerActivity();
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS);
        }
    }

    private void startSmsPickerActivity() {
        startActivity(new Intent(this, SmsPickerActivity.class));
    }

    private void openManualEntry() {
        SpendingEntryDialog dialog = SpendingEntryDialog.newAddInstance(null, 0, null);
        dialog.setOnSaveListener(this::loadData);
        dialog.show(getSupportFragmentManager(), "entry");
    }

    private void onSpendingClicked(SpendingRecord record) {
        SpendingEntryDialog dialog = SpendingEntryDialog.newEditInstance(record);
        dialog.setOnSaveListener(this::loadData);
        dialog.show(getSupportFragmentManager(), "edit");
    }

    private boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_export) {
            String filename = new SimpleDateFormat("yyyyMM", Locale.KOREA)
                    .format(currentMonth.getTime()) + "_용돈기록.json";
            exportLauncher.launch(filename);
            return true;
        }
        if (id == R.id.menu_import) {
            importLauncher.launch(new String[]{"application/json", "*/*"});
            return true;
        }
        return false;
    }

    private void exportData(Uri uri) {
        List<SpendingRecord> all = dao.getAll();
        try {
            JsonExportImport.export(getContentResolver(), uri, all);
            Toast.makeText(this, "내보내기 완료 (" + all.size() + "건)", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "내보내기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importData(Uri uri) {
        try {
            List<SpendingRecord> imported = JsonExportImport.importFrom(getContentResolver(), uri);
            int count = 0;
            for (SpendingRecord r : imported) {
                if (r.usedDate == null || r.amount <= 0) continue;
                String purpose = r.purpose != null ? r.purpose : "";
                if (dao.countDuplicate(r.usedDate, r.amount, purpose) == 0) {
                    dao.insert(r);
                    count++;
                }
            }
            Toast.makeText(this, "가져오기 완료 (" + count + "건 추가)", Toast.LENGTH_SHORT).show();
            loadData();
        } catch (IOException e) {
            Toast.makeText(this, "가져오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
