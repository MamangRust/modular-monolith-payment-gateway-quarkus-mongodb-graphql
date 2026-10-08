package com.sanedge.saldo.repository.stats;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

public final class SaldoStatsSupport {

    public static final String[] MONTH_ABBR = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private SaldoStatsSupport() {}

    public static String monthAbbr(int month) {
        return MONTH_ABBR[month - 1];
    }

    public static Date startOfYear(int year) {
        return Date.from(LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    public static long longValue(Document doc, String key) {
        return ((Number) doc.getOrDefault(key, 0)).longValue();
    }

    /**
     * Base stages for saldo aggregations: only non-deleted saldos whose card still
     * exists (lookup to {@code cards}) and whose {@code createdAt} falls in the
     * [start, end) range.
     */
    public static List<Bson> baseStages(Date start, Date end) {
        List<Bson> stages = new ArrayList<>();

        stages.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.gte("createdAt", start),
                Filters.lt("createdAt", end))));

        stages.add(Aggregates.lookup("cards", "cardNumber", "cardNumber", "validCards"));

        stages.add(Aggregates.match(Filters.and(
                Filters.expr(new Document("$gt",
                    List.of(new Document("$size", "$validCards"), 0))),
                Filters.eq("validCards.deletedAt", null))));

        return stages;
    }
}
