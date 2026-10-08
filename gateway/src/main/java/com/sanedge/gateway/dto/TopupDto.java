package com.sanedge.gateway.dto;

import java.util.List;

public class TopupDto {

        public record CreateTopupRequest(
                String cardNumber,
                int topupAmount,
                String topupMethod) {}

        public record UpdateTopupRequest(
                String cardNumber,
                int topupAmount,
                String topupMethod) {}

        public record TopupResponse(
                String id,
                String cardNumber,
                String topupNo,
                int topupAmount,
                String topupMethod,
                String topupTime,
                String createdAt,
                String updatedAt) {
            public static TopupResponse from(pb.topup.Topup.TopupResponse proto) {
                return new TopupResponse(
                        proto.getId(),
                        proto.getCardNumber(),
                        proto.getTopupNo(),
                        proto.getTopupAmount(),
                        proto.getTopupMethod(),
                        proto.getTopupTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
            public static TopupResponse from(pb.topup.Topup.TopupResponseDeleteAt proto) {
                return new TopupResponse(
                        proto.getId(),
                        proto.getCardNumber(),
                        proto.getTopupNo(),
                        proto.getTopupAmount(),
                        proto.getTopupMethod(),
                        proto.getTopupTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record FindAllTopupResponse(
                List<TopupResponse> data,
                String status,
                String message) {
            public static FindAllTopupResponse from(pb.topup.TopupQuery.ApiResponsePaginationTopup proto) {
                return new FindAllTopupResponse(
                        proto.getDataList().stream().map(TopupResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllTopupResponse from(pb.topup.TopupQuery.ApiResponsePaginationTopupDeleteAt proto) {
                return new FindAllTopupResponse(
                        proto.getDataList().stream().map(TopupResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record FindByIdTopupResponse(
                TopupResponse data,
                String status,
                String message) {
            public static FindByIdTopupResponse from(pb.topup.Topup.ApiResponseTopup proto) {
                return new FindByIdTopupResponse(
                        proto.hasData() ? TopupResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateTopupResponse(
                TopupResponse data,
                String status,
                String message) {
            public static CreateTopupResponse from(pb.topup.Topup.ApiResponseTopup proto) {
                return new CreateTopupResponse(
                        proto.hasData() ? TopupResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateTopupResponse(
                TopupResponse data,
                String status,
                String message) {
            public static UpdateTopupResponse from(pb.topup.Topup.ApiResponseTopup proto) {
                return new UpdateTopupResponse(
                        proto.hasData() ? TopupResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedTopupResponse(
                TopupResponse data,
                String status,
                String message) {
            public static TrashedTopupResponse from(pb.topup.Topup.ApiResponseTopupDeleteAt proto) {
                return new TrashedTopupResponse(
                        proto.hasData() ? TopupResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        @org.eclipse.microprofile.graphql.Name("TopupSimpleStatusMessageResponse")
        public record SimpleStatusMessageResponse(
                String status,
                String message) {
            public static SimpleStatusMessageResponse from(pb.topup.TopupCommand.ApiResponseTopupDelete proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
            public static SimpleStatusMessageResponse from(pb.topup.TopupCommand.ApiResponseTopupAll proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
        }
}
