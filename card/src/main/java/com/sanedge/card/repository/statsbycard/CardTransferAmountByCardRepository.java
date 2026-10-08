package com.sanedge.card.repository.statsbycard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.sanedge.card.entity.Card;
import com.sanedge.card.entity.CardMonthAmount;
import com.sanedge.card.entity.CardYearAmount;
import com.sanedge.card.repository.stats.CardStatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CardTransferAmountByCardRepository implements ReactivePanacheMongoRepository<Card> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("transfers", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    // ---------- Monthly ----------

    public Uni<List<CardMonthAmount>> getMonthlyTransferAmountBySender(String sender, LocalDate yearDate) {
        return monthly(yearDate, "transferFrom", sender);
    }

    public Uni<List<CardMonthAmount>> getMonthlyTransferAmountByReceiver(String receiver, LocalDate yearDate) {
        return monthly(yearDate, "transferTo", receiver);
    }

    private Uni<List<CardMonthAmount>> monthly(LocalDate yearDate, String cardField, String cardNumber) {
        int y = yearDate.getYear();
        Date start = CardStatsSupport.startOfYear(y);
        Date end = CardStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(
                CardStatsSupport.baseStages(start, end, "transferTime", cardField, cardNumber));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$transferTime")),
                Accumulators.sum("totalAmount", "$transferAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] totals = new long[13];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                totals[m] = CardStatsSupport.longValue(doc, "totalAmount");
            }
            List<CardMonthAmount> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                result.add(new CardMonthAmount(CardStatsSupport.monthAbbr(m), totals[m]));
            }
            return result;
        });
    }

    // ---------- Yearly ----------

    public Uni<List<CardYearAmount>> getYearlyTransferAmountBySender(String sender, Long year) {
        return yearly(year, "transferFrom", sender);
    }

    public Uni<List<CardYearAmount>> getYearlyTransferAmountByReceiver(String receiver, Long year) {
        return yearly(year, "transferTo", receiver);
    }

    private Uni<List<CardYearAmount>> yearly(Long year, String cardField, String cardNumber) {
        int y = year.intValue();
        int firstYear = y - 4;
        Date start = CardStatsSupport.startOfYear(firstYear);
        Date end = CardStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(
                CardStatsSupport.baseStages(start, end, "transferTime", cardField, cardNumber));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$transferTime")),
                Accumulators.sum("totalAmount", "$transferAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] totals = new long[5];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 5) {
                    totals[idx] = CardStatsSupport.longValue(doc, "totalAmount");
                }
            }
            List<CardYearAmount> result = new ArrayList<>(5);
            // ORDER BY y.y ASC
            for (int i = 0; i < 5; i++) {
                result.add(new CardYearAmount(String.valueOf(firstYear + i), totals[i]));
            }
            return result;
        });
    }
}
