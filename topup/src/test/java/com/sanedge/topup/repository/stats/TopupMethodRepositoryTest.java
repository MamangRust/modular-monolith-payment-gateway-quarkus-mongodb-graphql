package com.sanedge.topup.repository.stats;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@Disabled("Stats SQL uses PostgreSQL-specific functions; enable after verifying DB compatibility")
@QuarkusTest
@RunOnVertxContext
class TopupMethodRepositoryTest {

        @Inject
        TopupMethodRepository topupMethodRepository;

        @Test
        Uni<Void> testFindMonthlyTopupMethodsReturnsEmptyWhenNoData() {
                return topupMethodRepository.findMonthlyTopupMethods(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).isEmpty();
                                })
                                .replaceWithVoid();
        }

        @Test
        Uni<Void> testFindYearlyTopupMethodsReturnsEmptyWhenNoData() {
                return topupMethodRepository.findYearlyTopupMethods(2024L)
                                .invoke(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).isEmpty();
                                })
                                .replaceWithVoid();
        }
}