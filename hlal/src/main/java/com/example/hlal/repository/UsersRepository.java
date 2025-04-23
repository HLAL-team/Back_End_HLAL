package com.example.hlal.repository;

import com.example.hlal.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    public Optional<Users> findByEmail(String email);
    public Users findFirstByEmail(String email);
    public Optional<Users> findByEmailAndPassword(String email, String password);
    public boolean existsByEmail(String email);
    public Optional<Users> findByUsername(String username);
}
