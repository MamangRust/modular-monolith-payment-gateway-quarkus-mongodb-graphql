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
class TransactionAmountRepositoryTest extends MongoTestBase {

        @Inject
        TransactionAmountRepository transactionAmountRepository;

        @Test
        void testFindMonthlyAmountsReturnsTwelveMonths() {
                transactionAmountRepository.findMonthlyAmounts(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(12);
                                        assertThat(result.get(0).getMonth()).isEqualTo("Jan");
                                        assertThat(result.get(11).getMonth()).isEqualTo("Dec");
                                })
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindMonthlyAmountsReturnsZeroAmountWhenNoData() {
                transactionAmountRepository.findMonthlyAmounts(2024L)
                                .invoke(result -> assertThat(result).allSatisfy(
                                                month -> assertThat(month.getTotalAmount()).isZero()))
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindYearlyAmountsReturnsFiveYears() {
                transactionAmountRepository.findYearlyAmounts(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(5);
                                        assertThat(result.get(0).getYear()).isEqualTo("2020");
                                        assertThat(result.get(4).getYear()).isEqualTo("2024");
                                })
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindYearlyAmountsReturnsZeroAmountWhenNoData() {
                transactionAmountRepository.findYearlyAmounts(2024L)
                                .invoke(result -> assertThat(result)
                                                .allSatisfy(year -> assertThat(year.getTotalAmount()).isZero()))
                                .replaceWithVoid().await().indefinitely();
        }
}
