package com.sanedge.transaction.repository.stats;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.MongoDbResource;
import com.sanedge.common.test.MongoTestBase;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(MongoDbResource.class)
class TransactionMethodRepositoryTest extends MongoTestBase {

        @Inject
        TransactionMethodRepository transactionMethodRepository;

        @Test
        void testFindMonthlyPaymentMethodsReturnsEmptyOrNoError() {
                transactionMethodRepository.findMonthlyPaymentMethods(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).isEmpty();
                                })
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindYearlyPaymentMethodsReturnsEmptyOrNoError() {
                transactionMethodRepository.findYearlyPaymentMethods(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).isEmpty();
                                })
                                .replaceWithVoid().await().indefinitely();
        }
}
