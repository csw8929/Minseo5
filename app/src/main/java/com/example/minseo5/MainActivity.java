package com.example.minseo5;

import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minseo5.db.SpendingDao;
import com.example.minseo5.db.SpendingDatabase;
import com.example.minseo5.db.SpendingRecord;
import com.example.minseo5.ui.SpendingAdapter;
import com.example.minseo5.ui.SpendingEntryDialog;
import com.example.minseo5.util.JsonExportImport;
import com.example.minseo5.util.RuleStore;
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
    private MaterialToolbar toolbar;
    private boolean selectionMode = false;

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

        applySystemBarIconColors();

        dao = SpendingDatabase.getInstance(this).spendingDao();
        currentMonth = Calendar.getInstance();

        toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        tvMonth = findViewById(R.id.tv_month);
        tvTotal = findViewById(R.id.tv_total);

        RecyclerView rv = findViewById(R.id.rv_spending);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SpendingAdapter(new SpendingAdapter.Listener() {
            @Override
            public void onItemClick(SpendingRecord record) {
                if (selectionMode) {
                    adapter.toggle(record);
                    updateSelectionTitle();
                } else {
                    onSpendingClicked(record);
                }
            }

            @Override
            public void onItemLongClick(SpendingRecord record) {
                if (!selectionMode) enterSelectionMode();
                adapter.select(record);
                updateSelectionTitle();
            }
        });
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets wi = getWindowManager().getCurrentWindowMetrics().getWindowInsets();
            android.graphics.Insets bars = wi.getInsets(WindowInsets.Type.systemBars());
            findViewById(R.id.root_layout).setPadding(bars.left, bars.top, bars.right, bars.bottom);
        }

        loadData();

        ensureStorageAccess();
        RuleStore.get(this);

        if (savedInstanceState == null) {
            handleSendIntent(getIntent());
        }
    }

    private void applySystemBarIconColors() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        boolean lightBackground = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        controller.setAppearanceLightStatusBars(lightBackground);
        controller.setAppearanceLightNavigationBars(lightBackground);
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
        tvTotal.setText(String.format(Locale.KOREA, "%,d원", total));
    }

    private void showAddOptions() {
        new AlertDialog.Builder(this)
                .setTitle("추가 방법 선택")
                .setItems(new CharSequence[]{"문자에서 가져오기", "복사된 항목 가져오기", "직접 입력"}, (dialog, which) -> {
                    if (which == 0) openMessagingApp();
                    else if (which == 1) importFromClipboard();
                    else openManualEntry();
                })
                .show();
    }

    private void importFromClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip() == null
                || cm.getPrimaryClip().getItemCount() == 0) {
            Toast.makeText(this, "복사된 텍스트가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence text = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
        if (text == null || text.toString().trim().isEmpty()) {
            Toast.makeText(this, "복사된 텍스트가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent target = new Intent(this, SmsPickerActivity.class);
        target.putExtra(SmsPickerActivity.EXTRA_RAW_TEXT, text.toString());
        smsPickerLauncher.launch(target);
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
        if (id == R.id.menu_stats) {
            Intent intent = new Intent(this, StatsActivity.class);
            intent.putExtra(StatsActivity.EXTRA_MONTH,
                    new SimpleDateFormat("yyyy-MM", Locale.KOREA).format(currentMonth.getTime()));
            startActivity(intent);
            return true;
        }
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
        if (id == R.id.menu_sum) {
            showSumDialog();
            return true;
        }
        return false;
    }

    private void enterSelectionMode() {
        selectionMode = true;
        adapter.setSelectionMode(true);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> exitSelectionMode());
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.selection_menu);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
    }

    private void exitSelectionMode() {
        selectionMode = false;
        adapter.setSelectionMode(false);
        adapter.clearSelection();
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
        toolbar.setTitle("용돈 기록");
    }

    private void updateSelectionTitle() {
        toolbar.setTitle(adapter.getSelectedCount() + "개 선택");
    }

    private void showSumDialog() {
        List<SpendingRecord> selected = adapter.getSelectedRecords();
        if (selected.isEmpty()) {
            Toast.makeText(this, "선택된 항목이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        long total = 0;
        for (SpendingRecord r : selected) total += r.amount;

        RecyclerView rv = (RecyclerView) getLayoutInflater()
                .inflate(R.layout.dialog_sum, null);
        rv.setLayoutManager(new LinearLayoutManager(this));
        SpendingAdapter sumAdapter = new SpendingAdapter(new SpendingAdapter.Listener() {
            @Override
            public void onItemClick(SpendingRecord record) {
            }

            @Override
            public void onItemLongClick(SpendingRecord record) {
            }
        });
        sumAdapter.setItems(selected);
        rv.setAdapter(sumAdapter);

        new AlertDialog.Builder(this)
                .setTitle(String.format(Locale.KOREA, "선택 합계: %,d원 (%d건)", total, selected.size()))
                .setView(rv)
                .setPositiveButton("닫기", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (selectionMode) {
            exitSelectionMode();
            return;
        }
        super.onBackPressed();
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
