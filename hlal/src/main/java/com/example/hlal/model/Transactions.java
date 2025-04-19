package com.example.hlal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Long id;

    private Long wallet_id;//foreignkey dari wallets OneToManny

    @Column(name = "transaction_type", length = 20)
    private String  transaction_type;

    @Column(name = "amount", length = 12)
    private Integer amount;

    private Long recipient_wallet_id;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transaction_date;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
