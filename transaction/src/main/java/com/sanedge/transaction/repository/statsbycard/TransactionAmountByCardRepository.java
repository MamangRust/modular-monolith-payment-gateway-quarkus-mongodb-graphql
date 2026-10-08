package com.sanedge.transaction.repository.statsbycard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.entity.TransactionMonthAmount;
import com.sanedge.transaction.entity.TransactionYearAmount;
import com.sanedge.transaction.repository.StatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * MongoDB aggregation counterpart of the former PostgreSQL native
 * transaction-amount-by-card statistics queries. The SQL {@code cards} join is
 * replaced by an equality filter on the transaction {@code cardNumber}; the
 * zero rows for periods without data are produced in Java.
 */
@ApplicationScoped
public class TransactionAmountByCardRepository implements ReactivePanacheMongoRepository<Transaction> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoCollection().aggregate(pipeline, Document.class).collect().asList();
    }

    private static String firstDayOfMonth(int year, int month) {
        return String.format("%04d-%02d-01T00:00:00Z", year, month);
    }

    private static long longValue(Document doc, String key) {
        return ((Number) doc.getOrDefault(key, 0)).longValue();
    }

    public Uni<List<TransactionMonthAmount>> findMonthlyAmountsByCard(String cardNumber, Long year) {
        int y = year.intValue();
        Date start = Date.from(Instant.parse(firstDayOfMonth(y, 1)));
        Date end = Date.from(Instant.parse(firstDayOfMonth(y + 1, 1)));

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.eq("cardNumber", cardNumber),
                Filters.gte("transactionTime", start),
                Filters.lt("transactionTime", end))));
        pipeline.add(Aggregates.group(
                new Document("monthIndex", new Document("$month", "$transactionTime")),
                Accumulators.sum("totalAmount", "$amount")));

        return runAggregate(pipeline).map(docs -> {
            Map<Integer, Long> byMonth = new HashMap<>();
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                if (id != null && id.getInteger("monthIndex") != null) {
                    byMonth.put(id.getInteger("monthIndex"), longValue(doc, "totalAmount"));
                }
            }
            List<TransactionMonthAmount> list = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                list.add(new TransactionMonthAmount(StatsSupport.monthAbbr(month), byMonth.getOrDefault(month, 0L)));
            }
            return list;
        });
    }

    public Uni<List<TransactionYearAmount>> findYearlyAmountsByCard(String cardNumber, Long endYear) {
        int y = endYear.intValue();
        int startYear = y - 4;
        Date start = Date.from(Instant.parse(firstDayOfMonth(startYear, 1)));
        Date end = Date.from(Instant.parse(firstDayOfMonth(y + 1, 1)));

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.eq("cardNumber", cardNumber),
                Filters.gte("transactionTime", start),
                Filters.lt("transactionTime", end))));
        pipeline.add(Aggregates.group(
                new Document("year",
                        new Document("$dateToString", new Document("format", "%Y").append("date", "$transactionTime"))),
                Accumulators.sum("totalAmount", "$amount")));

        return runAggregate(pipeline).map(docs -> {
            Map<String, Long> byYear = new HashMap<>();
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                if (id != null && id.getString("year") != null) {
                    byYear.put(id.getString("year"), longValue(doc, "totalAmount"));
                }
            }
            List<TransactionYearAmount> list = new ArrayList<>();
            for (int current = startYear; current <= y; current++) {
                String key = String.valueOf(current);
                list.add(new TransactionYearAmount(key, byYear.getOrDefault(key, 0L)));
            }
            return list;
        });
    }
}
