package com.example.minseo5.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataJsonStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final String DIR_NAME = "Minseo5";
    private static final String FILE_NAME = "data.json";

    public static List<String> load(Context context) {
        try {
            String json;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                json = loadViaMediaStore(context);
            } else {
                json = loadViaFile();
            }
            if (json == null) return new ArrayList<>();
            List<String> list = GSON.fromJson(json, LIST_TYPE);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            android.util.Log.e("MINSEO5", "DataJsonStore.load FAILED", e);
            return new ArrayList<>();
        }
    }

    public static void save(Context context, List<String> rawList) throws IOException {
        byte[] bytes = GSON.toJson(rawList, LIST_TYPE).getBytes(StandardCharsets.UTF_8);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, bytes);
        } else {
            saveViaFile(bytes);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static String loadViaMediaStore(Context context) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        Uri collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String relativePath = Environment.DIRECTORY_DOCUMENTS + "/" + DIR_NAME + "/";
        Uri target = findExisting(resolver, collection, relativePath);
        if (target == null) return null;
        try (InputStream is = resolver.openInputStream(target);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return readAll(reader);
        }
    }

    private static String loadViaFile() throws IOException {
        File file = new File(new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), DIR_NAME), FILE_NAME);
        if (!file.exists()) return null;
        try (InputStream is = new FileInputStream(file);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return readAll(reader);
        }
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
        return sb.toString();
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static void saveViaMediaStore(Context context, byte[] bytes) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        Uri collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String relativePath = Environment.DIRECTORY_DOCUMENTS + "/" + DIR_NAME + "/";
        Uri target = findExisting(resolver, collection, relativePath);
        boolean isNew = target == null;
        if (isNew) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            target = resolver.insert(collection, values);
            if (target == null) throw new IOException("data.json 생성 실패");
        }
        try (OutputStream os = resolver.openOutputStream(target, "wt")) {
            if (os == null) throw new IOException("data.json 쓰기 실패");
            os.write(bytes);
        }
        if (isNew) {
            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(target, done, null, null);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static Uri findExisting(ContentResolver resolver, Uri collection, String relativePath) {
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=? AND "
                + MediaStore.MediaColumns.DISPLAY_NAME + "=?";
        String[] args = {relativePath, FILE_NAME};
        try (Cursor c = resolver.query(collection,
                new String[]{MediaStore.MediaColumns._ID}, selection, args, null)) {
            if (c != null && c.moveToFirst()) {
                return ContentUris.withAppendedId(collection, c.getLong(0));
            }
        }
        return null;
    }

    private static void saveViaFile(byte[] bytes) throws IOException {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), DIR_NAME);
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("폴더 생성 실패");
        File file = new File(dir, FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }
    }
}
