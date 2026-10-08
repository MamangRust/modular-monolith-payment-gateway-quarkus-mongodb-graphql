package com.sanedge.merchant.repository.stats;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.entity.MerchantMonthlyAmount;
import com.sanedge.merchant.entity.MerchantYearlyAmount;
import com.sanedge.merchant.repository.StatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * MongoDB aggregation counterpart of the former PostgreSQL native monthly /
 * yearly merchant amount queries. The {@code WITH months} skeleton and the
 * year range are produced in Java; the aggregation only returns the months /
 * years that actually have transactions.
 */
@ApplicationScoped
public class MerchantAmountRepository implements ReactivePanacheMongoRepository<Merchant> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("transactions", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    public Uni<List<MerchantMonthlyAmount>> findMonthlyAmount(Long year) {
        int y = year.intValue();
        Date start = StatsSupport.startOfYear(y);
        Date end = StatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.gte("transactionTime", start),
                Filters.lt("transactionTime", end))));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$transactionTime")),
                Accumulators.sum("totalAmount", "$amount")));

        return runAggregate(pipeline).map(docs -> {
            long[] totals = new long[13]; // index 1..12
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                totals[m] = StatsSupport.longValue(doc, "totalAmount");
            }
            List<MerchantMonthlyAmount> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                result.add(new MerchantMonthlyAmount(StatsSupport.monthAbbr(m), totals[m]));
            }
            return result;
        });
    }

    public Uni<List<MerchantYearlyAmount>> findYearlyAmount(Long year) {
        int y = year.intValue();
        int firstYear = y - 4;
        Date start = StatsSupport.startOfYear(firstYear);
        Date end = StatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.gte("transactionTime", start),
                Filters.lt("transactionTime", end))));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$transactionTime")),
                Accumulators.sum("totalAmount", "$amount")));

        return runAggregate(pipeline).map(docs -> {
            long[] totals = new long[5]; // index 0..4 -> year-4 .. year
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 5) {
                    totals[idx] = StatsSupport.longValue(doc, "totalAmount");
                }
            }
            List<MerchantYearlyAmount> result = new ArrayList<>(5);
            for (int i = 0; i < 5; i++) {
                result.add(new MerchantYearlyAmount(String.valueOf(firstYear + i), totals[i]));
            }
            return result;
        });
    }
}
