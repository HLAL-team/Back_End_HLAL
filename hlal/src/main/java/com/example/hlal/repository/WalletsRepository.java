package com.example.hlal.repository;

import com.example.hlal.model.Wallets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletsRepository extends JpaRepository<Wallets, Integer> {
}
