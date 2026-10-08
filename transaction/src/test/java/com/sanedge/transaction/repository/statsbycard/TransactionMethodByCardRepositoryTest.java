package com.sanedge.transaction.repository.statsbycard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.MongoDbResource;
import com.sanedge.common.test.MongoTestBase;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(MongoDbResource.class)
class TransactionMethodByCardRepositoryTest extends MongoTestBase {

    @Inject
    TransactionMethodByCardRepository repository;

    @Test
    void testFindMonthlyPaymentMethodsByCardReturnsEmptyOrNoError() {
        repository.findMonthlyPaymentMethodsByCard("NONEXISTENT", 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).isEmpty();
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyPaymentMethodsByCardReturnsEmptyOrNoError() {
        repository.findYearlyPaymentMethodsByCard("NONEXISTENT", 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).isEmpty();
                })
                .replaceWithVoid().await().indefinitely();
    }
}
