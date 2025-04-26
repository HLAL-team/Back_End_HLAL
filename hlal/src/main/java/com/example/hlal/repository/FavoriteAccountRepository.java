package com.example.hlal.repository;

import com.example.hlal.model.FavoriteAccount;
import com.example.hlal.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteAccountRepository extends JpaRepository<FavoriteAccount, Long> {
    List<FavoriteAccount> findByUser(Users user);
    Optional<FavoriteAccount> findByUserAndFavoriteUser(Users user, Users favoriteUser);
    boolean existsByUserAndFavoriteUser(Users user, Users favoriteUser);
}


