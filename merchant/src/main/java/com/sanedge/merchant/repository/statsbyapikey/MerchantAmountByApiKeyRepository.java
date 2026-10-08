package com.sanedge.merchant.repository.statsbyapikey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

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
 * MongoDB aggregation counterpart of the former PostgreSQL native
 * api-key-scoped monthly / yearly amount queries.
 */
@ApplicationScoped
public class MerchantAmountByApiKeyRepository implements ReactivePanacheMongoRepository<Merchant> {

    private static final ObjectId NO_MERCHANT = new ObjectId("000000000000000000000000");

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("transactions", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    private Uni<ObjectId> resolveMerchant(String apiKey) {
        if (apiKey == null) {
            return Uni.createFrom().item(NO_MERCHANT);
        }
        return find("{ 'apiKey': ?1, 'deletedAt': null }", apiKey).firstResult()
                .map(merchant -> merchant != null ? merchant.id : NO_MERCHANT);
    }

    public Uni<List<MerchantMonthlyAmount>> findMonthlyAmountByApiKey(String apiKey, Long year) {
        int y = year.intValue();
        Date start = StatsSupport.startOfYear(y);
        Date end = StatsSupport.startOfYear(y + 1);

        return resolveMerchant(apiKey).chain(resolved -> {
            List<Bson> pipeline = new ArrayList<>();
            pipeline.add(Aggregates.match(Filters.and(
                    Filters.eq("deletedAt", null),
                    Filters.eq("merchantId", resolved),
                    Filters.gte("transactionTime", start),
                    Filters.lt("transactionTime", end))));
            pipeline.add(Aggregates.group(
                    new Document("month", new Document("$month", "$transactionTime")),
                    Accumulators.sum("totalAmount", "$amount")));

            return runAggregate(pipeline).map(docs -> {
                long[] totals = new long[13];
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
        });
    }

    public Uni<List<MerchantYearlyAmount>> findYearlyAmountByApiKey(String apiKey, Long endYear) {
        int y = endYear.intValue();
        int firstYear = y - 4;
        Date start = StatsSupport.startOfYear(firstYear);
        Date end = StatsSupport.startOfYear(y + 1);

        return resolveMerchant(apiKey).chain(resolved -> {
            List<Bson> pipeline = new ArrayList<>();
            pipeline.add(Aggregates.match(Filters.and(
                    Filters.eq("deletedAt", null),
                    Filters.eq("merchantId", resolved),
                    Filters.gte("transactionTime", start),
                    Filters.lt("transactionTime", end))));
            pipeline.add(Aggregates.group(
                    new Document("year", new Document("$year", "$transactionTime")),
                    Accumulators.sum("totalAmount", "$amount")));

            return runAggregate(pipeline).map(docs -> {
                long[] totals = new long[5];
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
        });
    }
}
