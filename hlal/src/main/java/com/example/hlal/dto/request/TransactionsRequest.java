package com.example.hlal.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionsRequest {
    private Long senderWalletId;
    private BigDecimal amount;
    private String description;
    private Long transactionTypeId;
    private Long topUpMethodId;
    private String recipientAccountNumber;   // Optional: For transfer by account number
    private String recipientPhoneNumber;
}

