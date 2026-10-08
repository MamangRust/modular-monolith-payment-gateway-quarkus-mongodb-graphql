package com.sanedge.saldo.repository.stats;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.sanedge.saldo.entity.Saldo;
import com.sanedge.saldo.entity.SaldoMonthTotalBalance;
import com.sanedge.saldo.entity.SaldoYearTotalBalance;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SaldoTotalAmountRepository implements ReactivePanacheMongoRepository<Saldo> {

    private Uni<List<Document>> runSaldosAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("saldos", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    public Uni<List<SaldoMonthTotalBalance>> findMonthlyTotalSaldoBalance(
            Long startYear, Integer startMonth, Long endYear, Integer endMonth) {
        int y = startYear.intValue();
        Date start = SaldoStatsSupport.startOfYear(y);
        Date end = SaldoStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(SaldoStatsSupport.baseStages(start, end));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$createdAt")),
                Accumulators.sum("totalBalance", "$totalBalance")));

        return runSaldosAggregate(pipeline).map(docs -> {
            long[] totals = new long[13];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                totals[m] = SaldoStatsSupport.longValue(doc, "totalBalance");
            }
            List<SaldoMonthTotalBalance> result = new ArrayList<>(12);
            String yearStr = String.valueOf(y);
            for (int m = 12; m >= 1; m--) {
                result.add(new SaldoMonthTotalBalance(yearStr, SaldoStatsSupport.monthAbbr(m), totals[m]));
            }
            return result;
        });
    }

    public Uni<List<SaldoYearTotalBalance>> findYearlyTotalSaldoBalance(Long year) {
        int y = year.intValue();
        int firstYear = y - 1;
        Date start = SaldoStatsSupport.startOfYear(firstYear);
        Date end = SaldoStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(SaldoStatsSupport.baseStages(start, end));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$createdAt")),
                Accumulators.sum("totalBalance", "$totalBalance")));

        return runSaldosAggregate(pipeline).map(docs -> {
            long[] totals = new long[2]; // index 0..1 -> year-1, year
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 2) {
                    totals[idx] = SaldoStatsSupport.longValue(doc, "totalBalance");
                }
            }
            List<SaldoYearTotalBalance> result = new ArrayList<>(2);
            for (int i = 1; i >= 0; i--) {
                result.add(new SaldoYearTotalBalance(String.valueOf(firstYear + i), totals[i]));
            }
            return result;
        });
    }
}
