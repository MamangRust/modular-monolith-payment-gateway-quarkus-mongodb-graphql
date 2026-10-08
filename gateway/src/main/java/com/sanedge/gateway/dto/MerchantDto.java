package com.sanedge.gateway.dto;

import java.util.List;

public class MerchantDto {

    public record MerchantResponse(
            String id,
            String name,
            String apiKey,
            String status,
            String userId,
            String createdAt,
            String updatedAt) {
        public static MerchantResponse from(pb.merchant.Merchant.MerchantResponse proto) {
            return new MerchantResponse(
                    proto.getId(),
                    proto.getName(),
                    proto.getApiKey(),
                    proto.getStatus(),
                    proto.getUserId(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static MerchantResponse from(pb.merchant.Merchant.MerchantResponseDeleteAt proto) {
            return new MerchantResponse(
                    proto.getId(),
                    proto.getName(),
                    proto.getApiKey(),
                    proto.getStatus(),
                    proto.getUserId(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }

        public static MerchantResponse from(com.sanedge.common.adapter.model.Merchant m) {
            return new MerchantResponse(
                    m.id(),
                    m.name(),
                    m.apiKey(),
                    m.status(),
                    m.userId(),
                    format(m.createdAt()),
                    format(m.updatedAt())
            );
        }

        private static String format(java.time.Instant instant) {
            return instant == null ? null : java.time.format.DateTimeFormatter.ISO_INSTANT.format(instant);
        }
    }

    public record FindAllMerchantResponse(
            List<MerchantResponse> data,
            String status,
            String message) {
        public static FindAllMerchantResponse from(pb.merchant.MerchantQuery.ApiResponsePaginationMerchant proto) {
            return new FindAllMerchantResponse(
                    proto.getDataList().stream().map(MerchantResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllMerchantResponse from(pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt proto) {
            return new FindAllMerchantResponse(
                    proto.getDataList().stream().map(MerchantResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllMerchantResponse from(pb.merchant.Merchant.ApiResponsesMerchant proto) {
            return new FindAllMerchantResponse(
                    proto.getDataList().stream().map(MerchantResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static FindByIdMerchantResponse from(pb.merchant.Merchant.ApiResponseMerchant proto) {
            return new FindByIdMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }

        public static FindByIdMerchantResponse from(com.sanedge.common.adapter.model.Merchant m) {
            return new FindByIdMerchantResponse(
                    m == null ? null : MerchantResponse.from(m),
                    "success",
                    "OK"
            );
        }
    }

    public record CreateMerchantRequest(
            String userId,
            String name) {}

    public record CreateMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static CreateMerchantResponse from(pb.merchant.Merchant.ApiResponseMerchant proto) {
            return new CreateMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateMerchantRequest(
            String userId,
            String name,
            String status) {}

    public record UpdateMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static UpdateMerchantResponse from(pb.merchant.Merchant.ApiResponseMerchant proto) {
            return new UpdateMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TrashedMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static TrashedMerchantResponse from(pb.merchant.Merchant.ApiResponseMerchantDeleteAt proto) {
            return new TrashedMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("MerchantSimpleStatusMessageResponse")
    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.merchant.MerchantCommand.ApiResponseMerchantDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.merchant.MerchantCommand.ApiResponseMerchantAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
