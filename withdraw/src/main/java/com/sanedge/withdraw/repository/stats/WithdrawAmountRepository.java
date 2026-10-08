package com.sanedge.withdraw.repository.stats;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.sanedge.withdraw.entity.Withdraw;
import com.sanedge.withdraw.entity.WithdrawMonthlyAmount;
import com.sanedge.withdraw.entity.WithdrawYearlyAmount;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WithdrawAmountRepository implements ReactivePanacheMongoRepository<Withdraw> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("withdraws", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    public Uni<List<WithdrawMonthlyAmount>> findMonthlyWithdraws(Long year) {
        int y = year.intValue();
        Date start = WithdrawStatsSupport.startOfYear(y);
        Date end = WithdrawStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(WithdrawStatsSupport.baseStages(start, end, null));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$withdrawTime")),
                Accumulators.sum("totalAmount", "$withdrawAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] totals = new long[13];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                totals[m] = WithdrawStatsSupport.longValue(doc, "totalAmount");
            }
            List<WithdrawMonthlyAmount> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                result.add(new WithdrawMonthlyAmount(WithdrawStatsSupport.monthAbbr(m), totals[m]));
            }
            return result;
        });
    }

    public Uni<List<WithdrawYearlyAmount>> findYearlyWithdraws(Long year) {
        int y = year.intValue();
        int firstYear = y - 4;
        Date start = WithdrawStatsSupport.startOfYear(firstYear);
        Date end = WithdrawStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(WithdrawStatsSupport.baseStages(start, end, null));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$withdrawTime")),
                Accumulators.sum("totalAmount", "$withdrawAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] totals = new long[5];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 5) {
                    totals[idx] = WithdrawStatsSupport.longValue(doc, "totalAmount");
                }
            }
            List<WithdrawYearlyAmount> result = new ArrayList<>(5);
            for (int i = 0; i < 5; i++) {
                result.add(new WithdrawYearlyAmount(String.valueOf(firstYear + i), totals[i]));
            }
            return result;
        });
    }
}
