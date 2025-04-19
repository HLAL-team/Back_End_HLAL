package com.example.hlal.repository;

import com.example.hlal.model.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionsRepository extends JpaRepository<Transactions, Long> {
    List<Transactions> findByWalletId(Long walletId);
    List<Transactions> findByRecipientWalletId(Long recipientWalletId);
    List<Transactions> findByTransactionTypeId(Long transactionTypeId);
}
