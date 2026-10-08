package com.sanedge.merchant.repository.statsbyid;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.entity.MerchantMonthlyPaymentMethod;
import com.sanedge.merchant.entity.MerchantYearlyPaymentMethod;
import com.sanedge.merchant.repository.StatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * MongoDB aggregation counterpart of the former PostgreSQL native
 * merchant-scoped payment-method statistics.
 */
@ApplicationScoped
public class MerchantMethodByIdRepository implements ReactivePanacheMongoRepository<Merchant> {

    private static final ObjectId NO_MERCHANT = new ObjectId("000000000000000000000000");

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("transactions", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    private Uni<ObjectId> resolveMerchant(String merchantId) {
        if (merchantId == null) {
            return Uni.createFrom().item(NO_MERCHANT);
        }
        return find("{ '_id': ?1, 'deletedAt': null }", new ObjectId(merchantId)).firstResult()
                .map(merchant -> merchant != null ? merchant.id : NO_MERCHANT);
    }

    private Uni<List<String>> distinctPaymentMethods(ObjectId merchantId) {
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.eq("merchantId", merchantId))));
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

    private Uni<List<Document>> monthlyAggregate(ObjectId merchantId, int year) {
        Date start = StatsSupport.startOfYear(year);
        Date end = StatsSupport.startOfYear(year + 1);
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.eq("merchantId", merchantId),
                Filters.gte("transactionTime", start),
                Filters.lt("transactionTime", end))));
        pipeline.add(Aggregates.group(
                new Document("month", new Document("$month", "$transactionTime"))
                        .append("paymentMethod", "$paymentMethod"),
                Accumulators.sum("totalAmount", "$amount")));
        return runAggregate(pipeline);
    }

    private Uni<List<Document>> yearlyAggregate(ObjectId merchantId, int startYear, int endYear) {
        Date start = StatsSupport.startOfYear(startYear);
        Date end = StatsSupport.startOfYear(endYear + 1);
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(
                Filters.eq("deletedAt", null),
                Filters.eq("merchantId", merchantId),
                Filters.gte("transactionTime", start),
                Filters.lt("transactionTime", end))));
        pipeline.add(Aggregates.group(
                new Document("year", new Document("$year", "$transactionTime"))
                        .append("paymentMethod", "$paymentMethod"),
                Accumulators.sum("totalAmount", "$amount")));
        return runAggregate(pipeline);
    }

    private static List<MerchantMonthlyPaymentMethod> buildMonthly(List<String> methods, List<Document> docs) {
        Map<String, Document> index = new HashMap<>();
        for (Document doc : docs) {
            Document id = doc.get("_id", Document.class);
            index.put(id.getInteger("month") + "|" + id.getString("paymentMethod"), doc);
        }
        List<MerchantMonthlyPaymentMethod> list = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            for (String method : methods) {
                Document doc = index.get(month + "|" + method);
                long amount = doc != null ? StatsSupport.longValue(doc, "totalAmount") : 0L;
                list.add(new MerchantMonthlyPaymentMethod(StatsSupport.monthAbbr(month), method, amount));
            }
        }
        return list;
    }

    private static List<MerchantYearlyPaymentMethod> buildYearly(List<String> methods, List<Document> docs,
            int startYear, int endYear) {
        Map<String, Document> index = new HashMap<>();
        for (Document doc : docs) {
            Document id = doc.get("_id", Document.class);
            index.put(id.getInteger("year") + "|" + id.getString("paymentMethod"), doc);
        }
        List<MerchantYearlyPaymentMethod> list = new ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            for (String method : methods) {
                Document doc = index.get(year + "|" + method);
                long amount = doc != null ? StatsSupport.longValue(doc, "totalAmount") : 0L;
                list.add(new MerchantYearlyPaymentMethod(String.valueOf(year), method, amount));
            }
        }
        return list;
    }

    public Uni<List<MerchantMonthlyPaymentMethod>> findMonthlyPaymentMethodsById(String merchantId, Long year) {
        return resolveMerchant(merchantId).chain(resolved -> Uni.combine().all()
                .unis(distinctPaymentMethods(resolved), monthlyAggregate(resolved, year.intValue()))
                .asTuple()
                .map(tuple -> buildMonthly(tuple.getItem1(), tuple.getItem2())));
    }

    public Uni<List<MerchantYearlyPaymentMethod>> findYearlyPaymentMethodsById(String merchantId, Long endYear) {
        int y = endYear.intValue();
        int startYear = y - 4;
        return resolveMerchant(merchantId).chain(resolved -> Uni.combine().all()
                .unis(distinctPaymentMethods(resolved), yearlyAggregate(resolved, startYear, y))
                .asTuple()
                .map(tuple -> buildYearly(tuple.getItem1(), tuple.getItem2(), startYear, y)));
    }
}
