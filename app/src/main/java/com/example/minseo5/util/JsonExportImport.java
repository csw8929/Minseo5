package com.example.minseo5.util;

import android.content.ContentResolver;
import android.net.Uri;

import com.example.minseo5.db.SpendingRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class JsonExportImport {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<SpendingRecord>>() {}.getType();

    public static void export(ContentResolver resolver, Uri uri, List<SpendingRecord> records) throws IOException {
        try (OutputStream out = resolver.openOutputStream(uri);
             Writer writer = new OutputStreamWriter(out)) {
            GSON.toJson(records, LIST_TYPE, writer);
        }
    }

    public static List<SpendingRecord> importFrom(ContentResolver resolver, Uri uri) throws IOException {
        try (InputStream in = resolver.openInputStream(uri);
             Reader reader = new InputStreamReader(in)) {
            List<SpendingRecord> list = GSON.fromJson(reader, LIST_TYPE);
            if (list == null) return Collections.emptyList();
            for (SpendingRecord r : list) r.id = 0;
            return list;
        }
    }
}
