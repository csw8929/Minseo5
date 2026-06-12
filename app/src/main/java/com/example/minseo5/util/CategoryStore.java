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

public class CategoryStore {

    private static final String DIR_NAME = "Minseo5";
    private static final String FILE_NAME = "categories.json";
    private static final Gson GSON = new Gson();

    private static CategoryConfig cached;

    public static CategoryConfig get(Context context) {
        if (cached == null) cached = load(context);
        return cached;
    }

    public static void reload(Context context) {
        cached = load(context);
    }

    private static CategoryConfig load(Context context) {
        CategoryConfig def = loadAssets(context);
        CategoryConfig external = loadExternal();
        if (external != null && external.categories != null && !external.categories.isEmpty()
                && external.version >= def.version) {
            Log.d("MINSEO5", "CategoryStore: 외부 categories.json 사용 (" + external.categories.size() + "개)");
            return external;
        }
        seedExternal(context);
        Log.d("MINSEO5", "CategoryStore: 기본(assets) categories.json 사용/시드 (v" + def.version + ")");
        return def;
    }

    private static CategoryConfig loadExternal() {
        try {
            File file = externalFile();
            if (!file.exists() || !file.canRead()) return null;
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, CategoryConfig.class);
            }
        } catch (Exception e) {
            Log.e("MINSEO5", "CategoryStore.loadExternal 실패", e);
            return null;
        }
    }

    private static CategoryConfig loadAssets(Context context) {
        try (Reader reader = new InputStreamReader(
                context.getAssets().open(FILE_NAME), StandardCharsets.UTF_8)) {
            CategoryConfig c = GSON.fromJson(reader, CategoryConfig.class);
            return c != null ? c : new CategoryConfig();
        } catch (Exception e) {
            Log.e("MINSEO5", "CategoryStore.loadAssets 실패", e);
            return new CategoryConfig();
        }
    }

    private static void seedExternal(Context context) {
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), DIR_NAME);
            File file = new File(dir, FILE_NAME);
            if (!dir.exists() && !dir.mkdirs()) return;
            try (InputStream in = context.getAssets().open(FILE_NAME);
                 OutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            Log.d("MINSEO5", "CategoryStore: categories.json 시드 생성 " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.d("MINSEO5", "CategoryStore.seedExternal 생략 (권한 없음 가능): " + e.getMessage());
        }
    }

    private static File externalFile() {
        return new File(new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), DIR_NAME), FILE_NAME);
    }
}
