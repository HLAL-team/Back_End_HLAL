package com.example.hlal.repository;

import com.example.hlal.model.TopUpMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopUpMethodRepository extends JpaRepository<TopUpMethod, Long> {
    Optional<TopUpMethod> findTopUpMethodById(Long id);
}
