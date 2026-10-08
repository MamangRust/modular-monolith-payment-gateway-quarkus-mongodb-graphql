package com.sanedge.transaction.repository.stats;

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
import com.mongodb.client.model.Sorts;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.entity.TransactionMonthMethod;
import com.sanedge.transaction.entity.TransactionYearMethod;
import com.sanedge.transaction.repository.StatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * MongoDB aggregation counterpart of the former PostgreSQL native
 * transaction-method statistics queries. The SQL {@code payment_methods} CTE
 * (distinct methods) and the {@code months}/{@code years} cross joins are
 * rewritten as a distinct-methods aggregation plus a {@code $group}
 * aggregation, with the month/year × method cross product built in Java so
 * periods with no data still yield zero rows.
 */
@ApplicationScoped
public class TransactionMethodRepository implements ReactivePanacheMongoRepository<Transaction> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoCollection().aggregate(pipeline, Document.class).collect().asList();
    }

    private static String firstDayOfMonth(int year, int month) {
        return String.format("%04d-%02d-01T00:00:00Z", year, month);
    }

    private Uni<List<String>> distinctPaymentMethods() {
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.eq("deletedAt", null)));
        pipeline.add(Aggregates.group("$paymentMethod"));
        pipeline.add(Aggregates.sort(Sorts.ascending("_id")));
        return runAggregate(pipeline).map(docs -> {
            List<String> list = new ArrayList<>();
            for (Document doc : docs) {
                String method = doc.getString("_id");
                if (method != null) {
                    list.add(method);
                }
            }
            return list;
        });
    }

    private Uni<List<Document>> monthlyTransactions(int year, Date start, Date end) {
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.gte("transactionTime", start),
                Filters.lt("transactionTime", end))));
        pipeline.add(Aggregates.group(
                new Document("monthIndex", new Document("$month", "$transactionTime"))
                        .append("paymentMethod", "$paymentMethod"),
                Accumulators.sum("totalTransactions", 1),
                Accumulators.sum("totalAmount", "$amount")));
        return runAggregate(pipeline);
    }

    private Uni<List<Document>> yearlyTransactions(int startYear, int endYear) {
        Date start = Date.from(Instant.parse(firstDayOfMonth(startYear, 1)));
        Date end = Date.from(Instant.parse(firstDayOfMonth(endYear + 1, 1)));

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.gte("transactionTime", start),
                Filters.lt("transactionTime", end))));
        pipeline.add(Aggregates.group(
                new Document("year",
                        new Document("$dateToString", new Document("format", "%Y").append("date", "$transactionTime")))
                        .append("paymentMethod", "$paymentMethod"),
                Accumulators.sum("totalTransactions", 1),
                Accumulators.sum("totalAmount", "$amount")));
        return runAggregate(pipeline);
    }

    private static String monthKey(int monthIndex, String method) {
        return monthIndex + "|" + method;
    }

    private List<TransactionMonthMethod> buildMonthly(List<String> methods, List<Document> docs, int year) {
        Map<String, Document> index = new HashMap<>();
        for (Document doc : docs) {
            Document id = doc.get("_id", Document.class);
            if (id != null && id.getInteger("monthIndex") != null) {
                index.put(monthKey(id.getInteger("monthIndex"), id.getString("paymentMethod")), doc);
            }
        }

        List<TransactionMonthMethod> list = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            for (String method : methods) {
                Document doc = index.get(monthKey(month, method));
                int total = doc != null ? ((Number) doc.getOrDefault("totalTransactions", 0)).intValue() : 0;
                long amount = doc != null ? ((Number) doc.getOrDefault("totalAmount", 0)).longValue() : 0L;
                list.add(new TransactionMonthMethod(StatsSupport.monthAbbr(month), method, total, amount));
            }
        }
        return list;
    }

    private List<TransactionYearMethod> buildYearly(List<String> methods, List<Document> docs,
            int startYear, int endYear) {
        Map<String, Document> index = new HashMap<>();
        for (Document doc : docs) {
            Document id = doc.get("_id", Document.class);
            if (id != null && id.getString("year") != null) {
                index.put(id.getString("year") + "|" + id.getString("paymentMethod"), doc);
            }
        }

        List<TransactionYearMethod> list = new ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            String yearKey = String.valueOf(year);
            for (String method : methods) {
                Document doc = index.get(yearKey + "|" + method);
                int total = doc != null ? ((Number) doc.getOrDefault("totalTransactions", 0)).intValue() : 0;
                long amount = doc != null ? ((Number) doc.getOrDefault("totalAmount", 0)).longValue() : 0L;
                list.add(new TransactionYearMethod(yearKey, method, total, amount));
            }
        }
        return list;
    }

    public Uni<List<TransactionMonthMethod>> findMonthlyPaymentMethods(Long year) {
        int y = year.intValue();
        Date start = Date.from(Instant.parse(firstDayOfMonth(y, 1)));
        Date end = Date.from(Instant.parse(firstDayOfMonth(y + 1, 1)));

        return Uni.combine().all()
                .unis(distinctPaymentMethods(), monthlyTransactions(y, start, end))
                .asTuple()
                .map(tuple -> buildMonthly(tuple.getItem1(), tuple.getItem2(), y));
    }

    public Uni<List<TransactionYearMethod>> findYearlyPaymentMethods(Long year) {
        int y = year.intValue();
        int startYear = y - 4;

        return Uni.combine().all()
                .unis(distinctPaymentMethods(), yearlyTransactions(startYear, y))
                .asTuple()
                .map(tuple -> buildYearly(tuple.getItem1(), tuple.getItem2(), startYear, y));
    }
}
