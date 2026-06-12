package com.example.minseo5;

import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.minseo5.db.SpendingDao;
import com.example.minseo5.db.SpendingDatabase;
import com.example.minseo5.db.SpendingRecord;
import com.example.minseo5.util.CategoryColors;
import com.example.minseo5.util.CategoryStore;
import com.example.minseo5.util.Categorizer;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    public static final String EXTRA_MONTH = "extra_month"; // "yyyy-MM"

    // 파스텔 톤 컬러풀 팔레트 (시안 "파스텔 톤 컬러풀한 아이콘")
    private static final int[] PALETTE = {
            0xFF66BB6A, 0xFF4FC3F7, 0xFFFFB74D, 0xFFF06292, 0xFFBA68C8,
            0xFFFFD54F, 0xFF4DB6AC, 0xFFA1887F, 0xFF90A4AE, 0xFFFF8A65
    };
    private static final int SLICE_VALUE_COLOR = 0xFF26323A; // 파스텔 슬라이스 위 진한 텍스트

    private SpendingDao dao;
    private Categorizer categorizer;
    private Calendar currentMonth;

    private TextView tvMonth, tvTotal, tvCompare, tvEtcHint;
    private PieChart pieChart;
    private BarChart barChart;
    private LinearLayout top5Container, emptyContainer;

    private int onSurface;
    private int onSurfaceVar;
    private int chartBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        applySystemBarIconColors();

        dao = SpendingDatabase.getInstance(this).spendingDao();
        CategoryStore.reload(this);
        categorizer = Categorizer.of(this);

        onSurface = themeColor(com.google.android.material.R.attr.colorOnSurface);
        onSurfaceVar = themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        chartBar = ContextCompat.getColor(this, R.color.chart_bar);

        currentMonth = Calendar.getInstance();
        String month = getIntent().getStringExtra(EXTRA_MONTH);
        if (month != null && month.length() >= 7) {
            try {
                currentMonth.set(Calendar.YEAR, Integer.parseInt(month.substring(0, 4)));
                currentMonth.set(Calendar.MONTH, Integer.parseInt(month.substring(5, 7)) - 1);
            } catch (NumberFormatException ignored) {
            }
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvMonth = findViewById(R.id.tv_month);
        tvTotal = findViewById(R.id.tv_total);
        tvCompare = findViewById(R.id.tv_compare);
        tvEtcHint = findViewById(R.id.tv_etc_hint);
        pieChart = findViewById(R.id.pie_chart);
        barChart = findViewById(R.id.bar_chart);
        top5Container = findViewById(R.id.top5_container);
        emptyContainer = findViewById(R.id.empty_container);

        findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            loadStats();
        });
        findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            loadStats();
        });
        findViewById(R.id.btn_add_empty).setOnClickListener(v -> finish());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets wi = getWindowManager().getCurrentWindowMetrics().getWindowInsets();
            android.graphics.Insets bars = wi.getInsets(WindowInsets.Type.systemBars());
            findViewById(R.id.root_layout).setPadding(bars.left, bars.top, bars.right, bars.bottom);
        }

        setupCharts();
        loadStats();
    }

    private void applySystemBarIconColors() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        boolean lightBackground = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        controller.setAppearanceLightStatusBars(lightBackground);
        controller.setAppearanceLightNavigationBars(lightBackground);
    }

    private void setupCharts() {
        Description none = new Description();
        none.setEnabled(false);

        pieChart.setDescription(none);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawEntryLabels(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(0x00000000);
        pieChart.setHoleRadius(70f);
        pieChart.setTransparentCircleRadius(73f);
        pieChart.setCenterTextColor(onSurface);
        pieChart.setCenterTextSize(15f);
        Legend pieLegend = pieChart.getLegend();
        pieLegend.setEnabled(true);
        pieLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieLegend.setWordWrapEnabled(true);
        pieLegend.setTextColor(onSurface);
        pieLegend.setTextSize(12f);

        Description none2 = new Description();
        none2.setEnabled(false);
        barChart.setDescription(none2);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setScaleYEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setNoDataTextColor(onSurfaceVar);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(onSurfaceVar);
        barChart.getAxisLeft().setTextColor(onSurfaceVar);
        barChart.getAxisLeft().setGridColor(themeColor(com.google.android.material.R.attr.colorOutlineVariant));
    }

    private void loadStats() {
        String prefix = new SimpleDateFormat("yyyy-MM", Locale.KOREA).format(currentMonth.getTime());
        tvMonth.setText(new SimpleDateFormat("yyyy년 MM월", Locale.KOREA).format(currentMonth.getTime()));

        List<SpendingRecord> records = dao.getByMonth(prefix);
        long total = dao.getMonthTotal(prefix);

        Calendar prev = (Calendar) currentMonth.clone();
        prev.add(Calendar.MONTH, -1);
        String prevPrefix = new SimpleDateFormat("yyyy-MM", Locale.KOREA).format(prev.getTime());
        long prevTotal = dao.getMonthTotal(prevPrefix);

        tvTotal.setText(String.format(Locale.KOREA, "%,d원", total));
        tvCompare.setText(buildCompareText(total, prevTotal));

        boolean empty = records.isEmpty();
        emptyContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
        setContentVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            pieChart.clear();
            barChart.clear();
            top5Container.removeAllViews();
            tvEtcHint.setVisibility(View.GONE);
            return;
        }

        bindPie(records, total);
        bindBar(records);
        bindTop5(records, total);
    }

    private void setContentVisibility(int visibility) {
        int[] ids = {R.id.label_pie, R.id.card_pie, R.id.label_bar, R.id.card_bar,
                R.id.label_top5, R.id.card_top5};
        for (int id : ids) findViewById(id).setVisibility(visibility);
    }

    private String buildCompareText(long total, long prevTotal) {
        if (prevTotal <= 0) return "전월 기록 없음";
        long diff = total - prevTotal;
        if (diff == 0) return "전월과 동일";
        String arrow = diff > 0 ? "▲" : "▼";
        return String.format(Locale.KOREA, "전월 대비 %s %,d원", arrow, Math.abs(diff));
    }

    private void bindPie(List<SpendingRecord> records, long total) {
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (SpendingRecord r : records) {
            String cat = categorizer.categorize(r.purpose);
            byCategory.put(cat, byCategory.getOrDefault(cat, 0L) + r.amount);
        }

        long etcSum = byCategory.getOrDefault(categorizer.defaultCategory(), 0L);
        if (total > 0 && etcSum * 2 > total) {
            tvEtcHint.setVisibility(View.VISIBLE);
        } else {
            tvEtcHint.setVisibility(View.GONE);
        }

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(byCategory.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (Map.Entry<String, Long> e : sorted) {
            if (e.getValue() <= 0) continue;
            entries.add(new PieEntry(e.getValue(), e.getKey()));
            colors.add(CategoryColors.colorFor(e.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(SLICE_VALUE_COLOR);
        dataSet.setValueLinePart1OffsetPercentage(80f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(data);
        pieChart.setCenterText(String.format(Locale.KOREA, "합계\n%,d원", total));
        pieChart.highlightValues(null);
        pieChart.animateY(700);
        pieChart.invalidate();
    }

    private void bindBar(List<SpendingRecord> records) {
        int maxDay = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        long[] byDay = new long[maxDay + 1];
        for (SpendingRecord r : records) {
            int day = dayOf(r.usedDate);
            if (day >= 1 && day <= maxDay) byDay[day] += r.amount;
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int d = 1; d <= maxDay; d++) {
            entries.add(new BarEntry(d, byDay[d]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "일별 지출");
        dataSet.setColor(chartBar);
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        barChart.setData(data);
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        barChart.getXAxis().setAxisMinimum(0.5f);
        barChart.getXAxis().setAxisMaximum(maxDay + 0.5f);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.animateY(700);
        barChart.invalidate();
    }

    private void bindTop5(List<SpendingRecord> records, long total) {
        Map<String, Long> byPurpose = new LinkedHashMap<>();
        for (SpendingRecord r : records) {
            String key = (r.purpose == null || r.purpose.trim().isEmpty()) ? "(미상)" : r.purpose.trim();
            byPurpose.put(key, byPurpose.getOrDefault(key, 0L) + r.amount);
        }

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(byPurpose.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        top5Container.removeAllViews();
        int count = Math.min(5, sorted.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<String, Long> e = sorted.get(i);
            int percent = total > 0 ? (int) Math.round(e.getValue() * 100.0 / total) : 0;
            top5Container.addView(buildTop5Row(i + 1, e.getKey(), e.getValue(), percent));
        }
    }

    private View buildTop5Row(int rank, String purpose, long amount, int percent) {
        int padV = dp(10);
        int padH = dp(8);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(padH, padV, padH, padV);

        int tile = dp(40);
        int color = PALETTE[(rank - 1) % PALETTE.length];
        TextView rankView = new TextView(this);
        rankView.setText(String.valueOf(rank));
        rankView.setTextColor(0xFFFFFFFF);
        rankView.setTextSize(16f);
        rankView.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        rankView.setBackground(circle);
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(tile, tile);
        tileParams.setMarginEnd(dp(12));
        rankView.setLayoutParams(tileParams);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(colParams);

        TextView nameView = new TextView(this);
        nameView.setText(purpose);
        nameView.setTextSize(15f);
        nameView.setTextColor(onSurface);
        nameView.setMaxLines(1);
        nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView pctView = new TextView(this);
        pctView.setText(String.format(Locale.KOREA, "%d%%", percent));
        pctView.setTextSize(12f);
        pctView.setTextColor(onSurfaceVar);

        textCol.addView(nameView);
        textCol.addView(pctView);

        TextView amountView = new TextView(this);
        amountView.setText(String.format(Locale.KOREA, "%,d원", amount));
        amountView.setTextSize(15f);
        amountView.setTextColor(onSurface);

        row.addView(rankView);
        row.addView(textCol);
        row.addView(amountView);
        return row;
    }

    private int themeColor(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private static int dayOf(String usedDate) {
        if (usedDate == null || usedDate.length() < 10) return -1;
        try {
            return Integer.parseInt(usedDate.substring(8, 10));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
