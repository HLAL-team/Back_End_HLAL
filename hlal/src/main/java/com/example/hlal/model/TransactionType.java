package com.example.hlal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transaction_type")
public class TransactionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 255)
    private String name;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transactions transaction;

    @OneToMany(mappedBy = "transactionType")
    private List<Transactions> transactions;

    @OneToMany(mappedBy = "transactionType")
    private List<TopUpMethod> topUpMethods;
}
