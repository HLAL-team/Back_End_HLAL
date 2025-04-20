package com.example.hlal.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private String status;
    private String message;
    private String token;
}
