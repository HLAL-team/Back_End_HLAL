package com.example.hlal.service;


import com.example.hlal.dto.request.RegisterRequest;
import com.example.hlal.model.Users;
import com.example.hlal.model.Wallets;
import com.example.hlal.repository.UsersRepository;
import com.example.hlal.repository.WalletsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private WalletsRepository walletsRepository;

    public Users register(RegisterRequest registerRequest) {
        Optional<Users> checkUserByEmail = this.usersRepository.findByEmail(registerRequest.getEmail());
        String accountNumber;
        if (checkUserByEmail.isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        Users user = new Users();
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        user.setFullname(registerRequest.getFullname());
        user.setPassword(registerRequest.getPassword());

        Users savedUser = this.usersRepository.save(user);

        Wallets wallets = new Wallets();
        wallets.setUsers(savedUser);
        do{
            accountNumber = generateAccountNumber();
        }while(this.walletsRepository.findByAccountNumber(wallets.getAccountNumber()).isPresent());
        wallets.setAccountNumber(accountNumber);
        wallets.setBalance(BigDecimal.valueOf(0));
        wallets.setCreatedAt(LocalDateTime.now());
        wallets.setUpdatedAt(LocalDateTime.now());
        this.walletsRepository.save(wallets);

        return savedUser;
    }

    private String generateAccountNumber(){
        StringBuilder accountNumber = new StringBuilder();
        accountNumber.append(ThreadLocalRandom.current().nextInt(1,10));
        for (int i = 1; i < 10; i++) {
            accountNumber.append(ThreadLocalRandom.current().nextInt(0,10));
        }
        return accountNumber.toString();
    }
}
