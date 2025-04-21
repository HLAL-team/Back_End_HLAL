package com.example.hlal.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String username;
    private String fullname;
    private String password;
    private String phoneNumber;
}
