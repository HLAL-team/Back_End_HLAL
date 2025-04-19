package com.example.hlal.repository;

import com.example.hlal.model.Wallets;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletsRepository extends JpaRepository<Wallets, Integer> {
    public Optional<Wallets> findByAccountNumber(String accountNumber);
}
