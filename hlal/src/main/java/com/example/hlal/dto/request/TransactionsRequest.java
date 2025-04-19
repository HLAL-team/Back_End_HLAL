package com.example.hlal.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionsRequest {
    private Long senderWalletId;
    private Long recipientWalletId;
    private Long transactionTypeId;
    private BigDecimal amount;
    private String description;
    private Long topUpMethodId; // hanya wajib kalau tipe transaksinya Top Up
}

