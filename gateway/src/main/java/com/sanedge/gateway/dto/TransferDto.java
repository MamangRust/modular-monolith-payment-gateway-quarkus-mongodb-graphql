package com.sanedge.gateway.dto;

import java.util.List;

public class TransferDto {

        public record CreateTransferRequest(
                String transferFrom,
                String transferTo,
                int transferAmount) {}

        public record UpdateTransferRequest(
                String transferFrom,
                String transferTo,
                int transferAmount) {}

        public record TransferResponse(
                String id,
                String transferNo,
                String transferFrom,
                String transferTo,
                int transferAmount,
                String transferTime,
                String createdAt,
                String updatedAt) {
            public static TransferResponse from(pb.transfer.Transfer.TransferResponse proto) {
                return new TransferResponse(
                        proto.getId(),
                        proto.getTransferNo(),
                        proto.getTransferFrom(),
                        proto.getTransferTo(),
                        proto.getTransferAmount(),
                        proto.getTransferTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
            public static TransferResponse from(pb.transfer.Transfer.TransferResponseDeleteAt proto) {
                return new TransferResponse(
                        proto.getId(),
                        proto.getTransferNo(),
                        proto.getTransferFrom(),
                        proto.getTransferTo(),
                        proto.getTransferAmount(),
                        proto.getTransferTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record FindAllTransferResponse(
                List<TransferResponse> data,
                String status,
                String message) {
            public static FindAllTransferResponse from(pb.transfer.TransferQuery.ApiResponsePaginationTransfer proto) {
                return new FindAllTransferResponse(
                        proto.getDataList().stream().map(TransferResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllTransferResponse from(pb.transfer.TransferQuery.ApiResponsePaginationTransferDeleteAt proto) {
                return new FindAllTransferResponse(
                        proto.getDataList().stream().map(TransferResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllTransferResponse from(pb.transfer.TransferQuery.ApiResponseTransfers proto) {
                return new FindAllTransferResponse(
                        proto.getDataList().stream().map(TransferResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record FindByIdTransferResponse(
                TransferResponse data,
                String status,
                String message) {
            public static FindByIdTransferResponse from(pb.transfer.Transfer.ApiResponseTransfer proto) {
                return new FindByIdTransferResponse(
                        proto.hasData() ? TransferResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateTransferResponse(
                TransferResponse data,
                String status,
                String message) {
            public static CreateTransferResponse from(pb.transfer.Transfer.ApiResponseTransfer proto) {
                return new CreateTransferResponse(
                        proto.hasData() ? TransferResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateTransferResponse(
                TransferResponse data,
                String status,
                String message) {
            public static UpdateTransferResponse from(pb.transfer.Transfer.ApiResponseTransfer proto) {
                return new UpdateTransferResponse(
                        proto.hasData() ? TransferResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedTransferResponse(
                TransferResponse data,
                String status,
                String message) {
            public static TrashedTransferResponse from(pb.transfer.Transfer.ApIResponseTransferDeleteAt proto) {
                return new TrashedTransferResponse(
                        proto.hasData() ? TransferResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        @org.eclipse.microprofile.graphql.Name("TransferSimpleStatusMessageResponse")
        public record SimpleStatusMessageResponse(
                String status,
                String message) {
            public static SimpleStatusMessageResponse from(pb.transfer.TransferCommand.ApiResponseTransferDelete proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
        }
}
