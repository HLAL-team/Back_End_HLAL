package com.example.hlal.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wallets")
@Entity
public class Wallets {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long user_id; //foreign key untuk users OnetoOne

    @Column(name = "account_number", nullable = false, length = 20)
    private String account_number;

    @Column(name = "balance", nullable = false, length = 12)
    private Integer balance;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime created_at;

    @Column(name = "updated_at")
    private LocalDateTime updated_at;
}
