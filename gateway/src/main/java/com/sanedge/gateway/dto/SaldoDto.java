package com.sanedge.gateway.dto;

import java.util.List;

public class SaldoDto {

    public record SaldoResponse(
            String saldoId,
            String cardNumber,
            int totalBalance,
            String withdrawTime,
            int withdrawAmount,
            String createdAt,
            String updatedAt) {
        public static SaldoResponse from(pb.saldo.Saldo.SaldoResponse proto) {
            return new SaldoResponse(
                    proto.getSaldoId(),
                    proto.getCardNumber(),
                    proto.getTotalBalance(),
                    proto.getWithdrawTime(),
                    proto.getWithdrawAmount(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
        public static SaldoResponse from(pb.saldo.Saldo.SaldoResponseDeleteAt proto) {
            return new SaldoResponse(
                    proto.getSaldoId(),
                    proto.getCardNumber(),
                    proto.getTotalBalance(),
                    proto.getWithdrawTime(),
                    proto.getWithdrawAmount(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
    }

    public record FindAllSaldoResponse(
            List<SaldoResponse> data,
            String status,
            String message) {
        public static FindAllSaldoResponse from(pb.saldo.SaldoQuery.ApiResponsePaginationSaldo proto) {
            return new FindAllSaldoResponse(
                    proto.getDataList().stream().map(SaldoResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage());
        }
        public static FindAllSaldoResponse from(pb.saldo.SaldoQuery.ApiResponsePaginationSaldoDeleteAt proto) {
            return new FindAllSaldoResponse(
                    proto.getDataList().stream().map(SaldoResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record FindByIdSaldoResponse(
            SaldoResponse data,
            String status,
            String message) {
        public static FindByIdSaldoResponse from(pb.saldo.Saldo.ApiResponseSaldo proto) {
            return new FindByIdSaldoResponse(
                    proto.hasData() ? SaldoResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record CreateSaldoRequest(
            String cardNumber,
            int totalBalance) {
    }

    public record CreateSaldoResponse(
            SaldoResponse data,
            String status,
            String message) {
        public static CreateSaldoResponse from(pb.saldo.Saldo.ApiResponseSaldo proto) {
            return new CreateSaldoResponse(
                    proto.hasData() ? SaldoResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record UpdateSaldoRequest(
            String cardNumber,
            int totalBalance) {
    }

    public record UpdateSaldoResponse(
            SaldoResponse data,
            String status,
            String message) {
        public static UpdateSaldoResponse from(pb.saldo.Saldo.ApiResponseSaldo proto) {
            return new UpdateSaldoResponse(
                    proto.hasData() ? SaldoResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record UpdateSaldoWithdrawRequest(
            String cardNumber,
            int totalBalance,
            String withdrawTime,
            int withdrawAmount) {
    }

    public record UpdateSaldoBalanceRequest(
            String cardNumber,
            int totalBalance) {
    }

    public record TrashedSaldoResponse(
            SaldoResponse data,
            String status,
            String message) {
        public static TrashedSaldoResponse from(pb.saldo.Saldo.ApiResponseSaldoDeleteAt proto) {
            return new TrashedSaldoResponse(
                    proto.hasData() ? SaldoResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    @org.eclipse.microprofile.graphql.Name("SaldoSimpleStatusMessageResponse")
    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.saldo.SaldoCommand.ApiResponseSaldoDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.saldo.SaldoCommand.ApiResponseSaldoAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
