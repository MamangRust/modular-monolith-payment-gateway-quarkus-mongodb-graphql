package com.sanedge.card.repository.stats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.sanedge.card.entity.Card;
import com.sanedge.card.entity.CardMonthBalance;
import com.sanedge.card.entity.CardYearBalance;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CardBalanceRepository implements ReactivePanacheMongoRepository<Card> {

    private Uni<List<Document>> runSaldosAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("saldos", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    public Uni<List<CardMonthBalance>> getMonthlyBalances(LocalDate yearDate) {
        int y = yearDate.getYear();
        Date start = CardStatsSupport.startOfYear(y);
        Date end = CardStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(CardStatsSupport.baseStages(start, end, "createdAt", "cardNumber"));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$createdAt")),
                Accumulators.sum("totalBalance", "$totalBalance")));

        return runSaldosAggregate(pipeline).map(docs -> {
            long[] totals = new long[13]; // index 1..12
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                totals[m] = CardStatsSupport.longValue(doc, "totalBalance");
            }
            List<CardMonthBalance> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                result.add(new CardMonthBalance(CardStatsSupport.monthAbbr(m), totals[m]));
            }
            return result;
        });
    }

    public Uni<List<CardYearBalance>> getYearlyBalances(Long year) {
        int y = year.intValue();
        int firstYear = y - 4;
        Date start = CardStatsSupport.startOfYear(firstYear);
        Date end = CardStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(CardStatsSupport.baseStages(start, end, "createdAt", "cardNumber"));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$createdAt")),
                Accumulators.sum("totalBalance", "$totalBalance")));

        return runSaldosAggregate(pipeline).map(docs -> {
            long[] totals = new long[5]; // index 0..4 -> year-4 .. year
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 5) {
                    totals[idx] = CardStatsSupport.longValue(doc, "totalBalance");
                }
            }
            List<CardYearBalance> result = new ArrayList<>(5);
            for (int i = 4; i >= 0; i--) {
                result.add(new CardYearBalance(String.valueOf(firstYear + i), totals[i]));
            }
            return result;
        });
    }
}