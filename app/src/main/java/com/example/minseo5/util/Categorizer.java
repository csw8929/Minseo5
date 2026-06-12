package com.example.minseo5.util;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class Categorizer {

    private final List<CategoryConfig.Category> categories;
    private final String defaultCategory;

    private Categorizer(List<CategoryConfig.Category> categories, String defaultCategory) {
        this.categories = categories != null ? categories : new ArrayList<>();
        this.defaultCategory = (defaultCategory != null && !defaultCategory.isEmpty())
                ? defaultCategory : "기타";
    }

    public static Categorizer of(Context context) {
        CategoryConfig cfg = CategoryStore.get(context);
        return new Categorizer(cfg.categories, cfg.defaultCategory);
    }

    public String categorize(String purpose) {
        if (purpose == null || purpose.trim().isEmpty()) return defaultCategory;
        for (CategoryConfig.Category c : categories) {
            if (c == null || c.category == null || c.keywords == null) continue;
            for (String keyword : c.keywords) {
                if (keyword != null && !keyword.isEmpty() && purpose.contains(keyword)) {
                    return c.category;
                }
            }
        }
        return defaultCategory;
    }

    public String defaultCategory() {
        return defaultCategory;
    }
}
