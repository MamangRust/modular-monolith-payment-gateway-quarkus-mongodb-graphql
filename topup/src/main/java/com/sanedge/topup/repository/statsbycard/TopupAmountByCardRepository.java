package com.sanedge.topup.repository.statsbycard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.sanedge.topup.entity.Topup;
import com.sanedge.topup.entity.TopupMonthAmount;
import com.sanedge.topup.entity.TopupYearAmount;
import com.sanedge.topup.repository.stats.TopupStatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TopupAmountByCardRepository implements ReactivePanacheMongoRepository<Topup> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("topups", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    public Uni<List<TopupMonthAmount>> findMonthlyTopupAmountsByCard(String cardNumber, Long year) {
        int y = year.intValue();
        Date start = TopupStatsSupport.startOfYear(y);
        Date end = TopupStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(TopupStatsSupport.baseStages(start, end, cardNumber));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$topupTime")),
                Accumulators.sum("totalAmount", "$topupAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] totals = new long[13];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                totals[m] = TopupStatsSupport.longValue(doc, "totalAmount");
            }
            List<TopupMonthAmount> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                result.add(new TopupMonthAmount(TopupStatsSupport.monthAbbr(m), totals[m]));
            }
            return result;
        });
    }

    public Uni<List<TopupYearAmount>> findYearlyTopupAmountsByCard(String cardNumber, Long endYear) {
        int y = endYear.intValue();
        int firstYear = y - 4;
        Date start = TopupStatsSupport.startOfYear(firstYear);
        Date end = TopupStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(TopupStatsSupport.baseStages(start, end, cardNumber));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$topupTime")),
                Accumulators.sum("totalAmount", "$topupAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] totals = new long[5];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 5) {
                    totals[idx] = TopupStatsSupport.longValue(doc, "totalAmount");
                }
            }
            List<TopupYearAmount> result = new ArrayList<>(5);
            for (int i = 0; i < 5; i++) {
                result.add(new TopupYearAmount(String.valueOf(firstYear + i), totals[i]));
            }
            return result;
        });
    }
}
