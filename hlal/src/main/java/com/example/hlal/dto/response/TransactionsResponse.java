package com.example.hlal.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionsResponse {
    private Long transactionId;
    private String transactionType;
    private BigDecimal amount;
    private String sender;
    private String recipient;
    private String description;
    private LocalDateTime transactionDate;
    private String transactionDateFormatted;
}
