package com.sanedge.transfer.repository.statsbycard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.MongoDbResource;
import com.sanedge.common.test.MongoTestBase;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(MongoDbResource.class)
class TransferAmountByCardRepositoryTest extends MongoTestBase {

    @Inject
    TransferAmountByCardRepository transferAmountByCardRepository;

    private static final String TEST_CARD_NUMBER = "1234567890123456";

    @Test
    void testFindMonthlyTransferAmountsBySender_ReturnsTwelveMonths() {
        transferAmountByCardRepository
                .findMonthlyTransferAmountsBySender(TEST_CARD_NUMBER, 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(12);
                    assertThat(result.get(0).getMonth()).isEqualTo("Jan");
                    assertThat(result.get(11).getMonth()).isEqualTo("Dec");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindMonthlyTransferAmountsBySender_ReturnsZeroAmountWhenNoData() {
        transferAmountByCardRepository
                .findMonthlyTransferAmountsBySender(TEST_CARD_NUMBER, 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).allSatisfy(month -> {
                        assertThat(month.getTotalAmount()).isZero();
                    });
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindMonthlyTransferAmountsBySender_ReturnsNonNullResult() {
        transferAmountByCardRepository
                .findMonthlyTransferAmountsBySender("9999999999999999", 2023L)
                .invoke(result -> assertThat(result).isNotNull())
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyTransferAmountsBySender_ReturnsFiveYears() {
        transferAmountByCardRepository
                .findYearlyTransferAmountsBySender(TEST_CARD_NUMBER, 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(5);
                    assertThat(result.get(0).getYear()).isEqualTo("2020");
                    assertThat(result.get(4).getYear()).isEqualTo("2024");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyTransferAmountsBySender_ReturnsZeroAmountWhenNoData() {
        transferAmountByCardRepository
                .findYearlyTransferAmountsBySender(TEST_CARD_NUMBER, 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).allSatisfy(year -> {
                        assertThat(year.getTotalAmount()).isZero();
                    });
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindMonthlyTransferAmountsByReceiver_ReturnsTwelveMonths() {
        transferAmountByCardRepository
                .findMonthlyTransferAmountsByReceiver(TEST_CARD_NUMBER, 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(12);
                    assertThat(result.get(0).getMonth()).isEqualTo("Jan");
                    assertThat(result.get(11).getMonth()).isEqualTo("Dec");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyTransferAmountsByReceiver_ReturnsFiveYears() {
        transferAmountByCardRepository
                .findYearlyTransferAmountsByReceiver(TEST_CARD_NUMBER, 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(5);
                    assertThat(result.get(0).getYear()).isEqualTo("2020");
                    assertThat(result.get(4).getYear()).isEqualTo("2024");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyTransferAmountsByReceiver_ReturnsNonNullResult() {
        transferAmountByCardRepository
                .findYearlyTransferAmountsByReceiver("9999999999999999", 2023L)
                .invoke(result -> assertThat(result).isNotNull())
                .replaceWithVoid().await().indefinitely();
    }
}
