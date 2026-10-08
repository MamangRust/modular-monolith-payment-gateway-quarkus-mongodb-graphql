package com.sanedge.card.repository.stats;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Uni;

public final class CardStatsSupport {

    public static final String[] MONTH_ABBR = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private CardStatsSupport() {}

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
     * Stage awal untuk agregasi "dokumen yang card-nya belum dihapus".
     *
     * @param start      awal rentang (inklusif)
     * @param end        akhir rentang (eksklusif)
     * @param timeField  field waktu untuk range, mis. "createdAt", "topupTime",
     *                   "transactionTime", "transferTime", "withdrawTime"
     * @param cardField  field card di dokumen sumber, mis. "cardNumber",
     *                   "transferFrom", "transferTo"
     */
    public static List<Bson> baseStages(Date start, Date end,
                                        String timeField, String cardField) {
        return baseStages(start, end, timeField, cardField, null);
    }

    /**
     * Sama seperti {@link #baseStages(Date, Date, String, String)}, tapi
     * ditambah filter cardNumber spesifik (untuk repository ByCard).
     *
     * @param cardNumber nilai card yang difilter, atau null untuk tanpa filter.
     */
    public static List<Bson> baseStages(Date start, Date end,
                                        String timeField, String cardField,
                                        String cardNumber) {
        List<Bson> stages = new ArrayList<>();

        List<Bson> matchConds = new ArrayList<>();
        matchConds.add(Filters.eq("deletedAt", null));
        matchConds.add(Filters.gte(timeField, start));
        matchConds.add(Filters.lt(timeField, end));
        if (cardNumber != null) {
            matchConds.add(Filters.eq(cardField, cardNumber));
        }
        stages.add(Aggregates.match(Filters.and(matchConds)));

        stages.add(Aggregates.lookup("cards", cardField, "cardNumber", "validCards"));

        stages.add(Aggregates.match(Filters.and(
                Filters.expr(new Document("$gt",
                    List.of(new Document("$size", "$validCards"), 0))),
                Filters.eq("validCards.deletedAt", null))));

        return stages;
    }

    /**
     * Stage agregasi untuk seluruh rentang waktu: dokumen movement yang belum
     * dihapus dan card-nya (dicocokkan lewat {@code cardField}) juga belum
     * dihapus. Dipakai oleh dashboard totals.
     */
    public static List<Bson> allTimeStages(String cardField, String cardNumber) {
        List<Bson> stages = new ArrayList<>();

        List<Bson> matchConds = new ArrayList<>();
        matchConds.add(Filters.eq("deletedAt", null));
        if (cardNumber != null) {
            matchConds.add(Filters.eq(cardField, cardNumber));
        }
        stages.add(Aggregates.match(Filters.and(matchConds)));

        stages.add(Aggregates.lookup("cards", cardField, "cardNumber", "validCards"));

        stages.add(Aggregates.match(Filters.and(
                Filters.expr(new Document("$gt",
                    List.of(new Document("$size", "$validCards"), 0))),
                Filters.eq("validCards.deletedAt", null))));

        return stages;
    }

    /**
     * SUM satu field pada collection lain (saldos/topups/transactions/transfers/
     * withdraws). Bila {@code requireValidCard} true, movement hanya dihitung
     * bila card-nya masih ada (lookup ke {@code cards}); bila false, cukup
     * filter {@code deletedAt: null} pada movement (dipakai untuk transfer by
     * sender / receiver yang pada SQL lama tidak join ke cards).
     */
    public static Uni<Long> sumField(ReactiveMongoDatabase database, String collection, String amountField,
                                     String cardField, String cardNumber, boolean requireValidCard) {
        List<Bson> pipeline;
        if (requireValidCard) {
            pipeline = new ArrayList<>(allTimeStages(cardField, cardNumber));
        } else {
            List<Bson> matchConds = new ArrayList<>();
            matchConds.add(Filters.eq("deletedAt", null));
            if (cardNumber != null) {
                matchConds.add(Filters.eq(cardField, cardNumber));
            }
            pipeline = new ArrayList<>();
            pipeline.add(Aggregates.match(Filters.and(matchConds)));
        }
        pipeline.add(Aggregates.group((Bson) null, Accumulators.sum("total", "$" + amountField)));

        return database.getCollection(collection, Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList()
                .map(docs -> docs.isEmpty() ? 0L : longValue(docs.get(0), "total"));
    }
}
