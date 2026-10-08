package com.sanedge.merchant.repository.statsbyapikey;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.MongoDbResource;
import com.sanedge.common.test.MongoTestBase;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(MongoDbResource.class)
class MerchantMethodByApiKeyRepositoryTest extends MongoTestBase {

    @Inject
    MerchantMethodByApiKeyRepository repository;

    @Test
    void testFindMonthlyPaymentMethodsByApiKeyReturnsEmptyWhenNoData() {
        repository.findMonthlyPaymentMethodsByApiKey("non-existent-key", 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).isEmpty();
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyPaymentMethodsByApiKeyReturnsEmptyWhenNoData() {
        repository.findYearlyPaymentMethodsByApiKey("non-existent-key", 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).isEmpty();
                })
                .replaceWithVoid().await().indefinitely();
    }
}
