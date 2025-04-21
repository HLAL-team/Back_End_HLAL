package com.example.hlal.service;


import com.example.hlal.dto.request.LoginRequest;
import com.example.hlal.dto.request.RegisterRequest;
import com.example.hlal.dto.response.LoginResponse;
import com.example.hlal.model.Users;
import com.example.hlal.model.Wallets;
import com.example.hlal.repository.UsersRepository;
import com.example.hlal.repository.WalletsRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@Service
public class UsersService {
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private WalletsRepository walletsRepository;


    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final JWTService jwtService;


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
        user.setPhoneNumber(registerRequest.getPhoneNumber());
//        user.setPassword(registerRequest.getPassword());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

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

    public String login(LoginRequest loginRequest) {
        Optional<Users> optionalUser = usersRepository.findByEmail(loginRequest.getEmail());
        System.out.println(optionalUser.isPresent());
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Email Wrong");
        }

        Users user = optionalUser.get();

        boolean isPasswordMatch = BCrypt.checkpw(loginRequest.getPassword(), user.getPassword());
        System.out.println(isPasswordMatch);
        if (!isPasswordMatch) {
            throw new RuntimeException("Password Wrong");
        }

//        authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        loginRequest.getEmail(),
//                        loginRequest.getPassword()
//                )
//        );
        String token = jwtService.generateToken(user.getEmail());
        return token;
    }

}
