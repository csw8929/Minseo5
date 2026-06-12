package com.example.minseo5.util;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    public static class ParseResult {
        public String usedDate;
        public String usedTime;   // "HH:mm" or null
        public long amount;
        public String purpose;
        public boolean duplicate; // 검토 시 DB/배치 중복 여부 (영속 아님)
    }

    public static List<ParseResult> parseAll(Context context, String text) {
        List<ParseResult> out = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return out;

        RuleConfig cfg = RuleStore.get(context);
        if (cfg.rules == null) return out;

        for (RuleConfig.Rule rule : cfg.rules) {
            if (rule == null || rule.pattern == null) continue;
            Pattern pattern;
            try {
                pattern = Pattern.compile(rule.pattern);
            } catch (Exception e) {
                Log.e("MINSEO5", "룰 정규식 오류: " + rule.name, e);
                continue;
            }
            Matcher m = pattern.matcher(text);
            while (m.find()) {
                ParseResult r = new ParseResult();
                r.amount = toAmount(group(m, rule.amountGroup));
                r.usedDate = toDate(group(m, rule.monthGroup), group(m, rule.dayGroup),
                        cfg.yearInference);
                r.usedTime = toTime(group(m, rule.hourGroup), group(m, rule.minGroup));
                String purpose = group(m, rule.purposeGroup);
                r.purpose = purpose != null ? purpose.trim() : "";
                out.add(r);
            }
        }
        return out;
    }

    // 룰에 안 잡힌(파싱 실패) 블록을 반환 → 사용자에게 "어느 부분이 안 됐는지" 표시용
    public static List<String> findUnparsed(Context context, String text) {
        List<String> unparsed = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return unparsed;

        String[] blocks = text.contains("[Web발신]")
                ? text.split("\\[Web발신\\]")
                : text.split("\\n{2,}");

        for (String block : blocks) {
            String t = block.trim();
            if (t.isEmpty()) continue;
            if (parseAll(context, t).isEmpty()) unparsed.add(t);
        }
        return unparsed;
    }

    private static String group(Matcher m, int idx) {
        if (idx <= 0 || idx > m.groupCount()) return null;
        return m.group(idx);
    }

    private static String toTime(String hh, String mm) {
        if (hh == null || mm == null) return null;
        try {
            return String.format(Locale.KOREA, "%02d:%02d",
                    Integer.parseInt(hh.trim()), Integer.parseInt(mm.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long toAmount(String s) {
        if (s == null) return 0;
        try {
            return Long.parseLong(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String toDate(String mm, String dd, String mode) {
        Calendar today = Calendar.getInstance();
        if (mm == null || dd == null) {
            return String.format(Locale.KOREA, "%04d-%02d-%02d",
                    today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1,
                    today.get(Calendar.DAY_OF_MONTH));
        }
        int month, day;
        try {
            month = Integer.parseInt(mm.trim());
            day = Integer.parseInt(dd.trim());
        } catch (NumberFormatException e) {
            return String.format(Locale.KOREA, "%04d-%02d-%02d",
                    today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1,
                    today.get(Calendar.DAY_OF_MONTH));
        }

        int year = today.get(Calendar.YEAR);
        if ("recentPast".equals(mode)) {
            Calendar candidate = Calendar.getInstance();
            candidate.set(year, month - 1, day, 23, 59, 59);
            if (candidate.after(today)) year -= 1;
        }
        return String.format(Locale.KOREA, "%04d-%02d-%02d", year, month, day);
    }
}
