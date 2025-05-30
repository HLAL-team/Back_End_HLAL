package com.example.hlal.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class RegisterRequest {
    private String email;
    private String username;
    private String fullname;
    private String password;
    private String phoneNumber;
    private MultipartFile avatar;
}
