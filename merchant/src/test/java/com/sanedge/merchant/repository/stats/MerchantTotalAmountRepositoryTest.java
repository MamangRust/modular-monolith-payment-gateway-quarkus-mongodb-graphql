package com.sanedge.merchant.repository.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.MongoDbResource;
import com.sanedge.common.test.MongoTestBase;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(MongoDbResource.class)
class MerchantTotalAmountRepositoryTest extends MongoTestBase {

    @Inject
    MerchantTotalAmountRepository repository;

    @Test
    void testFindMonthlyTotalAmount_ReturnsZeroAmountsWhenNoData() {
        LocalDate date = LocalDate.of(2024, 6, 15);
        repository.findMonthlyTotalAmount(date)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).anySatisfy(item -> {
                        assertThat(item.getMonth()).isIn("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
                        assertThat(item.getTotalAmount()).isZero();
                    });
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyTotalAmount_ReturnsTwoYearsWithZeroWhenNoData() {
        repository.findYearlyTotalAmount(2024L, 2023L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(2);
                    assertThat(result).allSatisfy(year -> {
                        assertThat(year.getYear()).isIn("2024", "2023");
                        assertThat(year.getTotalAmount()).isZero();
                    });
                })
                .replaceWithVoid().await().indefinitely();
    }
}
