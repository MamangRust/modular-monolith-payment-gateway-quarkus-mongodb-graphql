package com.sanedge.migration;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * MongoDB counterpart of the former Flyway runner. Creates the indexes that
 * used to live in the V1..V29 SQL migrations and seeds the base roles, then
 * exits. {@code createIndex} is idempotent, so running it repeatedly is safe.
 */
@ApplicationScoped
public class MigrationLifecycle {

    private static final Logger LOGGER = Logger.getLogger(MigrationLifecycle.class);

    private static final List<String> BASE_ROLES = List.of("ROLE_USER", "ROLE_STAFF", "ROLE_ADMIN");

    @Inject
    MongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database", defaultValue = "PAYMENT_GATEWAY")
    String databaseName;

    void onStart(@Observes StartupEvent ev) {
        MongoDatabase db = mongoClient.getDatabase(databaseName);
        ensureIndexes(db);
        seedBaseRoles(db);
        LOGGER.info("MongoDB indexes ensured and base roles seeded. Shutting down initializer.");
        Quarkus.asyncExit();
    }

    private void ensureIndexes(MongoDatabase db) {
        // --- identity / auth ---
        unique(db, "users", "email");
        unique(db, "roles", "roleName");
        index(db, "user_roles", "userId");
        index(db, "user_roles", "roleId");
        index(db, "refresh_tokens", "userId");
        unique(db, "refresh_tokens", "token");
        index(db, "reset_tokens", "userId");
        unique(db, "reset_tokens", "token");

        // --- merchant ---
        unique(db, "merchants", "merchantNo");
        unique(db, "merchants", "apiKey");
        index(db, "merchants", "userId");
        index(db, "merchants", "deletedAt");
        index(db, "merchant_documents", "merchantId");
        index(db, "merchant_documents", "deletedAt");

        // --- cards ---
        unique(db, "cards", "cardNumber");
        index(db, "cards", "userId");
        index(db, "cards", "status");
        index(db, "cards", "deletedAt");
        index(db, "billing_statements", "cardNumber");
        index(db, "billing_statements", "deletedAt");
        index(db, "card_payments", "statementId");
        index(db, "card_rewards", "cardNumber");
        index(db, "card_rewards", "authTxnId");
        uniquePartial(db, "card_auth_transactions", "idempotencyKey", Filters.exists("idempotencyKey", true));
        index(db, "card_auth_transactions", "cardNumber");
        index(db, "card_auth_transactions", "deletedAt");
        uniquePartial(db, "card_event_logs", "topic,referenceId",
                Filters.exists("referenceId", true));

        // --- saldos ---
        uniquePartial(db, "saldos", "cardNumber", Filters.eq("deletedAt", null));
        index(db, "saldos", "cardNumber");
        index(db, "saldos", "deletedAt");

        // --- movements ---
        unique(db, "topups", "topupNo");
        index(db, "topups", "cardNumber");
        index(db, "topups", "status");
        index(db, "topups", "deletedAt");
        uniquePartial(db, "topups", "idempotencyKey", Filters.exists("idempotencyKey", true));

        unique(db, "transactions", "transactionNo");
        index(db, "transactions", "cardNumber");
        index(db, "transactions", "merchantId");
        index(db, "transactions", "status");
        index(db, "transactions", "transactionTime");
        index(db, "transactions", "deletedAt");
        uniquePartial(db, "transactions", "idempotencyKey", Filters.exists("idempotencyKey", true));

        unique(db, "transfers", "transferNo");
        index(db, "transfers", "transferFrom");
        index(db, "transfers", "transferTo");
        index(db, "transfers", "status");
        index(db, "transfers", "deletedAt");
        uniquePartial(db, "transfers", "idempotencyKey", Filters.exists("idempotencyKey", true));

        unique(db, "withdraws", "withdrawNo");
        index(db, "withdraws", "cardNumber");
        index(db, "withdraws", "status");
        index(db, "withdraws", "deletedAt");
        uniquePartial(db, "withdraws", "idempotencyKey", Filters.exists("idempotencyKey", true));

        // --- reconciliation worker scans ---
        for (String c : List.of("topups", "transactions", "transfers", "withdraws")) {
            index(db, c, "status,compensationRequiredAt");
            index(db, c, "status,compensationNextAttemptAt,compensationClaimedAt");
        }
    }

    private void seedBaseRoles(MongoDatabase db) {
        MongoCollection<Document> roles = db.getCollection("roles");
        Date now = Date.from(Instant.now());
        for (String roleName : BASE_ROLES) {
            if (roles.countDocuments(Filters.eq("roleName", roleName)) == 0) {
                roles.insertOne(new Document("roleName", roleName)
                        .append("createdAt", now)
                        .append("updatedAt", now)
                        .append("deletedAt", null));
                LOGGER.infof("Seeded base role %s", roleName);
            }
        }
    }

    private void index(MongoDatabase db, String collection, String fields) {
        db.getCollection(collection).createIndex(Indexes.ascending(split(fields)));
    }

    private void unique(MongoDatabase db, String collection, String fields) {
        db.getCollection(collection).createIndex(Indexes.ascending(split(fields)),
                new IndexOptions().unique(true));
    }

    private void uniquePartial(MongoDatabase db, String collection, String fields,
            org.bson.conversions.Bson partialFilter) {
        db.getCollection(collection).createIndex(Indexes.ascending(split(fields)),
                new IndexOptions().unique(true).partialFilterExpression(partialFilter));
    }

    private static List<String> split(String fields) {
        return List.of(fields.split(","));
    }
}
