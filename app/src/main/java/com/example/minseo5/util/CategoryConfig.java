package com.example.minseo5.util;

import java.util.ArrayList;
import java.util.List;

public class CategoryConfig {

    public int version;
    public String defaultCategory = "기타";
    public List<Category> categories = new ArrayList<>();

    public static class Category {
        public String category;
        public List<String> keywords = new ArrayList<>();
    }
}
