package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.TopupDto.*;
import io.smallrye.mutiny.Uni;

public interface TopupService {
    Uni<FindAllTopupResponse> listTopups(int page, int size, String search);
    Uni<FindAllTopupResponse> listTopupsByCard(String cardNumber, int page, int size, String search);
    Uni<FindAllTopupResponse> findActiveTopups(int page, int size, String search);
    Uni<FindAllTopupResponse> findTrashedTopups(int page, int size, String search);
    Uni<FindByIdTopupResponse> getTopup(String id);
    Uni<FindByIdTopupResponse> getTopupByCard(String cardNumber, int year);
    Uni<CreateTopupResponse> createTopup(CreateTopupRequest body);
    Uni<UpdateTopupResponse> updateTopup(String id, UpdateTopupRequest body);
    Uni<SimpleStatusMessageResponse> deleteTopupPermanent(String id);
    Uni<TrashedTopupResponse> trashTopup(String id);
    Uni<TrashedTopupResponse> restoreTopup(String id);
    Uni<SimpleStatusMessageResponse> restoreAllTopups();
    Uni<SimpleStatusMessageResponse> deleteAllTopups();

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
