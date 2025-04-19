package com.example.hlal.repository;

import com.example.hlal.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Integer> {
    public Optional<Users> findByEmail(String email);
}
