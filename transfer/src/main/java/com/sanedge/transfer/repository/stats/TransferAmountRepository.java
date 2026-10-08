package com.sanedge.transfer.repository.stats;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.sanedge.transfer.entity.Transfer;
import com.sanedge.transfer.entity.TransferMonthAmount;
import com.sanedge.transfer.entity.TransferYearAmount;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransferAmountRepository implements ReactivePanacheMongoRepository<Transfer> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("transfers", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    public Uni<List<TransferMonthAmount>> findMonthlyTransferAmounts(Long year) {
        int y = year.intValue();
        Date start = TransferStatsSupport.startOfYear(y);
        Date end = TransferStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(TransferStatsSupport.baseStages(start, end));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$transferTime")),
                Accumulators.sum("totalAmount", "$transferAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] totals = new long[13];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int m = ((Number) id.get("month")).intValue();
                totals[m] = TransferStatsSupport.longValue(doc, "totalAmount");
            }
            List<TransferMonthAmount> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                result.add(new TransferMonthAmount(TransferStatsSupport.monthAbbr(m), totals[m]));
            }
            return result;
        });
    }

    public Uni<List<TransferYearAmount>> findYearlyTransferAmounts(Long year) {
        int y = year.intValue();
        int firstYear = y - 4;
        Date start = TransferStatsSupport.startOfYear(firstYear);
        Date end = TransferStatsSupport.startOfYear(y + 1);

        List<Bson> pipeline = new ArrayList<>(TransferStatsSupport.baseStages(start, end));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$transferTime")),
                Accumulators.sum("totalAmount", "$transferAmount")));

        return runAggregate(pipeline).map(docs -> {
            long[] totals = new long[5];
            for (Document doc : docs) {
                Document id = doc.get("_id", Document.class);
                int yr = ((Number) id.get("year")).intValue();
                int idx = yr - firstYear;
                if (idx >= 0 && idx < 5) {
                    totals[idx] = TransferStatsSupport.longValue(doc, "totalAmount");
                }
            }
            List<TransferYearAmount> result = new ArrayList<>(5);
            for (int i = 0; i < 5; i++) {
                result.add(new TransferYearAmount(String.valueOf(firstYear + i), totals[i]));
            }
            return result;
        });
    }
}
