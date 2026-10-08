package com.sanedge.merchant.repository.statsbyid;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.MongoDbResource;
import com.sanedge.common.test.MongoTestBase;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(MongoDbResource.class)
class MerchantMethodByIdRepositoryTest extends MongoTestBase {

    @Inject
    MerchantMethodByIdRepository repository;

    private static final String UNKNOWN_MERCHANT_ID = "507f1f77bcf86cd799439011";

    @Test
    void testFindMonthlyPaymentMethodsByIdReturnsEmptyWhenNoData() {
        repository.findMonthlyPaymentMethodsById(UNKNOWN_MERCHANT_ID, 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).isEmpty();
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyPaymentMethodsByIdReturnsEmptyWhenNoData() {
        repository.findYearlyPaymentMethodsById(UNKNOWN_MERCHANT_ID, 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).isEmpty();
                })
                .replaceWithVoid().await().indefinitely();
    }
}
