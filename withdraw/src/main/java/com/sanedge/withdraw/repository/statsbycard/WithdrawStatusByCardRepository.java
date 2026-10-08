package com.sanedge.withdraw.repository.statsbycard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.sanedge.withdraw.domain.requests.statsbycard.MonthStatusWithdrawCardNumber;
import com.sanedge.withdraw.entity.Withdraw;
import com.sanedge.withdraw.entity.WithdrawMonthStatusFailed;
import com.sanedge.withdraw.entity.WithdrawMonthStatusSuccess;
import com.sanedge.withdraw.entity.WithdrawYearStatusFailed;
import com.sanedge.withdraw.entity.WithdrawYearStatusSuccess;
import com.sanedge.withdraw.repository.stats.WithdrawStatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WithdrawStatusByCardRepository implements ReactivePanacheMongoRepository<Withdraw> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("withdraws", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    public Uni<List<WithdrawMonthStatusSuccess>> findMonthWithdrawStatusSuccessByCard(
            MonthStatusWithdrawCardNumber req) {
        return findMonthWithdrawStatusSuccessByCard(
                req.getCardNumber(), (long) req.getYear(), req.getMonth(),
                (long) req.getYear(), req.getMonth());
    }

    public Uni<List<WithdrawMonthStatusSuccess>> findMonthWithdrawStatusSuccessByCard(
            String cardNumber, Long startYear, Integer startMonth, Long endYear, Integer endMonth) {
        int y = startYear.intValue();
        Date start = WithdrawStatsSupport.startOfYear(y);
        Date end = WithdrawStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(WithdrawStatsSupport.baseStages(start, end, cardNumber));
        pipeline.add(Aggregates.match(Filters.eq("status", "SUCCESS")));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$withdrawTime")),
                Accumulators.sum("totalSuccess", 1),
                Accumulators.sum("totalAmount", "$withdrawAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] counts = new long[13];
            long[] amounts = new long[13];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                counts[m] = WithdrawStatsSupport.longValue(doc, "totalSuccess");
                amounts[m] = WithdrawStatsSupport.longValue(doc, "totalAmount");
            }
            List<WithdrawMonthStatusSuccess> result = new ArrayList<>(12);
            String yearStr = String.valueOf(y);
            for (int m = 1; m <= 12; m++) {
                result.add(new WithdrawMonthStatusSuccess(
                        yearStr,
                        WithdrawStatsSupport.monthAbbr(m),
                        (int) counts[m],
                        amounts[m]));
            }
            return result;
        });
    }

    public Uni<List<WithdrawMonthStatusFailed>> findMonthWithdrawStatusFailedByCard(
            MonthStatusWithdrawCardNumber req) {
        return findMonthWithdrawStatusFailedByCard(
                req.getCardNumber(), (long) req.getYear(), req.getMonth(),
                (long) req.getYear(), req.getMonth());
    }

    public Uni<List<WithdrawMonthStatusFailed>> findMonthWithdrawStatusFailedByCard(
            String cardNumber, Long startYear, Integer startMonth, Long endYear, Integer endMonth) {
        int y = startYear.intValue();
        Date start = WithdrawStatsSupport.startOfYear(y);
        Date end = WithdrawStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(WithdrawStatsSupport.baseStages(start, end, cardNumber));
        pipeline.add(Aggregates.match(Filters.eq("status", "FAILED")));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$withdrawTime")),
                Accumulators.sum("totalFailed", 1),
                Accumulators.sum("totalAmount", "$withdrawAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] counts = new long[13];
            long[] amounts = new long[13];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                counts[m] = WithdrawStatsSupport.longValue(doc, "totalFailed");
                amounts[m] = WithdrawStatsSupport.longValue(doc, "totalAmount");
            }
            List<WithdrawMonthStatusFailed> result = new ArrayList<>(12);
            String yearStr = String.valueOf(y);
            for (int m = 1; m <= 12; m++) {
                result.add(new WithdrawMonthStatusFailed(
                        yearStr,
                        WithdrawStatsSupport.monthAbbr(m),
                        (int) counts[m],
                        amounts[m]));
            }
            return result;
        });
    }

    public Uni<List<WithdrawYearStatusSuccess>> findYearlyWithdrawStatusSuccessByCard(
            String cardNumber, Long year) {
        int y = year.intValue();
        int firstYear = y - 1;
        Date start = WithdrawStatsSupport.startOfYear(firstYear);
        Date end = WithdrawStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(WithdrawStatsSupport.baseStages(start, end, cardNumber));
        pipeline.add(Aggregates.match(Filters.eq("status", "SUCCESS")));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$withdrawTime")),
                Accumulators.sum("totalSuccess", 1),
                Accumulators.sum("totalAmount", "$withdrawAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] counts = new long[2];
            long[] amounts = new long[2];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 2) {
                    counts[idx] = WithdrawStatsSupport.longValue(doc, "totalSuccess");
                    amounts[idx] = WithdrawStatsSupport.longValue(doc, "totalAmount");
                }
            }
            List<WithdrawYearStatusSuccess> result = new ArrayList<>(2);
            for (int i = 1; i >= 0; i--) {
                result.add(new WithdrawYearStatusSuccess(
                        String.valueOf(firstYear + i),
                        (int) counts[i],
                        amounts[i]));
            }
            return result;
        });
    }

    public Uni<List<WithdrawYearStatusFailed>> findYearlyWithdrawStatusFailedByCard(
            String cardNumber, Long year) {
        int y = year.intValue();
        int firstYear = y - 1;
        Date start = WithdrawStatsSupport.startOfYear(firstYear);
        Date end = WithdrawStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(WithdrawStatsSupport.baseStages(start, end, cardNumber));
        pipeline.add(Aggregates.match(Filters.eq("status", "FAILED")));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$withdrawTime")),
                Accumulators.sum("totalFailed", 1),
                Accumulators.sum("totalAmount", "$withdrawAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] counts = new long[2];
            long[] amounts = new long[2];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 2) {
                    counts[idx] = WithdrawStatsSupport.longValue(doc, "totalFailed");
                    amounts[idx] = WithdrawStatsSupport.longValue(doc, "totalAmount");
                }
            }
            List<WithdrawYearStatusFailed> result = new ArrayList<>(2);
            for (int i = 1; i >= 0; i--) {
                result.add(new WithdrawYearStatusFailed(
                        String.valueOf(firstYear + i),
                        (int) counts[i],
                        amounts[i]));
            }
            return result;
        });
    }
}
