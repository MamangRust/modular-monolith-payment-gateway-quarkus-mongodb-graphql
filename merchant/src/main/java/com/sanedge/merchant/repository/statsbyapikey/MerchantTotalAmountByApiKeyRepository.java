package com.sanedge.merchant.repository.statsbyapikey;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.entity.MerchantMonthlyTotalAmount;
import com.sanedge.merchant.entity.MerchantYearlyTotalAmount;
import com.sanedge.merchant.repository.StatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * MongoDB aggregation counterpart of the former PostgreSQL native
 * api-key-scoped total-amount statistics.
 */
@ApplicationScoped
public class MerchantTotalAmountByApiKeyRepository implements ReactivePanacheMongoRepository<Merchant> {

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

    public Uni<List<MerchantMonthlyTotalAmount>> findMonthlyTotalAmountByApiKey(String apiKey, LocalDate date) {
        YearMonth current = YearMonth.from(date);
        YearMonth previous = current.minusMonths(1);

        Date start = StatsSupport.firstDayOfMonth(previous.getYear(), previous.getMonthValue());
        Date end = StatsSupport.firstDayOfMonth(current.plusMonths(1).getYear(),
                current.plusMonths(1).getMonthValue());

        return resolveMerchant(apiKey).chain(resolved -> {
            List<Bson> pipeline = new ArrayList<>();
            pipeline.add(Aggregates.match(Filters.and(
                    Filters.eq("deletedAt", null),
                    Filters.eq("merchantId", resolved),
                    Filters.gte("transactionTime", start),
                    Filters.lt("transactionTime", end))));
            pipeline.add(Aggregates.group(
                    new Document("year", new Document("$year", "$transactionTime"))
                            .append("month", new Document("$month", "$transactionTime")),
                    Accumulators.sum("totalAmount", "$amount")));

            return runAggregate(pipeline).map(docs -> {
                Map<Long, MerchantMonthlyTotalAmount> byMonth = new LinkedHashMap<>();
                for (Document doc : docs) {
                    Document id = doc.get("_id", Document.class);
                    int yr = ((Number) id.get("year")).intValue();
                    int m = ((Number) id.get("month")).intValue();
                    byMonth.put(sortKey(yr, m), new MerchantMonthlyTotalAmount(
                            String.valueOf(yr), StatsSupport.monthAbbr(m), StatsSupport.longValue(doc, "totalAmount")));
                }
                byMonth.putIfAbsent(sortKey(current.getYear(), current.getMonthValue()),
                        new MerchantMonthlyTotalAmount(String.valueOf(current.getYear()),
                                StatsSupport.monthAbbr(current.getMonthValue()), 0L));
                byMonth.putIfAbsent(sortKey(previous.getYear(), previous.getMonthValue()),
                        new MerchantMonthlyTotalAmount(String.valueOf(previous.getYear()),
                                StatsSupport.monthAbbr(previous.getMonthValue()), 0L));

                List<MerchantMonthlyTotalAmount> result = new ArrayList<>(byMonth.values());
                result.sort((a, b) -> Long.compare(
                        sortKey(Integer.parseInt(b.getYear()), StatsSupport.monthIndex(b.getMonth())),
                        sortKey(Integer.parseInt(a.getYear()), StatsSupport.monthIndex(a.getMonth()))));
                return result;
            });
        });
    }

    public Uni<List<MerchantYearlyTotalAmount>> findYearlyTotalAmountByApiKey(String apiKey, Long year,
            Long previousYear) {
        int y = year.intValue();
        int prev = previousYear.intValue();

        return resolveMerchant(apiKey).chain(resolved -> {
            List<Bson> pipeline = new ArrayList<>();
            pipeline.add(Aggregates.match(Filters.and(
                    Filters.eq("deletedAt", null),
                    Filters.eq("merchantId", resolved),
                    Filters.or(
                            Filters.and(Filters.gte("transactionTime", StatsSupport.startOfYear(y)),
                                    Filters.lt("transactionTime", StatsSupport.startOfYear(y + 1))),
                            Filters.and(Filters.gte("transactionTime", StatsSupport.startOfYear(prev)),
                                    Filters.lt("transactionTime", StatsSupport.startOfYear(prev + 1)))))));
            pipeline.add(Aggregates.group(
                    new Document("year", new Document("$year", "$transactionTime")),
                    Accumulators.sum("totalAmount", "$amount")));

            return runAggregate(pipeline).map(docs -> {
                Map<Integer, MerchantYearlyTotalAmount> byYear = new LinkedHashMap<>();
                for (Document doc : docs) {
                    Document id = doc.get("_id", Document.class);
                    int yr = ((Number) id.get("year")).intValue();
                    byYear.put(yr, new MerchantYearlyTotalAmount(String.valueOf(yr),
                            StatsSupport.longValue(doc, "totalAmount")));
                }
                byYear.putIfAbsent(y, new MerchantYearlyTotalAmount(String.valueOf(y), 0L));
                byYear.putIfAbsent(prev, new MerchantYearlyTotalAmount(String.valueOf(prev), 0L));

                List<MerchantYearlyTotalAmount> result = new ArrayList<>(byYear.values());
                result.sort((a, b) -> b.getYear().compareTo(a.getYear()));
                return result;
            });
        });
    }

    private static long sortKey(int year, int month) {
        return year * 100L + month;
    }
}
