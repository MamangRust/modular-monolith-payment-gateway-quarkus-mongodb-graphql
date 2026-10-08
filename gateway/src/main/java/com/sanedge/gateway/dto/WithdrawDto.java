package com.sanedge.gateway.dto;

import java.util.List;

public class WithdrawDto {

        public record CreateWithdrawBody(
                        String cardNumber,
                        int withdrawAmount) {
        }

        public record UpdateWithdrawBody(
                        String cardNumber,
                        int withdrawAmount) {
        }

        public record WithdrawResponse(
                String withdrawId,
                String withdrawNo,
                String cardNumber,
                int withdrawAmount,
                String withdrawTime,
                String createdAt,
                String updatedAt) {
            public static WithdrawResponse from(pb.withdraw.Withdraw.WithdrawResponse proto) {
                return new WithdrawResponse(
                        proto.getWithdrawId(),
                        proto.getWithdrawNo(),
                        proto.getCardNumber(),
                        proto.getWithdrawAmount(),
                        proto.getWithdrawTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
            public static WithdrawResponse from(pb.withdraw.Withdraw.WithdrawResponseDeleteAt proto) {
                return new WithdrawResponse(
                        proto.getWithdrawId(),
                        proto.getWithdrawNo(),
                        proto.getCardNumber(),
                        proto.getWithdrawAmount(),
                        proto.getWithdrawTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record FindAllWithdrawResponse(
                List<WithdrawResponse> data,
                String status,
                String message) {
            public static FindAllWithdrawResponse from(pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdraw proto) {
                return new FindAllWithdrawResponse(
                        proto.getDataList().stream().map(WithdrawResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllWithdrawResponse from(pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdrawDeleteAt proto) {
                return new FindAllWithdrawResponse(
                        proto.getDataList().stream().map(WithdrawResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllWithdrawResponse from(pb.withdraw.Withdraw.ApiResponsesWithdraw proto) {
                return new FindAllWithdrawResponse(
                        proto.getDataList().stream().map(WithdrawResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record FindByIdWithdrawResponse(
                WithdrawResponse data,
                String status,
                String message) {
            public static FindByIdWithdrawResponse from(pb.withdraw.Withdraw.ApiResponseWithdraw proto) {
                return new FindByIdWithdrawResponse(
                        proto.hasData() ? WithdrawResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateWithdrawResponse(
                WithdrawResponse data,
                String status,
                String message) {
            public static CreateWithdrawResponse from(pb.withdraw.Withdraw.ApiResponseWithdraw proto) {
                return new CreateWithdrawResponse(
                        proto.hasData() ? WithdrawResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateWithdrawResponse(
                WithdrawResponse data,
                String status,
                String message) {
            public static UpdateWithdrawResponse from(pb.withdraw.Withdraw.ApiResponseWithdraw proto) {
                return new UpdateWithdrawResponse(
                        proto.hasData() ? WithdrawResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedWithdrawResponse(
                WithdrawResponse data,
                String status,
                String message) {
            public static TrashedWithdrawResponse from(pb.withdraw.Withdraw.ApIResponseWithdrawDeleteAt proto) {
                return new TrashedWithdrawResponse(
                        proto.hasData() ? WithdrawResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        @org.eclipse.microprofile.graphql.Name("WithdrawSimpleStatusMessageResponse")
        public record SimpleStatusMessageResponse(
                String status,
                String message) {
            public static SimpleStatusMessageResponse from(pb.withdraw.WithdrawCommand.ApiResponseWithdrawDelete proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
        }
}
