package com.example.hlal.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserProfileResponse {
    private String fullname;
    private String username;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private String accountNumber;
    private BigDecimal balance;
    private String createdAt;
    private String updatedAt;
}
