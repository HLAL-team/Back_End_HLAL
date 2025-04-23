package com.example.hlal.dto.response;

import lombok.Data;

@Data
public class EditProfileResponse {
    private String status;
    private String username;
    private String message;
    private String avatarUrl;
}
