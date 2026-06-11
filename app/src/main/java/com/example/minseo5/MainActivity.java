package com.example.minseo5;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.WindowInsets;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minseo5.db.SpendingDao;
import com.example.minseo5.db.SpendingDatabase;
import com.example.minseo5.db.SpendingRecord;
import com.example.minseo5.ui.SpendingAdapter;
import com.example.minseo5.ui.SpendingEntryDialog;
import com.example.minseo5.util.JsonExportImport;
import com.example.minseo5.util.RuleStore;
import com.google.android.material.appbar.AppBarLayout;
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

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) exportData(uri);
            });

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importData(uri);
            });

    private final ActivityResultLauncher<Intent> smsPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String date = result.getData().getStringExtra(SmsPickerActivity.RESULT_DATE);
                    if (date != null && date.length() >= 7) {
                        try {
                            currentMonth.set(Calendar.YEAR, Integer.parseInt(date.substring(0, 4)));
                            currentMonth.set(Calendar.MONTH, Integer.parseInt(date.substring(5, 7)) - 1);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                loadData();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        AppBarLayout appBar = findViewById(R.id.appbar);
        int fabMarginBase = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets wi = getWindowManager().getCurrentWindowMetrics().getWindowInsets();
            int top = wi.getInsets(WindowInsets.Type.statusBars()).top;
            int bottom = wi.getInsets(WindowInsets.Type.navigationBars()).bottom;
            appBar.setPadding(0, top, 0, 0);
            fab.post(() -> fab.setTranslationY(-bottom));
        }

        loadData();

        ensureStorageAccess();
        RuleStore.get(this);

        if (savedInstanceState == null) {
            handleSendIntent(getIntent());
        }
    }

    private void ensureStorageAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
        if (Environment.isExternalStorageManager()) return;
        new AlertDialog.Builder(this)
                .setTitle("파일 접근 권한")
                .setMessage("룰/데이터 파일(Documents/Minseo5)을 읽고 쓰려면 '모든 파일 액세스' 권한이 필요합니다. 없으면 기본 룰로만 동작합니다.")
                .setPositiveButton("설정 열기", (d, w) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("나중에", null)
                .show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSendIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        RuleStore.reload(this);
    }

    private void handleSendIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        intent.setAction(null);
        if (text == null || text.trim().isEmpty()) return;

        Intent target = new Intent(this, SmsPickerActivity.class);
        target.putExtra(SmsPickerActivity.EXTRA_RAW_TEXT, text);
        smsPickerLauncher.launch(target);
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
                .setItems(new CharSequence[]{"문자에서 가져오기", "직접 입력"}, (dialog, which) -> {
                    if (which == 0) openMessagingApp();
                    else openManualEntry();
                })
                .show();
    }

    private void openMessagingApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            Toast.makeText(this, "메시지를 길게 눌러 공유 → 용돈 기록 선택", Toast.LENGTH_LONG).show();
            startActivity(intent);
        } else {
            Toast.makeText(this, "메시지 앱을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
        }
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
