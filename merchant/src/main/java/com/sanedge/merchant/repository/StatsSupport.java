package com.sanedge.merchant.repository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.Document;

/**
 * Shared helpers for the merchant statistics aggregations. Keeps the month
 * abbreviations aligned with PostgreSQL {@code TO_CHAR(..., 'Mon')} and
 * provides the UTC date helpers plus a stable month ordering for the Java-side
 * result sorting.
 */
public final class StatsSupport {

    public static final String[] MONTH_ABBRS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    private StatsSupport() {
    }

    public static String monthAbbr(int month) {
        return MONTH_ABBRS[month - 1];
    }

    public static int monthIndex(String abbr) {
        return Arrays.asList(MONTH_ABBRS).indexOf(abbr) + 1;
    }

    public static List<String> monthAbbrList() {
        return Arrays.asList(MONTH_ABBRS);
    }

    public static Date startOfYear(int year) {
        return Date.from(LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    public static Date firstDayOfMonth(int year, int month) {
        return Date.from(LocalDate.of(year, month, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    public static long longValue(Document doc, String key) {
        Object value = doc.get(key);
        return value == null ? 0L : ((Number) value).longValue();
    }
}
