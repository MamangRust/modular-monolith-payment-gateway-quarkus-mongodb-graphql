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
import com.sanedge.common.enums.Status;
import com.sanedge.transaction.domain.requests.statsbycard.MonthStatusTransactionCardNumber;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.entity.TransactionMonthStatusFailed;
import com.sanedge.transaction.entity.TransactionMonthStatusSuccess;
import com.sanedge.transaction.entity.TransactionYearStatusFailed;
import com.sanedge.transaction.entity.TransactionYearStatusSuccess;
import com.sanedge.transaction.repository.StatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * MongoDB aggregation counterpart of the former PostgreSQL native
 * transaction-status-by-card statistics queries. The SQL {@code cards} join is
 * replaced by an equality filter on the transaction {@code cardNumber}; the
 * zero rows for periods without data are produced in Java.
 */
@ApplicationScoped
public class TransactionStatusByCardRepository implements ReactivePanacheMongoRepository<Transaction> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoCollection().aggregate(pipeline, Document.class).collect().asList();
    }

    private static String firstDayOfMonth(int year, int month) {
        return String.format("%04d-%02d-01T00:00:00Z", year, month);
    }

    private static long longValue(Document doc, String key) {
        return ((Number) doc.getOrDefault(key, 0)).longValue();
    }

    private static int intValue(Document doc, String key) {
        return ((Number) doc.getOrDefault(key, 0)).intValue();
    }

    private Uni<List<Document>> monthlyStatus(String cardNumber, int year, Status status) {
        Date start = Date.from(Instant.parse(firstDayOfMonth(year, 1)));
        Date end = Date.from(Instant.parse(firstDayOfMonth(year + 1, 1)));

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.eq("cardNumber", cardNumber),
                Filters.eq("status", status.name()),
                Filters.gte("transactionTime", start),
                Filters.lt("transactionTime", end))));
        pipeline.add(Aggregates.group(
                new Document("monthIndex", new Document("$month", "$transactionTime")),
                Accumulators.sum("total", 1),
                Accumulators.sum("totalAmount", "$amount")));
        return runAggregate(pipeline);
    }

    private Uni<List<Document>> yearlyStatus(String cardNumber, int year, Status status) {
        Date start = Date.from(Instant.parse(firstDayOfMonth(year - 1, 1)));
        Date end = Date.from(Instant.parse(firstDayOfMonth(year + 1, 1)));

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.eq("cardNumber", cardNumber),
                Filters.eq("status", status.name()),
                Filters.gte("transactionTime", start),
                Filters.lt("transactionTime", end))));
        pipeline.add(Aggregates.group(
                new Document("year",
                        new Document("$dateToString", new Document("format", "%Y").append("date", "$transactionTime"))),
                Accumulators.sum("total", 1),
                Accumulators.sum("totalAmount", "$amount")));
        return runAggregate(pipeline);
    }

    public Uni<List<TransactionMonthStatusSuccess>> findMonthTransactionStatusSuccessByCard(
            MonthStatusTransactionCardNumber req) {
        String cardNumber = req.getCardNumber();
        int year = req.getYear().intValue();
        return monthlyStatus(cardNumber, year, Status.SUCCESS).map(docs -> {
            Map<Integer, Document> index = new HashMap<>();
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                if (id != null && id.getInteger("monthIndex") != null) {
                    index.put(id.getInteger("monthIndex"), doc);
                }
            }
            List<TransactionMonthStatusSuccess> list = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                Document doc = index.get(month);
                list.add(new TransactionMonthStatusSuccess(
                        String.valueOf(year),
                        StatsSupport.monthAbbr(month),
                        doc != null ? intValue(doc, "total") : 0,
                        doc != null ? longValue(doc, "totalAmount") : 0L));
            }
            return list;
        });
    }

    public Uni<List<TransactionMonthStatusFailed>> findMonthTransactionStatusFailedByCard(
            MonthStatusTransactionCardNumber req) {
        String cardNumber = req.getCardNumber();
        int year = req.getYear().intValue();
        return monthlyStatus(cardNumber, year, Status.FAILED).map(docs -> {
            Map<Integer, Document> index = new HashMap<>();
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                if (id != null && id.getInteger("monthIndex") != null) {
                    index.put(id.getInteger("monthIndex"), doc);
                }
            }
            List<TransactionMonthStatusFailed> list = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                Document doc = index.get(month);
                list.add(new TransactionMonthStatusFailed(
                        String.valueOf(year),
                        StatsSupport.monthAbbr(month),
                        doc != null ? intValue(doc, "total") : 0,
                        doc != null ? longValue(doc, "totalAmount") : 0L));
            }
            return list;
        });
    }

    public Uni<List<TransactionYearStatusSuccess>> findYearlyTransactionStatusSuccessByCard(
            String cardNumber, Long year) {
        int y = year.intValue();
        return yearlyStatus(cardNumber, y, Status.SUCCESS).map(docs -> {
            Map<String, Document> index = new HashMap<>();
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                if (id != null && id.getString("year") != null) {
                    index.put(id.getString("year"), doc);
                }
            }
            List<TransactionYearStatusSuccess> list = new ArrayList<>();
            for (int current = y; current >= y - 1; current--) {
                String key = String.valueOf(current);
                Document doc = index.get(key);
                list.add(new TransactionYearStatusSuccess(
                        key,
                        doc != null ? intValue(doc, "total") : 0,
                        doc != null ? longValue(doc, "totalAmount") : 0L));
            }
            return list;
        });
    }

    public Uni<List<TransactionYearStatusFailed>> findYearlyTransactionStatusFailedByCard(
            String cardNumber, Long year) {
        int y = year.intValue();
        return yearlyStatus(cardNumber, y, Status.FAILED).map(docs -> {
            Map<String, Document> index = new HashMap<>();
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                if (id != null && id.getString("year") != null) {
                    index.put(id.getString("year"), doc);
                }
            }
            List<TransactionYearStatusFailed> list = new ArrayList<>();
            for (int current = y; current >= y - 1; current--) {
                String key = String.valueOf(current);
                Document doc = index.get(key);
                list.add(new TransactionYearStatusFailed(
                        key,
                        doc != null ? intValue(doc, "total") : 0,
                        doc != null ? longValue(doc, "totalAmount") : 0L));
            }
            return list;
        });
    }
}
