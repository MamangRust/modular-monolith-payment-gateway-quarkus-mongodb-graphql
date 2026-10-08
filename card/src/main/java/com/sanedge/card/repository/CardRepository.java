package com.sanedge.card.repository;

import java.time.Instant;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.card.entity.Card;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CardRepository implements ReactivePanacheMongoRepository<Card> {

    private ReactivePanacheQuery<Card> search(boolean trashed, String keyword) {
        String deletedFilter = trashed ? "'deletedAt': { '$ne': null }" : "'deletedAt': null";
        if (keyword == null) {
            return find("{ " + deletedFilter + " }");
        }
        String quoted = Pattern.quote(keyword);
        return find("{ " + deletedFilter
                + ", '$or': [ { 'cardNumber': { '$regex': ?1, '$options': 'i' } }, "
                + "{ 'cardType': { '$regex': ?2, '$options': 'i' } }, "
                + "{ 'cardProvider': { '$regex': ?3, '$options': 'i' } } ] }",
                quoted, quoted, quoted);
    }

    private Uni<PagedResult<Card>> paged(ReactivePanacheQuery<Card> query, int page, int size) {
        return Uni.combine().all().unis(
                query.page(page > 0 ? page - 1 : 0, size > 0 ? size : 10).list(),
                query.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Card>> findCards(String keyword, int page, int size) {
        return paged(search(false, keyword), page, size);
    }

    public Uni<PagedResult<Card>> findActiveCards(String keyword, int page, int size) {
        return paged(search(false, keyword), page, size);
    }

    public Uni<PagedResult<Card>> findTrashedCards(String keyword, int page, int size) {
        return paged(search(true, keyword), page, size);
    }

    public Uni<Card> findCardById(String cardId) {
        return find("{ '_id': ?1, 'deletedAt': null }", new ObjectId(cardId)).firstResult();
    }

    public Uni<Card> findCardByUserId(String userId) {
        if (userId == null) {
            return Uni.createFrom().nullItem();
        }
        return find("{ 'userId': ?1, 'deletedAt': null }", new ObjectId(userId)).firstResult();
    }

    public Uni<Card> findCardByCardNumber(String cardNumber) {
        return find("{ 'cardNumber': ?1, 'deletedAt': null }", cardNumber).firstResult();
    }

    public Uni<Card> trashed(String cardId) {
        return find("{ '_id': ?1, 'deletedAt': null }", new ObjectId(cardId)).firstResult()
                .chain(card -> {
                    if (card != null) {
                        card.setDeletedAt(Instant.now());
                        return card.update().map(v -> card);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    public Uni<Card> restore(String cardId) {
        return find("{ '_id': ?1, 'deletedAt': { '$ne': null } }", new ObjectId(cardId)).firstResult()
                .chain(card -> {
                    if (card != null) {
                        card.setDeletedAt(null);
                        return card.update().map(v -> card);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    public Uni<Boolean> deletePermanent(String cardId) {
        return find("{ '_id': ?1 }", new ObjectId(cardId)).firstResult()
                .chain(card -> {
                    if (card != null) {
                        return card.delete().map(v -> true);
                    }
                    return Uni.createFrom().item(false);
                });
    }

    public Uni<Boolean> restoreAllDeleted() {
        return update("{ '$set': { 'deletedAt': null } }")
                .where("{ 'deletedAt': { '$ne': null } }")
                .map(updatedCount -> updatedCount > 0);
    }

    public Uni<Boolean> deleteAllDeleted() {
        return delete("{ 'deletedAt': { '$ne': null } }")
                .map(deletedCount -> deletedCount > 0);
    }
}
