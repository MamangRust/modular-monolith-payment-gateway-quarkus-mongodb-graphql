package com.sanedge.saldo.repository.stats;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.sanedge.saldo.entity.Saldo;
import com.sanedge.saldo.entity.SaldoMonthBalance;
import com.sanedge.saldo.entity.SaldoYearBalance;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SaldoBalanceRepository implements ReactivePanacheMongoRepository<Saldo> {

    private Uni<List<Document>> runSaldosAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("saldos", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    public Uni<List<SaldoMonthBalance>> findMonthlySaldoBalances(Long year) {
        int y = year.intValue();
        Date start = SaldoStatsSupport.startOfYear(y);
        Date end = SaldoStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(SaldoStatsSupport.baseStages(start, end));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$createdAt")),
                Accumulators.sum("totalBalance", "$totalBalance")));

        return runSaldosAggregate(pipeline).map(docs -> {
            long[] totals = new long[13]; // index 1..12
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                totals[m] = SaldoStatsSupport.longValue(doc, "totalBalance");
            }
            List<SaldoMonthBalance> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                result.add(new SaldoMonthBalance(SaldoStatsSupport.monthAbbr(m), totals[m]));
            }
            return result;
        });
    }

    public Uni<List<SaldoYearBalance>> findYearlySaldoBalances(Long year) {
        int y = year.intValue();
        int firstYear = y - 4;
        Date start = SaldoStatsSupport.startOfYear(firstYear);
        Date end = SaldoStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(SaldoStatsSupport.baseStages(start, end));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$createdAt")),
                Accumulators.sum("totalBalance", "$totalBalance")));

        return runSaldosAggregate(pipeline).map(docs -> {
            long[] totals = new long[5]; // index 0..4 -> year-4 .. year
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 5) {
                    totals[idx] = SaldoStatsSupport.longValue(doc, "totalBalance");
                }
            }
            List<SaldoYearBalance> result = new ArrayList<>(5);
            for (int i = 0; i < 5; i++) {
                result.add(new SaldoYearBalance(String.valueOf(firstYear + i), totals[i]));
            }
            return result;
        });
    }
}
