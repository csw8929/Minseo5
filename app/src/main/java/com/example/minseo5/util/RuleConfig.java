package com.example.minseo5.util;

import java.util.ArrayList;
import java.util.List;

public class RuleConfig {

    public int version;
    public String yearInference = "recentPast";
    public List<Rule> rules = new ArrayList<>();

    public static class Rule {
        public String name;
        public String pattern;
        public int amountGroup;
        public int monthGroup;
        public int dayGroup;
        public int hourGroup;
        public int minGroup;
        public int purposeGroup;
    }
}
