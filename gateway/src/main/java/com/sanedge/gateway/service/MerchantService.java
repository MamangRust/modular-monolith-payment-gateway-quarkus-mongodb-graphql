package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantDto.*;
import io.smallrye.mutiny.Uni;

public interface MerchantService {
    Uni<FindAllMerchantResponse> listMerchants(int page, int size, String search);
    Uni<FindByIdMerchantResponse> getMerchant(String id);
    Uni<FindByIdMerchantResponse> getMerchantByApiKey(String apiKey);
    Uni<FindAllMerchantResponse> getMerchantsByUserId(String userId);
    Uni<FindAllMerchantResponse> findActiveMerchants(int page, int size, String search);
    Uni<FindAllMerchantResponse> findTrashedMerchants(int page, int size, String search);
    Uni<CreateMerchantResponse> createMerchant(CreateMerchantRequest body);
    Uni<UpdateMerchantResponse> updateMerchant(String id, UpdateMerchantRequest body);
    Uni<UpdateMerchantResponse> updateMerchantStatus(String id, String status);
    Uni<SimpleStatusMessageResponse> deleteMerchantPermanent(String id);
    Uni<TrashedMerchantResponse> deleteMerchant(String id);
    Uni<TrashedMerchantResponse> restoreMerchant(String id);
    Uni<SimpleStatusMessageResponse> restoreAllMerchants();
    Uni<SimpleStatusMessageResponse> deleteAllMerchants();

    Uni<String> findAllTransactions(int page, int size, String search, String merchantId);
    Uni<String> findTransactionsById(int page, int size, String search, String id);
    Uni<String> findTransactionsByApiKey(int page, int size, String search, String apiKey);

    Uni<String> getMonthlyAmount(int year);
    Uni<String> getYearlyAmount(int year);
    Uni<String> getMonthlyAmountById(int year, String merchantId);
    Uni<String> getYearlyAmountById(int year, String merchantId);
    Uni<String> getMonthlyAmountByApiKey(int year, String apiKey);
    Uni<String> getYearlyAmountByApiKey(int year, String apiKey);

    Uni<String> getMonthlyMethod(int year, int month);
    Uni<String> getYearlyMethod(int year);
    Uni<String> getMonthlyMethodById(int year, int month, String merchantId);
    Uni<String> getYearlyMethodById(int year, String merchantId);
    Uni<String> getMonthlyMethodByApiKey(int year, int month, String apiKey);
    Uni<String> getYearlyMethodByApiKey(int year, String apiKey);

    Uni<String> getTotalMonthlyAmount(int year);
    Uni<String> getTotalYearlyAmount(int year);
    Uni<String> getTotalMonthlyAmountById(int year, String merchantId);
    Uni<String> getTotalYearlyAmountById(int year, String merchantId);
    Uni<String> getTotalMonthlyAmountByApiKey(int year, String apiKey);
    Uni<String> getTotalYearlyAmountByApiKey(int year, String apiKey);
}
