package com.sanedge.topup.repository.statsbycard;

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
import com.sanedge.topup.entity.Topup;
import com.sanedge.topup.entity.TopupMonthMethod;
import com.sanedge.topup.entity.TopupYearMethod;
import com.sanedge.topup.repository.stats.TopupStatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TopupMethodByCardRepository implements ReactivePanacheMongoRepository<Topup> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("topups", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    private Uni<Boolean> isCardValid(String cardNumber) {
        return mongoDatabase()
                .getCollection("cards", Document.class)
                .find(Filters.and(
                        Filters.eq("cardNumber", cardNumber),
                        Filters.eq("deletedAt", null)))
                .toUni()
                .map(doc -> doc != null);
    }

    private Uni<List<String>> distinctMethodsForCard(String cardNumber) {
        return mongoCollection()
                .distinct("topupMethod",
                        Filters.and(
                                Filters.eq("cardNumber", cardNumber),
                                Filters.eq("deletedAt", null)),
                        String.class)
                .collect().asList();
    }

    public Uni<List<TopupMonthMethod>> findMonthlyTopupMethodsByCard(String cardNumber, Long year) {
        int y = year.intValue();
        Date start = TopupStatsSupport.startOfYear(y);
        Date end = TopupStatsSupport.startOfYear(y + 1);

        return isCardValid(cardNumber).chain(valid -> {
            if (!valid) {
                return Uni.createFrom().item(new ArrayList<TopupMonthMethod>());
            }
            return distinctMethodsForCard(cardNumber).chain(methods -> {
                List<Bson> pipeline = new ArrayList<>(TopupStatsSupport.baseStages(start, end, cardNumber));
                pipeline.add(Aggregates.group(
                        new Document("month", new Document("$month", "$topupTime"))
                                .append("method", "$topupMethod"),
                        Accumulators.sum("totalTopups", 1),
                        Accumulators.sum("totalAmount", "$topupAmount")));

                return runAggregate(pipeline).map(docs -> {
                    Map<String, Document> map = new HashMap<>();
                    for (Document doc : docs) {
                        Document id = doc.get("_id", Document.class);
                        int m = ((Number) id.get("month")).intValue();
                        String method = id.getString("method");
                        map.put(m + "|" + method, doc);
                    }

                    List<TopupMonthMethod> result = new ArrayList<>();
                    for (int m = 1; m <= 12; m++) {
                        String monthAbbr = TopupStatsSupport.monthAbbr(m);
                        for (String method : methods) {
                            Document doc = map.get(m + "|" + method);
                            int totalTopups = 0;
                            long totalAmount = 0L;
                            if (doc != null) {
                                totalTopups = TopupStatsSupport.intValue(doc, "totalTopups");
                                totalAmount = TopupStatsSupport.longValue(doc, "totalAmount");
                            }
                            result.add(new TopupMonthMethod(monthAbbr, method, totalTopups, totalAmount));
                        }
                    }
                    return result;
                });
            });
        });
    }

    public Uni<List<TopupYearMethod>> findYearlyTopupMethodsByCardId(String cardNumber, Long endYear) {
        int y = endYear.intValue();
        int firstYear = y - 4;
        Date start = TopupStatsSupport.startOfYear(firstYear);
        Date end = TopupStatsSupport.startOfYear(y + 1);

        return isCardValid(cardNumber).chain(valid -> {
            if (!valid) {
                return Uni.createFrom().item(new ArrayList<TopupYearMethod>());
            }
            return distinctMethodsForCard(cardNumber).chain(methods -> {
                List<Bson> pipeline = new ArrayList<>(TopupStatsSupport.baseStages(start, end, cardNumber));
                pipeline.add(Aggregates.group(
                        new Document("year", new Document("$year", "$topupTime"))
                                .append("method", "$topupMethod"),
                        Accumulators.sum("totalTopups", 1),
                        Accumulators.sum("totalAmount", "$topupAmount")));

                return runAggregate(pipeline).map(docs -> {
                    Map<String, Document> map = new HashMap<>();
                    for (Document doc : docs) {
                        Document id = doc.get("_id", Document.class);
                        int yr = ((Number) id.get("year")).intValue();
                        String method = id.getString("method");
                        map.put(yr + "|" + method, doc);
                    }

                    List<TopupYearMethod> result = new ArrayList<>();
                    for (int i = 0; i < 5; i++) {
                        int yr = firstYear + i;
                        String yearStr = String.valueOf(yr);
                        for (String method : methods) {
                            Document doc = map.get(yr + "|" + method);
                            int totalTopups = 0;
                            long totalAmount = 0L;
                            if (doc != null) {
                                totalTopups = TopupStatsSupport.intValue(doc, "totalTopups");
                                totalAmount = TopupStatsSupport.longValue(doc, "totalAmount");
                            }
                            result.add(new TopupYearMethod(yearStr, method, totalTopups, totalAmount));
                        }
                    }
                    return result;
                });
            });
        });
    }
}
