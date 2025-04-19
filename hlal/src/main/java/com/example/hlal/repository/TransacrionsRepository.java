package com.example.hlal.repository;

import com.example.hlal.model.Transactions;
import com.example.hlal.service.TransactionsService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransacrionsRepository extends JpaRepository<Transactions, Integer> {
}
