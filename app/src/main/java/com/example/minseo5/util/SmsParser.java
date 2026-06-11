package com.example.minseo5.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("(\\d[\\d,]*)원");
    private static final Pattern BANK_TRANSFER =
            Pattern.compile("(.{1,20}?)(?:에서|로부터)\\s*(?:보냄|입금|이체)");
    private static final Pattern CARD_MERCHANT =
            Pattern.compile("승인[^가-힣A-Za-z0-9]*([가-힣A-Za-z0-9][가-힣A-Za-z0-9\\s]{1,15})");
    private static final Pattern WEB_MERCHANT =
            Pattern.compile("\\[Web발신\\]\\s*[^\\n]*?([가-힣A-Za-z0-9]{2,})");

    public static class ParseResult {
        public String usedDate;
        public long amount;
        public String purpose;
    }

    public static ParseResult parse(String address, String body, long timestamp) {
        ParseResult result = new ParseResult();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        result.usedDate = sdf.format(new Date(timestamp));

        if (body != null) {
            Matcher m = AMOUNT_PATTERN.matcher(body);
            if (m.find()) {
                try {
                    result.amount = Long.parseLong(m.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) {
                }
            }
            result.purpose = extractPurpose(address, body);
        } else {
            result.purpose = address != null ? address : "";
        }

        return result;
    }

    private static String extractPurpose(String address, String body) {
        Matcher m = BANK_TRANSFER.matcher(body);
        if (m.find()) return m.group(1).trim();

        m = CARD_MERCHANT.matcher(body);
        if (m.find()) {
            String candidate = m.group(1).trim();
            if (candidate.length() >= 2) return candidate;
        }

        m = WEB_MERCHANT.matcher(body);
        if (m.find()) return m.group(1).trim();

        return address != null ? address : "";
    }
}
