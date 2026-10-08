package com.sanedge.withdraw.repository.stats;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@Disabled("Stats aggregation uses MongoDB-specific pipeline; enable after verifying DB compatibility")
@QuarkusTest
@RunOnVertxContext
class WithdrawStatusRepositoryTest {

        @Inject
        WithdrawStatusRepository withdrawStatusRepository;

        @Test
        Uni<Void> testFindMonthWithdrawStatusSuccess_ReturnsOneRow() {
                return withdrawStatusRepository
                                .findMonthWithdrawStatusSuccess(2024L, 6, 2024L, 6)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(1);
                                        assertThat(result.get(0).getMonth()).isEqualTo("Jun");
                                        assertThat(result.get(0).getTotalSuccess()).isZero();
                                        assertThat(result.get(0).getTotalAmount()).isZero();
                                })
                                .replaceWithVoid();
        }

        @Test
        Uni<Void> testFindMonthWithdrawStatusSuccess_ReturnsMultipleMonths() {
                return withdrawStatusRepository
                                .findMonthWithdrawStatusSuccess(2024L, 1, 2024L, 3)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(3);
                                        assertThat(result.get(0).getMonth()).isEqualTo("Jan");
                                        assertThat(result.get(1).getMonth()).isEqualTo("Feb");
                                        assertThat(result.get(2).getMonth()).isEqualTo("Mar");
                                })
                                .replaceWithVoid();
        }

        @Test
        Uni<Void> testFindMonthWithdrawStatusSuccess_ReturnsNonNullResult() {
                return withdrawStatusRepository
                                .findMonthWithdrawStatusSuccess(2024L, 12, 2024L, 12)
                                .invoke(result -> assertThat(result).isNotNull())
                                .replaceWithVoid();
        }

        @Test
        Uni<Void> testFindMonthWithdrawStatusFailed_ReturnsOneRow() {
                return withdrawStatusRepository
                                .findMonthWithdrawStatusFailed(2024L, 6, 2024L, 6)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(1);
                                        assertThat(result.get(0).getMonth()).isEqualTo("Jun");
                                        assertThat(result.get(0).getTotalFailed()).isZero();
                                        assertThat(result.get(0).getTotalAmount()).isZero();
                                })
                                .replaceWithVoid();
        }

        @Test
        Uni<Void> testFindMonthWithdrawStatusFailed_ReturnsMultipleMonths() {
                return withdrawStatusRepository
                                .findMonthWithdrawStatusFailed(2024L, 10, 2024L, 12)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(3);
                                        assertThat(result.get(0).getMonth()).isEqualTo("Oct");
                                        assertThat(result.get(1).getMonth()).isEqualTo("Nov");
                                        assertThat(result.get(2).getMonth()).isEqualTo("Dec");
                                })
                                .replaceWithVoid();
        }

        @Test
        Uni<Void> testFindMonthWithdrawStatusFailed_ReturnsNonNullResult() {
                return withdrawStatusRepository
                                .findMonthWithdrawStatusFailed(2024L, 12, 2024L, 12)
                                .invoke(result -> assertThat(result).isNotNull())
                                .replaceWithVoid();
        }

        @Test
        Uni<Void> testFindYearlyWithdrawStatusSuccess_ReturnsTwoPlaceholders() {
                return withdrawStatusRepository
                                .findYearlyWithdrawStatusSuccess(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(2);
                                        assertThat(result.get(0).getTotalSuccess()).isZero();
                                        assertThat(result.get(0).getTotalAmount()).isZero();
                                })
                                .replaceWithVoid();
        }

        @Test
        Uni<Void> testFindYearlyWithdrawStatusSuccess_ReturnsNonNullResult() {
                return withdrawStatusRepository
                                .findYearlyWithdrawStatusSuccess(2023L)
                                .invoke(result -> assertThat(result).isNotNull())
                                .replaceWithVoid();
        }

        @Test
        Uni<Void> testFindYearlyWithdrawStatusFailed_ReturnsTwoPlaceholders() {
                return withdrawStatusRepository
                                .findYearlyWithdrawStatusFailed(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).hasSize(2);
                                        assertThat(result.get(0).getTotalFailed()).isZero();
                                        assertThat(result.get(0).getTotalAmount()).isZero();
                                })
                                .replaceWithVoid();
        }

        @Test
        Uni<Void> testFindYearlyWithdrawStatusFailed_ReturnsNonNullResult() {
                return withdrawStatusRepository
                                .findYearlyWithdrawStatusFailed(2023L)
                                .invoke(result -> assertThat(result).isNotNull())
                                .replaceWithVoid();
        }
}
