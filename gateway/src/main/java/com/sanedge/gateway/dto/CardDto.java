package com.sanedge.gateway.dto;

import java.util.List;

public class CardDto {

        public record CardResponse(
                String id,
                String userId,
                String cardNumber,
                String cardType,
                String expireDate,
                String cvv,
                String cardProvider,
                String createdAt,
                String updatedAt) {
            public static CardResponse from(pb.card.Card.CardResponse proto) {
                return new CardResponse(
                        proto.getId(),
                        proto.getUserId(),
                        proto.getCardNumber(),
                        proto.getCardType(),
                        proto.getExpireDate(),
                        proto.getCvv(),
                        proto.getCardProvider(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
            public static CardResponse from(pb.card.Card.CardResponseDeleteAt proto) {
                return new CardResponse(
                        proto.getId(),
                        proto.getUserId(),
                        proto.getCardNumber(),
                        proto.getCardType(),
                        proto.getExpireDate(),
                        proto.getCvv(),
                        proto.getCardProvider(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record CardWithEmailResponse(
                String id,
                String userId,
                String email,
                String cardNumber,
                String cardType,
                String expireDate,
                String cvv,
                String cardProvider,
                String createdAt,
                String updatedAt) {
            public static CardWithEmailResponse from(pb.card.Card.CardWithEmailResponse proto) {
                return new CardWithEmailResponse(
                        proto.getId(),
                        proto.getUserId(),
                        proto.getEmail(),
                        proto.getCardNumber(),
                        proto.getCardType(),
                        proto.getExpireDate(),
                        proto.getCvv(),
                        proto.getCardProvider(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record FindAllCardResponse(
                List<CardResponse> data,
                String status,
                String message) {
            public static FindAllCardResponse from(pb.card.CardQuery.ApiResponsePaginationCard proto) {
                return new FindAllCardResponse(
                        proto.getDataList().stream().map(CardResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllCardResponse from(pb.card.CardQuery.ApiResponsePaginationCardDeleteAt proto) {
                return new FindAllCardResponse(
                        proto.getDataList().stream().map(CardResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record FindByIdCardResponse(
                CardResponse data,
                String status,
                String message) {
            public static FindByIdCardResponse from(pb.card.Card.ApiResponseCard proto) {
                return new FindByIdCardResponse(
                        proto.hasData() ? CardResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateCardRequest(
                String userId,
                String cardType,
                String expireDate,
                String cvv,
                String cardProvider) {}

        public record CreateCardResponse(
                CardResponse data,
                String status,
                String message) {
            public static CreateCardResponse from(pb.card.Card.ApiResponseCard proto) {
                return new CreateCardResponse(
                        proto.hasData() ? CardResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateCardRequest(
                String userId,
                String cardType,
                String expireDate,
                String cvv,
                String cardProvider) {}

        public record UpdateCardResponse(
                CardResponse data,
                String status,
                String message) {
            public static UpdateCardResponse from(pb.card.Card.ApiResponseCard proto) {
                return new UpdateCardResponse(
                        proto.hasData() ? CardResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedCardResponse(
                CardResponse data,
                String status,
                String message) {
            public static TrashedCardResponse from(pb.card.Card.ApiResponseCardDeleteAt proto) {
                return new TrashedCardResponse(
                        proto.hasData() ? CardResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        @org.eclipse.microprofile.graphql.Name("CardSimpleStatusMessageResponse")
        public record SimpleStatusMessageResponse(
                String status,
                String message) {
            public static SimpleStatusMessageResponse from(pb.card.CardCommand.ApiResponseCardDelete proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
            public static SimpleStatusMessageResponse from(pb.card.CardCommand.ApiResponseCardAll proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
        }
}
