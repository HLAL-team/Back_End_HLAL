package com.example.hlal.dto.response;

import lombok.Data;

@Data
public class RecipientCheckResponse {
    private String status;
    private String message;
    private String recipientName;
}
