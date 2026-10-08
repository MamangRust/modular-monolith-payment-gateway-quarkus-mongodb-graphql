package com.sanedge.transfer.repository.statsbycard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.MongoDbResource;
import com.sanedge.common.test.MongoTestBase;
import com.sanedge.transfer.domain.requests.statsbycard.MonthStatusTransferCardNumber;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(MongoDbResource.class)
class TransferStatusByCardRepositoryTest extends MongoTestBase {

    @Inject
    TransferStatusByCardRepository transferStatusByCardRepository;

    private static final String TEST_CARD_NUMBER = "1234567890123456";

    private MonthStatusTransferCardNumber buildMonthReq(String cardNumber, int year, int month) {
        MonthStatusTransferCardNumber req = new MonthStatusTransferCardNumber();
        req.setCardNumber(cardNumber);
        req.setYear(year);
        req.setMonth(month);
        return req;
    }

    @Test
    void testFindMonthTransferStatusSuccessByCard_ReturnsTwelveMonths() {
        MonthStatusTransferCardNumber req = buildMonthReq(TEST_CARD_NUMBER, 2024, 6);
        transferStatusByCardRepository
                .findMonthTransferStatusSuccessByCard(req)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(12);
                    assertThat(result.get(5).getMonth()).isEqualTo("Jun");
                    assertThat(result.get(5).getTotalSuccess()).isZero();
                    assertThat(result.get(5).getTotalAmount()).isZero();
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindMonthTransferStatusSuccessByCard_ReturnsNonNullResult() {
        MonthStatusTransferCardNumber req = buildMonthReq("9999999999999999", 2024, 12);
        transferStatusByCardRepository
                .findMonthTransferStatusSuccessByCard(req)
                .invoke(result -> assertThat(result).isNotNull())
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyTransferStatusSuccessByCard_ReturnsTwoPlaceholders() {
        transferStatusByCardRepository
                .findYearlyTransferStatusSuccessByCard(TEST_CARD_NUMBER, 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(2);
                    assertThat(result.get(0).getTotalSuccess()).isZero();
                    assertThat(result.get(0).getTotalAmount()).isZero();
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindMonthTransferStatusFailedByCard_ReturnsTwelveMonths() {
        MonthStatusTransferCardNumber req = buildMonthReq(TEST_CARD_NUMBER, 2024, 6);
        transferStatusByCardRepository
                .findMonthTransferStatusFailedByCard(req)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(12);
                    assertThat(result.get(5).getMonth()).isEqualTo("Jun");
                    assertThat(result.get(5).getTotalFailed()).isZero();
                    assertThat(result.get(5).getTotalAmount()).isZero();
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyTransferStatusFailedByCard_ReturnsTwoPlaceholders() {
        transferStatusByCardRepository
                .findYearlyTransferStatusFailedByCard(TEST_CARD_NUMBER, 2024L)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(2);
                    assertThat(result.get(0).getTotalFailed()).isZero();
                    assertThat(result.get(0).getTotalAmount()).isZero();
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindYearlyTransferStatusFailedByCard_ReturnsNonNullResult() {
        transferStatusByCardRepository
                .findYearlyTransferStatusFailedByCard("9999999999999999", 2023L)
                .invoke(result -> assertThat(result).isNotNull())
                .replaceWithVoid().await().indefinitely();
    }
}
