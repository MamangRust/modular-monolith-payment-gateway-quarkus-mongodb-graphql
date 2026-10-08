package com.sanedge.transfer.repository.stats;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.MongoDbResource;
import com.sanedge.common.test.MongoTestBase;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(MongoDbResource.class)
class TransferStatusRepositoryTest extends MongoTestBase {

        @Inject
        TransferStatusRepository transferStatusRepository;

        @Test
        void testFindMonthTransferStatusSuccess_ReturnsOneRow() {
                transferStatusRepository
                                .findMonthTransferStatusSuccess(2023L, 12, 2023L, 12)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(1);
                                        assertThat(result.get(0).getTotalSuccess()).isZero();
                                        assertThat(result.get(0).getTotalAmount()).isZero();
                                })
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindYearlyTransferStatusSuccess_ReturnsTwoPlaceholders() {
                transferStatusRepository
                                .findYearlyTransferStatusSuccess(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(2);
                                        assertThat(result.get(0).getTotalSuccess()).isZero();
                                })
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindMonthTransferStatusFailed_ReturnsOneRow() {
                transferStatusRepository
                                .findMonthTransferStatusFailed(2023L, 12, 2023L, 12)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(1);
                                        assertThat(result.get(0).getTotalFailed()).isZero();
                                })
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindYearlyTransferStatusFailed_ReturnsTwoPlaceholders() {
                transferStatusRepository
                                .findYearlyTransferStatusFailed(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(2);
                                        assertThat(result.get(0).getTotalFailed()).isZero();
                                })
                                .replaceWithVoid().await().indefinitely();
        }

        @Test
        void testFindMonthTransferStatusSuccess_ReturnsNonNullResult() {
                transferStatusRepository
                                .findMonthTransferStatusSuccess(2024L, 6, 2024L, 6)
                                .invoke(result -> assertThat(result).isNotNull())
                                .replaceWithVoid().await().indefinitely();
        }
}
