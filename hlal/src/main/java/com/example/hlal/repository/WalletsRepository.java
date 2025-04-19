package com.example.hlal.repository;

import com.example.hlal.model.Wallets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletsRepository extends JpaRepository<Wallets, Long> {
    Optional<Wallets> findByAccountNumber(String accountNumber);
    Optional<Wallets> findByUsersId(Long userId);
}
