package com.example.hlal.service;

import com.example.hlal.dto.request.EditProfileRequest;
import com.example.hlal.dto.request.LoginRequest;
import com.example.hlal.dto.request.RegisterRequest;
import com.example.hlal.dto.response.EditProfileResponse;
import com.example.hlal.dto.response.LoginResponse;
import com.example.hlal.dto.response.RegisterResponse;
import com.example.hlal.dto.response.UserProfileResponse;
import com.example.hlal.model.Users;
import com.example.hlal.model.Wallets;
import com.example.hlal.repository.UsersRepository;
import com.example.hlal.repository.WalletsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private WalletsRepository walletsRepository;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private final JWTService jwtService;

    public RegisterResponse register(RegisterRequest registerRequest) {
        // Validasi input
        if (isNull(registerRequest.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field Email cannot be empty");
        }
        if (!isValidEmail(registerRequest.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }

        if (isNull(registerRequest.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field Username cannot be empty");
        }
        if (!isValidUsername(registerRequest.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be 5–20 characters and only contain letters, numbers, or underscores");
        }

        if (isNull(registerRequest.getFullname())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full Name cannot be empty");
        }
        if (!isValidFullname(registerRequest.getFullname())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name must only contain letters, spaces, periods, hyphens, and be up to 70 characters");
        }

        if (isNull(registerRequest.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field Phone Number cannot be empty");
        }
        if (!isValidPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number must be 10–15 digits");
        }

        if (isNull(registerRequest.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field Password cannot be empty");
        }
        if (!isValidPassword(registerRequest.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character");
        }

        // Duplikasi data
        if (usersRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }

        if (usersRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }

        if (usersRepository.findByPhoneNumber(registerRequest.getPhoneNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already in use");
        }

        // Buat user
        Users user = new Users();
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        user.setFullname(registerRequest.getFullname());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setAvatarUrl(null); // Avatar belum diupload

        Users savedUser = usersRepository.save(user);

        // Buat wallet
        Wallets wallets = new Wallets();
        wallets.setUsers(savedUser);

        String accountNumber;
        do {
            accountNumber = generateAccountNumber();
        } while (walletsRepository.findByAccountNumber(accountNumber).isPresent());

        wallets.setAccountNumber(accountNumber);
        wallets.setBalance(BigDecimal.ZERO);
        wallets.setCreatedAt(LocalDateTime.now());
        wallets.setUpdatedAt(LocalDateTime.now());

        walletsRepository.save(wallets);

        // Build response
        RegisterResponse response = new RegisterResponse();
        response.setStatus("Success");
        response.setMessage("Registration successful");
        response.setEmail(savedUser.getEmail());
        response.setFullname(savedUser.getFullname());
        response.setUsername(savedUser.getUsername());
        response.setPhoneNumber(savedUser.getPhoneNumber());
        response.setAvatarUrl(null);
        response.setAccountNumber(wallets.getAccountNumber());
        response.setBalance(wallets.getBalance());
        response.setCreatedAt(wallets.getCreatedAt());
        response.setUpdatedAt(wallets.getUpdatedAt());

        return response;
    }


    public EditProfileResponse editProfile(String email, EditProfileRequest request) {
        EditProfileResponse response = new EditProfileResponse();
        try {
            // Base URL otomatis untuk server
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

            Users user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isUsernameUpdated = false;
            boolean isPasswordUpdated = false;
            boolean isAvatarUpdated = false;

            // Update username
            if (request.getUsername() != null && !request.getUsername().isEmpty()) {
                if (!isValidUsername(request.getUsername())) {
                    throw new RuntimeException("Invalid username format");
                }

                if (request.getUsername().equals(user.getUsername())) {
                    throw new RuntimeException("New username must be different from the current username");
                }

                Optional<Users> existingUser = usersRepository.findByUsername(request.getUsername());
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    throw new RuntimeException("Username is already taken");
                }

                user.setUsername(request.getUsername());
                isUsernameUpdated = true;
            }

            // Update password
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                if (!isValidPassword(request.getPassword())) {
                    throw new RuntimeException("Password must be at least 8 characters and include uppercase, lowercase, digit, and special character");
                }

                if (BCrypt.checkpw(request.getPassword(), user.getPassword())) {
                    throw new RuntimeException("New password must be different from the current password");
                }

                user.setPassword(passwordEncoder.encode(request.getPassword()));
                isPasswordUpdated = true;
            }

            // Update avatar
            MultipartFile avatarFile = request.getAvatar();
            if (avatarFile != null && !avatarFile.isEmpty()) {
                // Path untuk upload file di root proyek
                String uploadDir = "uploads/";
                String originalFilename = avatarFile.getOriginalFilename();
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
                String fileName = System.currentTimeMillis() + "_" + user.getId() + fileExtension;
                Path filePath = Paths.get(uploadDir + fileName);

                // Pastikan foldernya ada
                Files.createDirectories(filePath.getParent());
                Files.copy(avatarFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                user.setAvatarUrl("/uploads/" + fileName);
                isAvatarUpdated = true;
            }

            usersRepository.save(user);

            // Update wallet timestamp
            Optional<Wallets> optionalWallet = walletsRepository.findByUsers(user);
            optionalWallet.ifPresent(wallet -> {
                wallet.setUpdatedAt(LocalDateTime.now());
                walletsRepository.save(wallet);
            });

            // Build response message
            StringBuilder messageBuilder = new StringBuilder("Profile updated successfully");
            if (isUsernameUpdated) messageBuilder.append(", username updated");
            if (isPasswordUpdated) messageBuilder.append(", password updated");
            if (isAvatarUpdated) messageBuilder.append(", avatar updated");

            response.setStatus("Success");
            response.setMessage(messageBuilder.toString());
            response.setUsername(user.getUsername());
            response.setAvatarUrl(user.getAvatarUrl() != null ? baseUrl + user.getAvatarUrl() : null);

            return response;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload avatar", e);
        } catch (Exception e) {
            throw new RuntimeException("Profile update failed: " + e.getMessage(), e);
        }
    }


    public LoginResponse login(LoginRequest loginRequest) {
        LoginResponse response = new LoginResponse();
        try {
            String usernameOrEmail = loginRequest.getUsernameOrEmail();
            Optional<Users> optionalUser;

            if(isNull(usernameOrEmail)){
                throw new RuntimeException("Username or email Field cannot be empty");
            }
            if (usernameOrEmail.contains("@")) {
                // Email login
                optionalUser = usersRepository.findByEmail(usernameOrEmail);
                if (optionalUser.isEmpty()) {
                    throw new RuntimeException("Wrong email");
                }
            } else {
                // Username login
                optionalUser = usersRepository.findByUsername(usernameOrEmail);
                if (optionalUser.isEmpty()) {
                    throw new RuntimeException("Wrong username");
                }
            }

            Users user = optionalUser.get();

            boolean isPasswordMatch = BCrypt.checkpw(loginRequest.getPassword(), user.getPassword());
            if (!isPasswordMatch) {
                throw new RuntimeException("Wrong password");
            }

            String token = jwtService.generateToken(user.getEmail());

            response.setStatus("Success");
            response.setMessage("Login successful");
            response.setToken(token);
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    public UserProfileResponse getProfile(String token) {
        try {
            String email = jwtService.extractUsername(token);
            Users user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Wallets wallet = walletsRepository.findByUsers(user)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            // Format date to a specific pattern
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");

            // Format the createdAt and updatedAt dates
            String formattedCreatedAt = wallet.getCreatedAt().format(formatter);
            String formattedUpdatedAt = wallet.getUpdatedAt().format(formatter);

            UserProfileResponse profile = new UserProfileResponse();
            profile.setFullname(user.getFullname());
            profile.setUsername(user.getUsername());
            profile.setEmail(user.getEmail());
            profile.setPhoneNumber(user.getPhoneNumber());
            profile.setAvatarUrl(user.getAvatarUrl());
            profile.setAccountNumber(wallet.getAccountNumber());
            profile.setBalance(wallet.getBalance());
            profile.setCreatedAt(formattedCreatedAt);  // Set the formatted date
            profile.setUpdatedAt(formattedUpdatedAt);  // Set the formatted date

            return profile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get profile: " + e.getMessage(), e);
        }
    }

    private String generateAccountNumber() {
        int maxRetries = 5; // Biar aman, maksimal 5x coba generate
        int attempts = 0;
        String accountNumber;

        do {
            StringBuilder builder = new StringBuilder();
            builder.append("7"); // Awal angka 7

            for (int i = 1; i < 10; i++) { // Total 10 digit
                builder.append(ThreadLocalRandom.current().nextInt(0, 10));
            }

            accountNumber = builder.toString();
            attempts++;

            if (attempts > maxRetries) {
                throw new RuntimeException("Failed to generate a unique account number after multiple attempts");
            }
        } while (walletsRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }

    private boolean isNull(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_]{5,20}$");
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=]).{8,}$");
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("^\\d{10,15}$");
    }

    private boolean isValidFullname(String fullname) {
        return fullname != null && fullname.matches("^[a-zA-Z\\s.'-]{1,70}$");
    }
}