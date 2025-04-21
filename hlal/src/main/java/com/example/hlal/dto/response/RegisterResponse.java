package com.example.hlal.dto.response;

import lombok.Data;

@Data
public class RegisterResponse {
    private String status;
    private String message;
    private String phoneNumber;
}
