package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.WithdrawDto.*;
import io.smallrye.mutiny.Uni;

public interface WithdrawService {
    Uni<FindAllWithdrawResponse> listWithdraws(int page, int size, String search);
    Uni<FindAllWithdrawResponse> findByCard(String cardNumber);
    Uni<FindAllWithdrawResponse> findActiveWithdraws(int page, int size, String search);
    Uni<FindAllWithdrawResponse> findTrashedWithdraws(int page, int size, String search);
    Uni<FindByIdWithdrawResponse> getWithdraw(String id);
    Uni<CreateWithdrawResponse> createWithdraw(CreateWithdrawBody body);
    Uni<UpdateWithdrawResponse> updateWithdraw(String id, CreateWithdrawBody body);
    Uni<TrashedWithdrawResponse> deleteWithdraw(String id);
    Uni<SimpleStatusMessageResponse> deleteWithdrawPermanent(String id);
    Uni<TrashedWithdrawResponse> trashWithdraw(String id);
    Uni<TrashedWithdrawResponse> restoreWithdraw(String id);
    Uni<String> restoreAllWithdraws();
    Uni<String> deleteAllWithdraws();

    Uni<String> findMonthlyAmounts(int year);
    Uni<String> findYearlyAmounts(int year);
    Uni<String> findMonthlyByCard(int year, String cardNumber);
    Uni<String> findYearlyByCard(int year, String cardNumber);

    Uni<String> findMonthlyStatusSuccess(int year, int month);
    Uni<String> findYearlyStatusSuccess(int year);
    Uni<String> findMonthlyStatusFailed(int year, int month);
    Uni<String> findYearlyStatusFailed(int year);

    Uni<String> findMonthlyStatusSuccessByCard(int year, int month, String cardNumber);
    Uni<String> findYearlyStatusSuccessByCard(int year, String cardNumber);
    Uni<String> findMonthlyStatusFailedByCard(int year, int month, String cardNumber);
    Uni<String> findYearlyStatusFailedByCard(int year, String cardNumber);
}
