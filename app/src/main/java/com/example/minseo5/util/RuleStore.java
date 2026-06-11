package com.example.minseo5.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class RuleStore {

    private static final String DIR_NAME = "Minseo5";
    private static final String FILE_NAME = "rules.json";
    private static final Gson GSON = new Gson();

    private static RuleConfig cached;

    public static RuleConfig get(Context context) {
        if (cached == null) cached = load(context);
        return cached;
    }

    public static void reload(Context context) {
        cached = load(context);
    }

    private static RuleConfig load(Context context) {
        RuleConfig external = loadExternal();
        if (external != null && external.rules != null && !external.rules.isEmpty()) {
            Log.d("MINSEO5", "RuleStore: 외부 rules.json 사용 (" + external.rules.size() + "개)");
            return external;
        }
        RuleConfig def = loadAssets(context);
        seedExternal(context);
        Log.d("MINSEO5", "RuleStore: 기본(assets) rules.json 사용");
        return def;
    }

    private static RuleConfig loadExternal() {
        try {
            File file = externalFile();
            if (!file.exists() || !file.canRead()) return null;
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, RuleConfig.class);
            }
        } catch (Exception e) {
            Log.e("MINSEO5", "RuleStore.loadExternal 실패", e);
            return null;
        }
    }

    private static RuleConfig loadAssets(Context context) {
        try (Reader reader = new InputStreamReader(
                context.getAssets().open(FILE_NAME), StandardCharsets.UTF_8)) {
            RuleConfig c = GSON.fromJson(reader, RuleConfig.class);
            return c != null ? c : new RuleConfig();
        } catch (Exception e) {
            Log.e("MINSEO5", "RuleStore.loadAssets 실패", e);
            return new RuleConfig();
        }
    }

    private static void seedExternal(Context context) {
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), DIR_NAME);
            File file = new File(dir, FILE_NAME);
            if (file.exists()) return;
            if (!dir.exists() && !dir.mkdirs()) return;
            try (InputStream in = context.getAssets().open(FILE_NAME);
                 OutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            Log.d("MINSEO5", "RuleStore: rules.json 시드 생성 " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.d("MINSEO5", "RuleStore.seedExternal 생략 (권한 없음 가능): " + e.getMessage());
        }
    }

    private static File externalFile() {
        return new File(new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), DIR_NAME), FILE_NAME);
    }
}
