package com.example.hlal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.security.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class Transactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transactions_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id")
    private Wallets wallet;

    @ManyToOne
    @JoinColumn(name = "recipient_wallet_id")
    private Wallets recipientWallet;

    @ManyToOne
    @JoinColumn(name = "transaction_type_id")
    private TransactionType transactionType;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
