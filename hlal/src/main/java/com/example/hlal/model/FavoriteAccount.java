package com.example.hlal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User yang menyimpan favorite
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Users user;

    // User yang jadi favorite
    @ManyToOne
    @JoinColumn(name = "favorite_user_id", referencedColumnName = "id")
    private Users favoriteUser;

    private LocalDateTime createdAt = LocalDateTime.now();
}
