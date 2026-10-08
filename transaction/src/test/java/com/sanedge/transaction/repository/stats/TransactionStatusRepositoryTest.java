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
class TransactionStatusRepositoryTest extends MongoTestBase {

        @Inject
        TransactionStatusRepository transactionStatusRepository;

        @Test
        void testFindMonthTransactionStatusSuccessReturnsTwelveMonths() {
                transactionStatusRepository
                                .findMonthTransactionStatusSuccess(2024L, 1, 2024L, 12)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(12);
                                        assertThat(result.get(0).getMonth()).isEqualTo("Jan");
                                        assertThat(result.get(11).getMonth()).isEqualTo("Dec");
                                        assertThat(result).allSatisfy(month -> {
                                                assertThat(month.getTotalSuccess()).isZero();
                                                assertThat(month.getTotalAmount()).isZero();
                                        });
                                })
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindMonthTransactionStatusFailedReturnsTwelveMonths() {
                transactionStatusRepository
                                .findMonthTransactionStatusFailed(2024L, 1, 2024L, 12)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(12);
                                        assertThat(result.get(0).getMonth()).isEqualTo("Jan");
                                        assertThat(result).allSatisfy(month -> {
                                                assertThat(month.getTotalFailed()).isZero();
                                                assertThat(month.getTotalAmount()).isZero();
                                        });
                                })
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindYearlyTransactionStatusSuccessReturnsTwoYearsOrZeroData() {
                transactionStatusRepository
                                .findYearlyTransactionStatusSuccess(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(2);
                                        assertThat(result).anySatisfy(row -> {
                                                assertThat(row.getYear()).isIn("2024", "2023");
                                                assertThat(row.getTotalSuccess()).isZero();
                                        });
                                })
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindYearlyTransactionStatusFailedReturnsTwoYearsOrZeroData() {
                transactionStatusRepository
                                .findYearlyTransactionStatusFailed(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(2);
                                        assertThat(result).anySatisfy(row -> {
                                                assertThat(row.getYear()).isIn("2024", "2023");
                                                assertThat(row.getTotalFailed()).isZero();
                                        });
                                })
                                .replaceWithVoid().await().indefinitely();
        }
}
