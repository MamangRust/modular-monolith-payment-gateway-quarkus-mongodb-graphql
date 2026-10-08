package com.sanedge.card.repository;

import com.sanedge.card.entity.Card;
import com.sanedge.card.repository.stats.CardStatsSupport;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Per-card variant of {@link CardDashboardRepository}: the same aggregation
 * with an extra {@code cardNumber} predicate. Transfer sender/receiver totals
 * filter the {@code transfers} collection directly (the old SQL did not join
 * cards there either).
 */
@ApplicationScoped
public class CardDashboardByCardRepository implements ReactivePanacheMongoRepository<Card> {

    public Uni<Long> getTotalBalanceByCard(String cardNumber) {
        return CardStatsSupport.sumField(mongoDatabase(), "saldos", "totalBalance", "cardNumber", cardNumber, true);
    }

    public Uni<Long> getTotalTopupAmountByCard(String cardNumber) {
        return CardStatsSupport.sumField(mongoDatabase(), "topups", "topupAmount", "cardNumber", cardNumber, true);
    }

    public Uni<Long> getTotalWithdrawAmountByCard(String cardNumber) {
        return CardStatsSupport.sumField(mongoDatabase(), "withdraws", "withdrawAmount", "cardNumber", cardNumber,
                true);
    }

    public Uni<Long> getTotalTransactionAmountByCard(String cardNumber) {
        return CardStatsSupport.sumField(mongoDatabase(), "transactions", "amount", "cardNumber", cardNumber, true);
    }

    public Uni<Long> getTotalTransferAmountBySender(String sender) {
        return CardStatsSupport.sumField(mongoDatabase(), "transfers", "transferAmount", "transferFrom", sender,
                false);
    }

    public Uni<Long> getTotalTransferAmountByReceiver(String receiver) {
        return CardStatsSupport.sumField(mongoDatabase(), "transfers", "transferAmount", "transferTo", receiver,
                false);
    }
}
