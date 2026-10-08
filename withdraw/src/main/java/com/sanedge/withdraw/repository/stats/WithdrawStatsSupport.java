package com.sanedge.withdraw.repository.stats;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

public final class WithdrawStatsSupport {

    public static final String[] MONTH_ABBR = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private WithdrawStatsSupport() {}

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
     * Base stages for withdraw aggregations:
     * - filter deletedAt null, withdrawTime within [start, end)
     * - optional filter cardNumber
     * - $lookup to "cards" and ensure the card is not deleted
     */
    public static List<Bson> baseStages(Date start, Date end, String cardNumber) {
        List<Bson> stages = new ArrayList<>();

        List<Bson> matchConds = new ArrayList<>();
        matchConds.add(Filters.eq("deletedAt", null));
        matchConds.add(Filters.gte("withdrawTime", start));
        matchConds.add(Filters.lt("withdrawTime", end));
        if (cardNumber != null) {
            matchConds.add(Filters.eq("cardNumber", cardNumber));
        }
        stages.add(Aggregates.match(Filters.and(matchConds)));

        stages.add(Aggregates.lookup("cards", "cardNumber", "cardNumber", "validCards"));

        stages.add(Aggregates.match(Filters.and(
                Filters.expr(new Document("$gt",
                    List.of(new Document("$size", "$validCards"), 0))),
                Filters.eq("validCards.deletedAt", null))));

        return stages;
    }
}
