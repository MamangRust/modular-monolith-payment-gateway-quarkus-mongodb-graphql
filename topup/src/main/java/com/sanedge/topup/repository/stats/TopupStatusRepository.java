package com.sanedge.topup.repository.stats;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.sanedge.topup.entity.Topup;
import com.sanedge.topup.entity.TopupMonthStatusFailed;
import com.sanedge.topup.entity.TopupMonthStatusSuccess;
import com.sanedge.topup.entity.TopupYearStatusFailed;
import com.sanedge.topup.entity.TopupYearStatusSuccess;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TopupStatusRepository implements ReactivePanacheMongoRepository<Topup> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("topups", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    // ---------- Monthly Success ----------

    public Uni<List<TopupMonthStatusSuccess>> findMonthTopupStatusSuccess(
            Long startYear, Integer startMonth, Long endYear, Integer endMonth) {
        int y = startYear.intValue();
        Date start = TopupStatsSupport.startOfYear(y);
        Date end = TopupStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(TopupStatsSupport.baseStages(start, end, null));
        pipeline.add(Aggregates.match(Filters.eq("status", "SUCCESS")));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$topupTime")),
                Accumulators.sum("totalSuccess", 1),
                Accumulators.sum("totalAmount", "$topupAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] counts = new long[13];
            long[] amounts = new long[13];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                counts[m] = TopupStatsSupport.longValue(doc, "totalSuccess");
                amounts[m] = TopupStatsSupport.longValue(doc, "totalAmount");
            }
            List<TopupMonthStatusSuccess> result = new ArrayList<>(12);
            String yearStr = String.valueOf(y);
            for (int m = 1; m <= 12; m++) {
                result.add(new TopupMonthStatusSuccess(
                        yearStr,
                        TopupStatsSupport.monthAbbr(m),
                        (int) counts[m],
                        amounts[m]));
            }
            return result;
        });
    }

    // ---------- Monthly Failed ----------

    public Uni<List<TopupMonthStatusFailed>> findMonthTopupStatusFailed(
            Long startYear, Integer startMonth, Long endYear, Integer endMonth) {
        int y = startYear.intValue();
        Date start = TopupStatsSupport.startOfYear(y);
        Date end = TopupStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(TopupStatsSupport.baseStages(start, end, null));
        pipeline.add(Aggregates.match(Filters.eq("status", "FAILED")));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$topupTime")),
                Accumulators.sum("totalFailed", 1),
                Accumulators.sum("totalAmount", "$topupAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] counts = new long[13];
            long[] amounts = new long[13];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                counts[m] = TopupStatsSupport.longValue(doc, "totalFailed");
                amounts[m] = TopupStatsSupport.longValue(doc, "totalAmount");
            }
            List<TopupMonthStatusFailed> result = new ArrayList<>(12);
            String yearStr = String.valueOf(y);
            for (int m = 1; m <= 12; m++) {
                result.add(new TopupMonthStatusFailed(
                        yearStr,
                        TopupStatsSupport.monthAbbr(m),
                        (int) counts[m],
                        amounts[m]));
            }
            return result;
        });
    }

    // ---------- Yearly Success ----------

    public Uni<List<TopupYearStatusSuccess>> findYearlyTopupStatusSuccess(Long year) {
        int y = year.intValue();
        int firstYear = y - 1;
        Date start = TopupStatsSupport.startOfYear(firstYear);
        Date end = TopupStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(TopupStatsSupport.baseStages(start, end, null));
        pipeline.add(Aggregates.match(Filters.eq("status", "SUCCESS")));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$topupTime")),
                Accumulators.sum("totalSuccess", 1),
                Accumulators.sum("totalAmount", "$topupAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] counts = new long[2];
            long[] amounts = new long[2];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 2) {
                    counts[idx] = TopupStatsSupport.longValue(doc, "totalSuccess");
                    amounts[idx] = TopupStatsSupport.longValue(doc, "totalAmount");
                }
            }
            List<TopupYearStatusSuccess> result = new ArrayList<>(2);
            for (int i = 1; i >= 0; i--) {
                result.add(new TopupYearStatusSuccess(
                        String.valueOf(firstYear + i),
                        (int) counts[i],
                        amounts[i]));
            }
            return result;
        });
    }

    // ---------- Yearly Failed ----------

    public Uni<List<TopupYearStatusFailed>> findYearlyTopupStatusFailed(Long year) {
        int y = year.intValue();
        int firstYear = y - 1;
        Date start = TopupStatsSupport.startOfYear(firstYear);
        Date end = TopupStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(TopupStatsSupport.baseStages(start, end, null));
        pipeline.add(Aggregates.match(Filters.eq("status", "FAILED")));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$topupTime")),
                Accumulators.sum("totalFailed", 1),
                Accumulators.sum("totalAmount", "$topupAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] counts = new long[2];
            long[] amounts = new long[2];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 2) {
                    counts[idx] = TopupStatsSupport.longValue(doc, "totalFailed");
                    amounts[idx] = TopupStatsSupport.longValue(doc, "totalAmount");
                }
            }
            List<TopupYearStatusFailed> result = new ArrayList<>(2);
            for (int i = 1; i >= 0; i--) {
                result.add(new TopupYearStatusFailed(
                        String.valueOf(firstYear + i),
                        (int) counts[i],
                        amounts[i]));
            }
            return result;
        });
    }
}
