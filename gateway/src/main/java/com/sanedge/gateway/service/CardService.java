package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.CardDto.*;
import io.smallrye.mutiny.Uni;

public interface CardService {
    Uni<FindAllCardResponse> listCards(int page, int size, String search);
    Uni<FindAllCardResponse> findActiveCards(int page, int size, String search);
    Uni<FindAllCardResponse> findTrashedCards(int page, int size, String search);
    Uni<FindByIdCardResponse> getCard(String id);
    Uni<FindByIdCardResponse> findCardByUser(String userId);
    Uni<FindByIdCardResponse> findCardByNumber(String cardNumber);
    Uni<CardWithEmailResponse> findUserCardByCardNumber(String cardNumber);
    Uni<CreateCardResponse> createCard(CreateCardRequest body);
    Uni<UpdateCardResponse> updateCard(String id, UpdateCardRequest body);
    Uni<SimpleStatusMessageResponse> deleteCardPermanent(String id);
    Uni<TrashedCardResponse> deleteCard(String id);
    Uni<TrashedCardResponse> trashCard(String id);
    Uni<TrashedCardResponse> restoreCard(String id);
    Uni<SimpleStatusMessageResponse> restoreAllCards();
    Uni<SimpleStatusMessageResponse> deleteAllCards();

    Uni<String> findMonthlyBalance(int year);
    Uni<String> findYearlyBalance(int year);
    Uni<String> getMonthlyBalanceByCard(int year, String cardNumber);
    Uni<String> getYearlyBalanceByCard(int year, String cardNumber);

    Uni<String> findMonthlyTopupAmount(int year);
    Uni<String> findYearlyTopupAmount(int year);
    Uni<String> getMonthlyTopupAmountByCard(int year, String cardNumber);
    Uni<String> getYearlyTopupAmountByCard(int year, String cardNumber);

    Uni<String> findMonthlyTransactionAmount(int year);
    Uni<String> findYearlyTransactionAmount(int year);
    Uni<String> getMonthlyTransactionAmountByCard(int year, String cardNumber);
    Uni<String> getYearlyTransactionAmountByCard(int year, String cardNumber);

    Uni<String> findMonthlyTransferAmountSender(int year);
    Uni<String> findMonthlyTransferAmountReceiver(int year);
    Uni<String> findYearlyTransferAmountSender(int year);
    Uni<String> findYearlyTransferAmountReceiver(int year);
    Uni<String> getMonthlyTransferAmountByCardSender(int year, String cardNumber);
    Uni<String> getMonthlyTransferAmountByCardReceiver(int year, String cardNumber);
    Uni<String> getYearlyTransferAmountByCardSender(int year, String cardNumber);
    Uni<String> getYearlyTransferAmountByCardReceiver(int year, String cardNumber);

    Uni<String> findMonthlyWithdrawAmount(int year);
    Uni<String> findYearlyWithdrawAmount(int year);
    Uni<String> getMonthlyWithdrawAmountByCard(int year, String cardNumber);
    Uni<String> getYearlyWithdrawAmountByCard(int year, String cardNumber);

    Uni<String> findCardDashboard();
    Uni<String> findCardDashboardByCardNumber(String cardNumber);
}
