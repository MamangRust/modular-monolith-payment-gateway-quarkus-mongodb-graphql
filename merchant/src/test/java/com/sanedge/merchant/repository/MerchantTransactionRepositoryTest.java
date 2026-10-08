package com.sanedge.merchant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import com.sanedge.common.enums.Status;
import com.sanedge.common.test.MongoDbResource;
import com.sanedge.common.test.MongoTestBase;
import com.sanedge.merchant.domain.requests.transactions.FindAllMerchantTransactions;
import com.sanedge.merchant.domain.requests.transactions.FindAllMerchantTransactionsByApiKey;
import com.sanedge.merchant.domain.requests.transactions.FindAllMerchantTransactionsById;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.entity.Transaction;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(MongoDbResource.class)
class MerchantTransactionRepositoryTest extends MongoTestBase {

    @Inject
    MerchantTransactionRepository merchantTransactionRepo;

    private static final String USER_ID = "507f1f77bcf86cd799439010";

    private Uni<Merchant> createMerchant(String name, String apiKey, Status status) {
        Merchant m = new Merchant();
        m.setName(name);
        m.setApiKey(apiKey);
        m.setUserId(new ObjectId(USER_ID));
        m.setStatus(status);
        m.setMerchantNo(java.util.UUID.randomUUID());
        Instant now = Instant.now();
        m.setCreatedAt(now);
        m.setUpdatedAt(now);
        return m.persist().map(v -> m);
    }

    private Uni<Merchant> createMerchant(String name, String apiKey) {
        return createMerchant(name, apiKey, Status.SUCCESS);
    }

    private Uni<Transaction> createTransaction(String merchantId, String cardNumber,
            int amount, String paymentMethod,
            Instant transactionTime) {
        Transaction t = new Transaction();
        t.setMerchantId(new ObjectId(merchantId));
        t.setCardNumber(cardNumber);
        t.setAmount(amount);
        t.setPaymentMethod(paymentMethod);
        t.setTransactionTime(transactionTime);
        t.setTransactionNo(java.util.UUID.randomUUID());
        t.setStatus(Status.SUCCESS);
        Instant now = Instant.now();
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        return t.persist().map(v -> t);
    }

    private static Instant at(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC);
    }

    private Uni<Void> cleanAll() {
        return Transaction.deleteAll()
                .chain(() -> Merchant.deleteAll())
                .replaceWithVoid();
    }

    // ---------- findAllTransactions ----------

    @Test
    void testFindAllTransactionsPagination() {
        cleanAll()
                .chain(() -> createMerchant("Shop1", "key1"))
                .chain(m -> createTransaction(m.getMerchantId(), "CARD-1", 1000, "CASH",
                        at(2026, 7, 1, 10, 0))
                        .chain(() -> createTransaction(m.getMerchantId(), "CARD-2", 2000, "CREDIT",
                                at(2026, 7, 1, 11, 0))))
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(1);
                    req.setPageSize(1);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(1);
                    assertThat(page1.getTotalRecords()).isEqualTo(2);
                    // sorted by transactionTime DESC, so latest first: CARD-2
                    assertThat(page1.getData().get(0).getCardNumber()).isEqualTo("CARD-2");
                })
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(2);
                    req.setPageSize(1);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(page2 -> {
                    assertThat(page2.getData()).hasSize(1);
                    assertThat(page2.getData().get(0).getCardNumber()).isEqualTo("CARD-1");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindAllTransactionsSearchByCardNumber() {
        cleanAll()
                .chain(() -> createMerchant("M1", "k1"))
                .chain(m -> createTransaction(m.getMerchantId(), "XYZ-111", 500, "CASH", Instant.now())
                        .chain(() -> createTransaction(m.getMerchantId(), "XYZ-222", 600, "CASH", Instant.now())))
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("111");
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getCardNumber()).isEqualTo("XYZ-111");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindAllTransactionsSearchByPaymentMethod() {
        cleanAll()
                .chain(() -> createMerchant("M2", "k2"))
                .chain(m -> createTransaction(m.getMerchantId(), "C1", 10, "QRIS", Instant.now())
                        .chain(() -> createTransaction(m.getMerchantId(), "C2", 20, "CREDIT", Instant.now())))
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("qris");
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getPaymentMethod()).isEqualTo("QRIS");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindAllTransactionsEmptySearchReturnsAll() {
        cleanAll()
                .chain(() -> createMerchant("M3", "k3"))
                .chain(m -> createTransaction(m.getMerchantId(), "A", 1, "CASH", Instant.now())
                        .chain(() -> createTransaction(m.getMerchantId(), "B", 2, "CASH", Instant.now())))
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindAllTransactionsNullSearchReturnsAll() {
        cleanAll()
                .chain(() -> createMerchant("M4", "k4"))
                .chain(m -> createTransaction(m.getMerchantId(), "C", 1, "DEBIT", Instant.now()))
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch(null);
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindAllTransactionsSorting() {
        Instant t1 = at(2026, 7, 1, 10, 0);
        Instant t2 = at(2026, 7, 1, 12, 0);
        Instant t3 = at(2026, 7, 1, 11, 0);

        cleanAll()
                .chain(() -> createMerchant("S", "sk"))
                .chain(m -> createTransaction(m.getMerchantId(), "C1", 100, "CASH", t1)
                        .chain(() -> createTransaction(m.getMerchantId(), "C2", 200, "CASH", t2))
                        .chain(() -> createTransaction(m.getMerchantId(), "C3", 300, "CASH", t3)))
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(r -> {
                    assertThat(r.getData()).hasSize(3);
                    // descending order by transactionTime
                    assertThat(r.getData().get(0).getTransactionTime()).isEqualTo(t2);
                    assertThat(r.getData().get(1).getTransactionTime()).isEqualTo(t3);
                    assertThat(r.getData().get(2).getTransactionTime()).isEqualTo(t1);
                })
                .replaceWithVoid().await().indefinitely();
    }

    // ---------- findAllTransactionsByMerchant ----------

    @Test
    void testFindAllTransactionsByMerchantBasic() {
        Merchant[] holder = new Merchant[1];
        cleanAll()
                .chain(() -> createMerchant("MerchA", "keyA"))
                .invoke(mA -> holder[0] = mA)
                .chain(mA -> createTransaction(mA.getMerchantId(), "XA", 500, "CASH", at(2026, 7, 1, 9, 0)))
                .chain(() -> createMerchant("MerchB", "keyB"))
                .chain(mB -> {
                    FindAllMerchantTransactionsById req = new FindAllMerchantTransactionsById();
                    req.setMerchantId(mB.getMerchantId());
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactionsByMerchant(req);
                })
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(0)) // no tx for B
                .chain(() -> {
                    FindAllMerchantTransactionsById req = new FindAllMerchantTransactionsById();
                    req.setMerchantId(holder[0].getMerchantId());
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactionsByMerchant(req);
                })
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getCardNumber()).isEqualTo("XA");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindAllTransactionsByMerchantWithSearch() {
        cleanAll()
                .chain(() -> createMerchant("MS", "ks"))
                .chain(m -> createTransaction(m.getMerchantId(), "FIND-1", 10, "CASH", Instant.now())
                        .chain(() -> createTransaction(m.getMerchantId(), "IGNORE-2", 20, "CASH", Instant.now()))
                        .replaceWith(m.getMerchantId()))
                .chain(merchantId -> {
                    FindAllMerchantTransactionsById req = new FindAllMerchantTransactionsById();
                    req.setMerchantId(merchantId);
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("FIND");
                    return merchantTransactionRepo.findAllTransactionsByMerchant(req);
                })
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getCardNumber()).isEqualTo("FIND-1");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindAllTransactionsByMerchantPagination() {
        cleanAll()
                .chain(() -> createMerchant("MP", "kp"))
                .chain(m -> {
                    Uni<Void> chain = Uni.createFrom().voidItem();
                    for (int i = 1; i <= 3; i++) {
                        int num = i;
                        chain = chain.chain(() -> createTransaction(m.getMerchantId(),
                                "C" + num, num * 100, "CASH", Instant.now()).replaceWithVoid());
                    }
                    return chain.replaceWith(m.getMerchantId());
                })
                .chain(merchantId -> {
                    FindAllMerchantTransactionsById req = new FindAllMerchantTransactionsById();
                    req.setMerchantId(merchantId);
                    req.setPage(1);
                    req.setPageSize(2);
                    return merchantTransactionRepo.findAllTransactionsByMerchant(req);
                })
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(3);
                })
                .chain(page1 -> {
                    FindAllMerchantTransactionsById req = new FindAllMerchantTransactionsById();
                    req.setMerchantId(page1.getData().get(0).getMerchantId());
                    req.setPage(2);
                    req.setPageSize(2);
                    return merchantTransactionRepo.findAllTransactionsByMerchant(req);
                })
                .invoke(page2 -> assertThat(page2.getData()).hasSize(1))
                .replaceWithVoid().await().indefinitely();
    }

    // ---------- findAllTransactionsByApiKey ----------

    @Test
    void testFindAllTransactionsByApiKeyBasic() {
        cleanAll()
                .chain(() -> createMerchant("ApiShop", "secret-123"))
                .chain(m -> createTransaction(m.getMerchantId(), "API-1", 999, "QRIS", at(2026, 7, 5, 12, 0)))
                .chain(() -> createMerchant("OtherShop", "other-key"))
                .chain(() -> {
                    FindAllMerchantTransactionsByApiKey req = new FindAllMerchantTransactionsByApiKey();
                    req.setApiKey("secret-123");
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactionsByApiKey(req);
                })
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getAmount()).isEqualTo(999L);
                    assertThat(r.getData().get(0).getMerchantName()).isEqualTo("ApiShop");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindAllTransactionsByApiKeyWithSearch() {
        cleanAll()
                .chain(() -> createMerchant("KeyShop", "apikey-X"))
                .chain(m -> createTransaction(m.getMerchantId(), "KEY-1", 111, "DEBIT", Instant.now())
                        .chain(() -> createTransaction(m.getMerchantId(), "OTHER-2", 222, "CREDIT", Instant.now())))
                .chain(() -> {
                    FindAllMerchantTransactionsByApiKey req = new FindAllMerchantTransactionsByApiKey();
                    req.setApiKey("apikey-X");
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("DEBIT");
                    return merchantTransactionRepo.findAllTransactionsByApiKey(req);
                })
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getPaymentMethod()).isEqualTo("DEBIT");
                })
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testFindAllTransactionsByApiKeyNotFoundApiKey() {
        cleanAll()
                .chain(() -> createMerchant("Real", "real"))
                .chain(() -> {
                    FindAllMerchantTransactionsByApiKey req = new FindAllMerchantTransactionsByApiKey();
                    req.setApiKey("nonexistent");
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactionsByApiKey(req);
                })
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid().await().indefinitely();
    }

    // ---------- edge cases ----------

    @Test
    void testEmptyDatabaseReturnsZeroRecords() {
        cleanAll()
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testPageZeroDefaultsToFirstPage() {
        cleanAll()
                .chain(() -> createMerchant("P0", "k0"))
                .chain(m -> createTransaction(m.getMerchantId(), "Z", 1, "CASH", Instant.now()))
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(0);
                    req.setPageSize(10);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testNegativePageDefaultsToFirstPage() {
        cleanAll()
                .chain(() -> createMerchant("NEG", "nk"))
                .chain(m -> createTransaction(m.getMerchantId(), "N", 5, "CASH", Instant.now()))
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(-1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid().await().indefinitely();
    }

    @Test
    void testZeroSizeDefaultsToTen() {
        cleanAll()
                .chain(() -> createMerchant("ZS", "zs"))
                .chain(m -> createTransaction(m.getMerchantId(), "Z2", 10, "CASH", Instant.now()))
                .chain(() -> {
                    FindAllMerchantTransactions req = new FindAllMerchantTransactions();
                    req.setPage(1);
                    req.setPageSize(0);
                    req.setSearch("");
                    return merchantTransactionRepo.findAllTransactions(req);
                })
                .invoke(r -> assertThat(r.getData()).hasSizeLessThanOrEqualTo(10))
                .replaceWithVoid().await().indefinitely();
    }
}
