package com.sanedge.merchant.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.merchant.domain.requests.transactions.FindAllMerchantTransactions;
import com.sanedge.merchant.domain.requests.transactions.FindAllMerchantTransactionsByApiKey;
import com.sanedge.merchant.domain.requests.transactions.FindAllMerchantTransactionsById;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.entity.MerchantTransactions;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * MongoDB aggregation counterpart of the former PostgreSQL native
 * transactions↔merchants {@code JOIN} query with {@code ILIKE} search and
 * pagination. The inner join is mirrored with a {@code $lookup} +
 * {@code $unwind} over the {@code merchants} collection, and the
 * {@code ILIKE '%search%'} predicates become case-insensitive {@code $regex}
 * matches.
 */
@ApplicationScoped
public class MerchantTransactionRepository implements ReactivePanacheMongoRepository<Merchant> {

    private Uni<List<Document>> runAggregate(List<Bson> pipeline) {
        return mongoDatabase()
                .getCollection("transactions", Document.class)
                .aggregate(pipeline, Document.class)
                .collect().asList();
    }

    private static List<Bson> basePipeline(String search, ObjectId merchantId, String apiKey) {
        List<Bson> conditions = new ArrayList<>();
        conditions.add(Filters.eq("deletedAt", null));
        if (merchantId != null) {
            conditions.add(Filters.eq("merchantId", merchantId));
        }
        if (search != null && !search.isEmpty()) {
            String regex = Pattern.quote(search);
            conditions.add(Filters.or(
                    Filters.regex("cardNumber", regex, "i"),
                    Filters.regex("paymentMethod", regex, "i")));
        }

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(Filters.and(conditions)));
        pipeline.add(Aggregates.lookup("merchants", "merchantId", "_id", "merchant"));
        pipeline.add(Aggregates.unwind("$merchant"));
        if (apiKey != null) {
            pipeline.add(Aggregates.match(Filters.eq("merchant.apiKey", apiKey)));
        }
        return pipeline;
    }

    private Uni<PagedResult<MerchantTransactions>> paged(List<Bson> base, int page, int size) {
        List<Bson> dataPipeline = new ArrayList<>(base);
        dataPipeline.add(Aggregates.sort(Sorts.descending("transactionTime")));
        dataPipeline.add(Aggregates.skip((page - 1) * size));
        dataPipeline.add(Aggregates.limit(size));

        List<Bson> countPipeline = new ArrayList<>(base);
        countPipeline.add(Aggregates.count("total"));

        Uni<List<MerchantTransactions>> dataUni = runAggregate(dataPipeline).map(this::mapTransactions);
        Uni<Long> countUni = runAggregate(countPipeline)
                .map(list -> list.isEmpty() ? 0L : ((Number) list.get(0).get("total")).longValue());

        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<MerchantTransactions>> findAllTransactions(FindAllMerchantTransactions req) {
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() : 1;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        return paged(basePipeline(search, null, null), page, size);
    }

    public Uni<PagedResult<MerchantTransactions>> findAllTransactionsByMerchant(FindAllMerchantTransactionsById req) {
        String merchantId = req.getMerchantId();
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() : 1;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        ObjectId oid = merchantId != null ? new ObjectId(merchantId) : null;
        return paged(basePipeline(search, oid, null), page, size);
    }

    public Uni<PagedResult<MerchantTransactions>> findAllTransactionsByApiKey(FindAllMerchantTransactionsByApiKey req) {
        String apiKey = req.getApiKey();
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() : 1;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        return paged(basePipeline(search, null, apiKey), page, size);
    }

    private List<MerchantTransactions> mapTransactions(List<Document> documents) {
        List<MerchantTransactions> list = new ArrayList<>();
        for (Document doc : documents) {
            Document merchant = doc.get("merchant", Document.class);
            list.add(new MerchantTransactions(
                    toHex(doc.get("_id")),
                    doc.getString("cardNumber"),
                    doc.get("amount") != null ? ((Number) doc.get("amount")).longValue() : null,
                    doc.getString("paymentMethod"),
                    toHex(doc.get("merchantId")),
                    merchant != null ? merchant.getString("name") : null,
                    toInstant(doc.get("transactionTime")),
                    toInstant(doc.get("createdAt")),
                    toInstant(doc.get("updatedAt")),
                    toInstant(doc.get("deletedAt"))));
        }
        return list;
    }

    private static String toHex(Object value) {
        if (value instanceof ObjectId objectId) {
            return objectId.toHexString();
        }
        return value != null ? value.toString() : null;
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        return null;
    }
}
