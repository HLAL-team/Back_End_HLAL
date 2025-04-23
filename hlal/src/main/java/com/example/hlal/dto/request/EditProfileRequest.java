package com.example.hlal.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EditProfileRequest {
    private String username;
    private String password;
    private MultipartFile avatar;
}
