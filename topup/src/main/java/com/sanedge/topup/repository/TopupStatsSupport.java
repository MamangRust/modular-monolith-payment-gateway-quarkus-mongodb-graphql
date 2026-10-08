package com.sanedge.topup.repository.stats;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Variable;

public final class TopupStatsSupport {

    public static final String[] MONTH_ABBR = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private TopupStatsSupport() {}

    public static String monthAbbr(int month) {
        return MONTH_ABBR[month - 1];
    }

    public static Date startOfYear(int year) {
        return Date.from(LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    public static long longValue(Document doc, String key) {
        return ((Number) doc.getOrDefault(key, 0)).longValue();
    }

    public static int intValue(Document doc, String key) {
        return ((Number) doc.getOrDefault(key, 0)).intValue();
    }

    /**
     * Base stages untuk agregasi topups:
     * - filter deletedAt null, topupTime dalam [start, end)
     * - optional filter cardNumber
     * - $lookup ke "cards" dan pastikan card belum dihapus
     */
    public static List<Bson> baseStages(Date start, Date end, String cardNumber) {
        List<Bson> stages = new ArrayList<>();

        List<Bson> matchConds = new ArrayList<>();
        matchConds.add(Filters.eq("deletedAt", null));
        matchConds.add(Filters.gte("topupTime", start));
        matchConds.add(Filters.lt("topupTime", end));
        if (cardNumber != null) {
            matchConds.add(Filters.eq("cardNumber", cardNumber));
        }
        stages.add(Aggregates.match(Filters.and(matchConds)));

        stages.add(Aggregates.lookup(
                "cards",
                List.of(new Variable<>("cardNum", "$cardNumber")),
                List.of(
                    Aggregates.match(new Document("$expr",
                        new Document("$eq", List.of("$cardNumber", "$$cardNum")))),
                    Aggregates.match(Filters.eq("deletedAt", null))
                ),
                "validCards"));

        stages.add(Aggregates.match(Filters.expr(
                new Document("$gt",
                    List.of(new Document("$size", "$validCards"), 0)))));

        return stages;
    }
}
