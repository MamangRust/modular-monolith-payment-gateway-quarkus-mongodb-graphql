package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.TransferDto.*;
import io.smallrye.mutiny.Uni;

public interface TransferService {
    Uni<FindAllTransferResponse> listTransfers(int page, int size, String search);
    Uni<FindAllTransferResponse> findActiveTransfers(int page, int size, String search);
    Uni<FindAllTransferResponse> findTrashedTransfers(int page, int size, String search);
    Uni<FindByIdTransferResponse> getTransfer(String id);
    Uni<FindAllTransferResponse> findTransfersFrom(String cardNumber);
    Uni<FindAllTransferResponse> findTransfersTo(String cardNumber);
    Uni<CreateTransferResponse> createTransfer(CreateTransferRequest body);
    Uni<UpdateTransferResponse> updateTransfer(String id, UpdateTransferRequest body);
    Uni<SimpleStatusMessageResponse> deleteTransferPermanent(String id);
    Uni<TrashedTransferResponse> trashTransfer(String id);
    Uni<TrashedTransferResponse> restoreTransfer(String id);
    Uni<String> restoreAllTransfers();
    Uni<String> deleteAllTransfers();

    Uni<String> findMonthlyAmounts(int year);
    Uni<String> findYearlyAmounts(int year);
    Uni<String> findMonthlyAmountsFromCard(int year, String cardNumber);
    Uni<String> findMonthlyAmountsToCard(int year, String cardNumber);
    Uni<String> findYearlyAmountsFromCard(int year, String cardNumber);
    Uni<String> findYearlyAmountsToCard(int year, String cardNumber);

    Uni<String> findMonthlyStatusSuccess(int year, int month);
    Uni<String> findYearlyStatusSuccess(int year);
    Uni<String> findMonthlyStatusFailed(int year, int month);
    Uni<String> findYearlyStatusFailed(int year);

    Uni<String> findMonthlyStatusSuccessByCard(int year, int month, String cardNumber);
    Uni<String> findYearlyStatusSuccessByCard(int year, String cardNumber);
    Uni<String> findMonthlyStatusFailedByCard(int year, int month, String cardNumber);
    Uni<String> findYearlyStatusFailedByCard(int year, String cardNumber);
}
