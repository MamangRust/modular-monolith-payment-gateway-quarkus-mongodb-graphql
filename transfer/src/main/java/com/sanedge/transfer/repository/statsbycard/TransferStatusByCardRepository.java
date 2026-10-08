package com.sanedge.transfer.repository.statsbycard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.sanedge.transfer.domain.requests.statsbycard.MonthStatusTransferCardNumber;
import com.sanedge.transfer.entity.Transfer;
import com.sanedge.transfer.entity.TransferMonthStatusFailed;
import com.sanedge.transfer.entity.TransferMonthStatusSuccess;
import com.sanedge.transfer.entity.TransferYearStatusFailed;
import com.sanedge.transfer.entity.TransferYearStatusSuccess;
import com.sanedge.transfer.repository.stats.TransferStatsSupport;
import com.sanedge.transfer.repository.stats.TransferStatsSupport.CardMode;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransferStatusByCardRepository implements ReactivePanacheMongoRepository<Transfer> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("transfers", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    // ---------- Monthly ----------

    public Uni<List<TransferMonthStatusSuccess>> findMonthTransferStatusSuccessByCard(
            MonthStatusTransferCardNumber req) {
        return monthly(req, "SUCCESS", true);
    }

    public Uni<List<TransferMonthStatusFailed>> findMonthTransferStatusFailedByCard(
            MonthStatusTransferCardNumber req) {
        return monthly(req, "FAILED", false);
    }

    @SuppressWarnings("unchecked")
    private <T> Uni<List<T>> monthly(MonthStatusTransferCardNumber req,
                                     String status, boolean isSuccess) {
        String cardNumber = req.getCardNumber();
        int y = req.getYear();
        Date start = TransferStatsSupport.startOfYear(y);
        Date end = TransferStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(
                TransferStatsSupport.baseStages(start, end, cardNumber, CardMode.SENDER_OR_RECEIVER));
        pipeline.add(Aggregates.match(Filters.eq("status", status)));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$transferTime")),
                Accumulators.sum("cnt", 1),
                Accumulators.sum("totalAmount", "$transferAmount")));

        String yearStr = String.valueOf(y);

        return (Uni<List<T>>) (Uni<?>) runAggregate(pipeline).map(docs -> {
            long[] counts = new long[13];
            long[] amounts = new long[13];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                counts[m] = TransferStatsSupport.longValue(doc, "cnt");
                amounts[m] = TransferStatsSupport.longValue(doc, "totalAmount");
            }
            List<Object> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                String abbr = TransferStatsSupport.monthAbbr(m);
                if (isSuccess) {
                    result.add(new TransferMonthStatusSuccess(yearStr, abbr, (int) counts[m], amounts[m]));
                } else {
                    result.add(new TransferMonthStatusFailed(yearStr, abbr, (int) counts[m], amounts[m]));
                }
            }
            return (List<T>) result;
        });
    }

    // ---------- Yearly ----------

    public Uni<List<TransferYearStatusSuccess>> findYearlyTransferStatusSuccessByCard(
            String cardNumber, Long year) {
        return yearly(cardNumber, year, "SUCCESS", true);
    }

    public Uni<List<TransferYearStatusFailed>> findYearlyTransferStatusFailedByCard(
            String cardNumber, Long year) {
        return yearly(cardNumber, year, "FAILED", false);
    }

    @SuppressWarnings("unchecked")
    private <T> Uni<List<T>> yearly(String cardNumber, Long year,
                                    String status, boolean isSuccess) {
        int y = year.intValue();
        int firstYear = y - 1;
        Date start = TransferStatsSupport.startOfYear(firstYear);
        Date end = TransferStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(
                TransferStatsSupport.baseStages(start, end, cardNumber, CardMode.SENDER_OR_RECEIVER));
        pipeline.add(Aggregates.match(Filters.eq("status", status)));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$transferTime")),
                Accumulators.sum("cnt", 1),
                Accumulators.sum("totalAmount", "$transferAmount")));

        return (Uni<List<T>>) (Uni<?>) runAggregate(pipeline).map(docs -> {
            long[] counts = new long[2];
            long[] amounts = new long[2];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 2) {
                    counts[idx] = TransferStatsSupport.longValue(doc, "cnt");
                    amounts[idx] = TransferStatsSupport.longValue(doc, "totalAmount");
                }
            }
            List<Object> result = new ArrayList<>(2);
            // ORDER BY reportYear DESC -> [year, year-1]
            for (int i = 1; i >= 0; i--) {
                String yStr = String.valueOf(firstYear + i);
                if (isSuccess) {
                    result.add(new TransferYearStatusSuccess(yStr, (int) counts[i], amounts[i]));
                } else {
                    result.add(new TransferYearStatusFailed(yStr, (int) counts[i], amounts[i]));
                }
            }
            return (List<T>) result;
        });
    }
}
