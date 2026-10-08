package com.sanedge.gateway.dto;

import java.util.List;

public class TransactionDto {

        public record CreateTransactionBody(
                        String cardNumber,
                        int amount,
                        String paymentMethod,
                        String merchantId) {
        }

        public record UpdateTransactionBody(
                        String cardNumber,
                        int amount,
                        String paymentMethod,
                        String merchantId) {
        }

        public record TransactionResponse(
                String id,
                String cardNumber,
                String transactionNo,
                int amount,
                String paymentMethod,
                String merchantId,
                String transactionTime,
                String createdAt,
                String updatedAt) {
            public static TransactionResponse from(pb.transaction.Transaction.TransactionResponse proto) {
                return new TransactionResponse(
                        proto.getId(),
                        proto.getCardNumber(),
                        proto.getTransactionNo(),
                        proto.getAmount(),
                        proto.getPaymentMethod(),
                        proto.getMerchantId(),
                        proto.getTransactionTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
            public static TransactionResponse from(pb.transaction.Transaction.TransactionResponseDeleteAt proto) {
                return new TransactionResponse(
                        proto.getId(),
                        proto.getCardNumber(),
                        proto.getTransactionNo(),
                        proto.getAmount(),
                        proto.getPaymentMethod(),
                        proto.getMerchantId(),
                        proto.getTransactionTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }

            public static TransactionResponse from(com.sanedge.common.adapter.model.Transaction t) {
                return new TransactionResponse(
                        t.id(),
                        t.cardNumber(),
                        t.transactionNo(),
                        t.amount(),
                        t.paymentMethod(),
                        t.merchantId(),
                        format(t.transactionTime()),
                        format(t.createdAt()),
                        format(t.updatedAt())
                );
            }

            private static String format(java.time.Instant instant) {
                return instant == null ? null : java.time.format.DateTimeFormatter.ISO_INSTANT.format(instant);
            }
        }

        public record FindAllTransactionResponse(
                List<TransactionResponse> data,
                String status,
                String message) {
            public static FindAllTransactionResponse from(pb.transaction.TransactionQuery.ApiResponsePaginationTransaction proto) {
                return new FindAllTransactionResponse(
                        proto.getDataList().stream().map(TransactionResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllTransactionResponse from(pb.transaction.TransactionQuery.ApiResponsePaginationTransactionDeleteAt proto) {
                return new FindAllTransactionResponse(
                        proto.getDataList().stream().map(TransactionResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllTransactionResponse from(pb.transaction.Transaction.ApiResponseTransactions proto) {
                return new FindAllTransactionResponse(
                        proto.getDataList().stream().map(TransactionResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }

            public static FindAllTransactionResponse from(List<com.sanedge.common.adapter.model.Transaction> items) {
                return new FindAllTransactionResponse(
                        items.stream().map(TransactionResponse::from).toList(),
                        "success",
                        "OK"
                );
            }
        }

        public record FindByIdTransactionResponse(
                TransactionResponse data,
                String status,
                String message) {
            public static FindByIdTransactionResponse from(pb.transaction.Transaction.ApiResponseTransaction proto) {
                return new FindByIdTransactionResponse(
                        proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateTransactionResponse(
                TransactionResponse data,
                String status,
                String message) {
            public static CreateTransactionResponse from(pb.transaction.Transaction.ApiResponseTransaction proto) {
                return new CreateTransactionResponse(
                        proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateTransactionResponse(
                TransactionResponse data,
                String status,
                String message) {
            public static UpdateTransactionResponse from(pb.transaction.Transaction.ApiResponseTransaction proto) {
                return new UpdateTransactionResponse(
                        proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedTransactionResponse(
                TransactionResponse data,
                String status,
                String message) {
            public static TrashedTransactionResponse from(pb.transaction.Transaction.ApiResponseTransactionDeleteAt proto) {
                return new TrashedTransactionResponse(
                        proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        @org.eclipse.microprofile.graphql.Name("TransactionSimpleStatusMessageResponse")
        public record SimpleStatusMessageResponse(
                String status,
                String message) {
            public static SimpleStatusMessageResponse from(pb.transaction.TransactionCommand.ApiResponseTransactionDelete proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
            public static SimpleStatusMessageResponse from(pb.transaction.TransactionCommand.ApiResponseTransactionAll proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
        }
}
