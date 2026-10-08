package com.sanedge.card.repository;

import com.sanedge.card.entity.Card;
import com.sanedge.card.repository.stats.CardStatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * MongoDB aggregation counterpart of the former PostgreSQL dashboard totals.
 * Every movement collection lives in the same {@code PAYMENT_GATEWAY} database,
 * so the totals are computed with a {@code $match} (deletedAt null, joined to a
 * non-deleted card) + a single {@code $sum}. The old SQL
 * {@code getTotalTransferAmount} UNION ALL'd the transfers table twice (a
 * latent double-count bug); here a single {@code $sum} is used.
 */
@ApplicationScoped
public class CardDashboardRepository implements ReactivePanacheMongoRepository<Card> {

    public Uni<Long> getTotalBalance() {
        return CardStatsSupport.sumField(mongoDatabase(), "saldos", "totalBalance", "cardNumber", null, true);
    }

    public Uni<Long> getTotalTopupAmount() {
        return CardStatsSupport.sumField(mongoDatabase(), "topups", "topupAmount", "cardNumber", null, true);
    }

    public Uni<Long> getTotalWithdrawAmount() {
        return CardStatsSupport.sumField(mongoDatabase(), "withdraws", "withdrawAmount", "cardNumber", null, true);
    }

    public Uni<Long> getTotalTransactionAmount() {
        return CardStatsSupport.sumField(mongoDatabase(), "transactions", "amount", "cardNumber", null, true);
    }

    public Uni<Long> getTotalTransferAmount() {
        // Old SQL did not join cards here; keep filtering only non-deleted
        // transfers and use a single $sum (fixes the double-count bug).
        return CardStatsSupport.sumField(mongoDatabase(), "transfers", "transferAmount", "transferFrom", null, false);
    }
}
