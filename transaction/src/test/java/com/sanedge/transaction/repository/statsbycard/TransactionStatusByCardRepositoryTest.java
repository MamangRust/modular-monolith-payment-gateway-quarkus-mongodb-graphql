package com.sanedge.transaction.repository.statsbycard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.MongoDbResource;
import com.sanedge.common.test.MongoTestBase;
import com.sanedge.transaction.domain.requests.statsbycard.MonthStatusTransactionCardNumber;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(MongoDbResource.class)
class TransactionStatusByCardRepositoryTest extends MongoTestBase {

    @Inject
    TransactionStatusByCardRepository repository;

    private MonthStatusTransactionCardNumber monthReq(String cardNumber, Long year) {
        MonthStatusTransactionCardNumber req = new MonthStatusTransactionCardNumber();
        req.setCardNumber(cardNumber);
        req.setYear(year);
        return req;
    }

    @Test
    void testFindMonthTransactionStatusSuccessByCardReturnsTwelveMonths() {
        repository.findMonthTransactionStatusSuccessByCard(monthReq("CARD-1", 2024L))
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
    void testFindMonthTransactionStatusFailedByCardReturnsTwelveMonths() {
        repository.findMonthTransactionStatusFailedByCard(monthReq("CARD-1", 2024L))
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
    void testFindYearlyTransactionStatusSuccessByCardReturnsTwoYearsOrZeroData() {
        repository.findYearlyTransactionStatusSuccessByCard("CARD-1", 2024L)
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
    void testFindYearlyTransactionStatusFailedByCardReturnsTwoYearsOrZeroData() {
        repository.findYearlyTransactionStatusFailedByCard("CARD-1", 2024L)
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
