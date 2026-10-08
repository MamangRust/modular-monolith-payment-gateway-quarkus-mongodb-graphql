package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.TransactionDto.*;
import io.smallrye.mutiny.Uni;

public interface TransactionService {
    Uni<FindAllTransactionResponse> listTransactions(int page, int size, String search);
    Uni<FindAllTransactionResponse> findTransactionsByCard(String cardNumber);
    Uni<FindAllTransactionResponse> findTransactionsByMerchant(String merchantId);
    Uni<FindAllTransactionResponse> findTransactionsByCardAndYear(String cardNumber, int year);
    Uni<FindAllTransactionResponse> findActiveTransactions(int page, int size, String search);
    Uni<FindAllTransactionResponse> findTrashedTransactions(int page, int size, String search);
    Uni<FindByIdTransactionResponse> getTransaction(String id);
    Uni<CreateTransactionResponse> createTransaction(CreateTransactionBody body);
    Uni<UpdateTransactionResponse> updateTransaction(String id, UpdateTransactionBody body);
    Uni<SimpleStatusMessageResponse> deleteTransactionPermanent(String id);
    Uni<TrashedTransactionResponse> trashTransaction(String id);
    Uni<TrashedTransactionResponse> restoreTransaction(String id);
    Uni<SimpleStatusMessageResponse> restoreAllTransactions();
    Uni<SimpleStatusMessageResponse> deleteAllTransactions();

    Uni<String> findMonthlyAmounts(int year);
    Uni<String> findYearlyAmounts(int year);
    Uni<String> findMonthlyAmountsByCard(int year, String cardNumber);
    Uni<String> findYearlyAmountsByCard(int year, String cardNumber);

    Uni<String> findMonthlyMethods(int year, int month);
    Uni<String> findYearlyMethods(int year);
    Uni<String> findMonthlyMethodsByCard(int year, int month, String cardNumber);
    Uni<String> findYearlyMethodsByCard(int year, String cardNumber);

    Uni<String> findMonthlyStatusSuccess(int year, int month);
    Uni<String> findYearlyStatusSuccess(int year);
    Uni<String> findMonthlyStatusFailed(int year, int month);
    Uni<String> findYearlyStatusFailed(int year);

    Uni<String> findMonthlyStatusSuccessByCard(int year, int month, String cardNumber);
    Uni<String> findYearlyStatusSuccessByCard(int year, String cardNumber);
    Uni<String> findMonthlyStatusFailedByCard(int year, int month, String cardNumber);
    Uni<String> findYearlyStatusFailedByCard(int year, String cardNumber);
}
