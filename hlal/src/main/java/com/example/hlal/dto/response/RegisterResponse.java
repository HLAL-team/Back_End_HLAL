package com.example.hlal.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RegisterResponse {
    private String status;
    private String message;
    private String email;
    private String username;
    private String fullname;
    private String phoneNumber;
    private String avatarUrl;
    private String accountNumber;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
