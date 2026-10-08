package com.sanedge.transfer.repository.stats;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

/**
 * Shared helpers for the transfer statistics aggregations.
 *
 * <p>All modules share one MongoDB database, so the aggregations run directly
 * over the {@code transfers} collection. The former PostgreSQL {@code cards}
 * join is replaced by an equality filter on {@code transferFrom}/{@code transferTo}
 * (the card-number fields stored on the transfer itself), matching the sibling
 * {@code transaction} module.</p>
 */
public final class TransferStatsSupport {

    public static final String[] MONTH_ABBR = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    /** Mode filter card. */
    public enum CardMode {
        /** Hanya transfer_from (sender). */
        SENDER,
        /** Hanya transfer_to (receiver). */
        RECEIVER,
        /** transfer_from ATAU transfer_to (dipakai di TransferStatusByCard). */
        SENDER_OR_RECEIVER,
        /** Tidak filter card (semua transfer). */
        NONE
    }

    private TransferStatsSupport() {}

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
     * Base stages untuk agregasi transfers: filter {@code deletedAt} null,
     * {@code transferTime} dalam [start, end) dan optional filter cardNumber
     * sesuai mode.
     *
     * @param cardNumber null untuk tanpa filter card.
     */
    public static List<Bson> baseStages(Date start, Date end,
                                        String cardNumber, CardMode mode) {
        List<Bson> stages = new ArrayList<>();

        List<Bson> matchConds = new ArrayList<>();
        matchConds.add(Filters.eq("deletedAt", null));
        matchConds.add(Filters.gte("transferTime", start));
        matchConds.add(Filters.lt("transferTime", end));
        if (cardNumber != null && mode != CardMode.NONE) {
            switch (mode) {
                case SENDER -> matchConds.add(Filters.eq("transferFrom", cardNumber));
                case RECEIVER -> matchConds.add(Filters.eq("transferTo", cardNumber));
                case SENDER_OR_RECEIVER -> matchConds.add(Filters.or(
                        Filters.eq("transferFrom", cardNumber),
                        Filters.eq("transferTo", cardNumber)));
                default -> { /* noop */ }
            }
        }
        stages.add(Aggregates.match(Filters.and(matchConds)));

        return stages;
    }

    /** Overload tanpa card filter. */
    public static List<Bson> baseStages(Date start, Date end) {
        return baseStages(start, end, null, CardMode.NONE);
    }
}
