package com.example.hlal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteAccountResponse {
    private Long id;
    private Long favoriteUserId;
    private String fullname;
    private String username;
    private String accountNumber;
    private String phoneNumber;
    private String avatarUrl;
}
