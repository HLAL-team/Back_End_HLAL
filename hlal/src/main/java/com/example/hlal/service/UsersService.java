package com.example.hlal.service;

import com.example.hlal.dto.request.EditProfileRequest;
import com.example.hlal.dto.request.LoginRequest;
import com.example.hlal.dto.request.RegisterRequest;
import com.example.hlal.dto.response.EditProfileResponse;
import com.example.hlal.dto.response.LoginResponse;
import com.example.hlal.dto.response.RegisterResponse;
import com.example.hlal.model.Users;
import com.example.hlal.model.Wallets;
import com.example.hlal.repository.UsersRepository;
import com.example.hlal.repository.WalletsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
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
        RegisterResponse response = new RegisterResponse();
        try {
            if (!isValidEmail(registerRequest.getEmail())) {
                throw new RuntimeException("Invalid email format");
            }

            if (!isValidUsername(registerRequest.getUsername())) {
                throw new RuntimeException("Username must be 5–20 characters and only contain letters, numbers, or underscores");
            }

            if (!isValidFullname(registerRequest.getFullname())) {
                throw new RuntimeException("Full name must only contain letters, spaces, periods, hyphens, and be up to 70 characters");
            }

            if (!isValidPhoneNumber(registerRequest.getPhoneNumber())) {
                throw new RuntimeException("Phone number must be 10–15 digits");
            }

            String password = registerRequest.getPassword();
            if (!isValidPassword(password)) {
                throw new RuntimeException("Password must be at least 8 characters long and include uppercase, lowercase, number, and special character");
            }

            if (usersRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
                throw new RuntimeException("Email is already in use");
            }

            if (usersRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
                throw new RuntimeException("Username is already taken");
            }

            Users user = new Users();
            user.setEmail(registerRequest.getEmail());
            user.setUsername(registerRequest.getUsername());
            user.setFullname(registerRequest.getFullname());
            user.setPhoneNumber(registerRequest.getPhoneNumber());
            user.setPassword(passwordEncoder.encode(password));
            user.setAvatarUrl(null); // Tidak upload avatar

            Users savedUser = usersRepository.save(user);

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
            response.setStatus("Success");
            response.setMessage("Berhasil Registrasi");
            response.setEmail(savedUser.getEmail());
            response.setFullname(savedUser.getFullname());
            response.setUsername(savedUser.getUsername());
            response.setPhoneNumber(savedUser.getPhoneNumber());
            response.setAvatarUrl(null);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }


    public EditProfileResponse editProfile(String email, EditProfileRequest request) {
        EditProfileResponse response = new EditProfileResponse();
        try {
            Users user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isUsernameUpdated = false;
            boolean isPasswordUpdated = false;
            boolean isAvatarUpdated = false;

            if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
                if (!isValidUsername(request.getUsername())) {
                    response.setStatus("Error");
                    response.setMessage("Invalid username format");
                    response.setUsername(user.getUsername());
                    response.setAvatarUrl(user.getAvatarUrl());
                    return response;
                }

                if (usersRepository.findByUsername(request.getUsername()).isPresent()) {
                    response.setStatus("Error");
                    response.setMessage("Username is already taken");
                    response.setUsername(user.getUsername());
                    response.setAvatarUrl(user.getAvatarUrl());
                    return response;
                }

                user.setUsername(request.getUsername());
                isUsernameUpdated = true;
            }

            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                if (!isValidPassword(request.getPassword())) {
                    response.setStatus("Error");
                    response.setMessage("Password must be at least 8 characters and include uppercase, lowercase, digit, and special character");
                    response.setUsername(user.getUsername());
                    response.setAvatarUrl(user.getAvatarUrl());
                    return response;
                }

                if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(request.getPassword()));
                    isPasswordUpdated = true;
                } else {
                    response.setStatus("Error");
                    response.setMessage("New password must be different from the current password");
                    response.setUsername(user.getUsername());
                    response.setAvatarUrl(user.getAvatarUrl());
                    return response;
                }
            }

            MultipartFile avatarFile = request.getAvatar();
            if (avatarFile != null && !avatarFile.isEmpty()) {
                String uploadDir = "src/main/resources/static/uploads/";
                String originalFilename = avatarFile.getOriginalFilename();
                String fileName = System.currentTimeMillis() + "_" + originalFilename;
                Path filePath = Paths.get(uploadDir + fileName);

                Files.createDirectories(filePath.getParent());
                Files.copy(avatarFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                user.setAvatarUrl("/uploads/" + fileName);
                isAvatarUpdated = true;
            }

            usersRepository.save(user);

            StringBuilder messageBuilder = new StringBuilder("Profile updated successfully");
            if (isUsernameUpdated) messageBuilder.append(", username updated");
            if (isPasswordUpdated) messageBuilder.append(", password updated");
            if (isAvatarUpdated) messageBuilder.append(", avatar updated");

            response.setStatus("Success");
            response.setMessage(messageBuilder.toString());
            response.setAvatarUrl(user.getAvatarUrl());
            response.setUsername(user.getUsername());
            return response;

        } catch (IOException e) {
            response.setStatus("Error");
            response.setMessage("Failed to upload avatar");
            response.setUsername(null);
            response.setAvatarUrl(null);
            return response;
        } catch (Exception e) {
            response.setStatus("Error");
            response.setMessage("Profile update failed: " + e.getMessage());
            response.setUsername(null);
            response.setAvatarUrl(null);
            return response;
        }
    }

//    public String login(LoginRequest loginRequest) {
//        try {
//            Optional<Users> optionalUser = usersRepository.findByEmail(loginRequest.getEmail());
//            if (optionalUser.isEmpty()) {
//                throw new RuntimeException("Wrong email");
//            }
//
//            Users user = optionalUser.get();
//            boolean isPasswordMatch = BCrypt.checkpw(loginRequest.getPassword(), user.getPassword());
//            if (!isPasswordMatch) {
//                throw new RuntimeException("Wrong password");
//            }
//
//            return jwtService.generateToken(user.getEmail());
//        } catch (Exception e) {
//            throw new RuntimeException("Login failed: " + e.getMessage(), e);
//        }
//    }

    public LoginResponse login(LoginRequest loginRequest) {
        LoginResponse response = new LoginResponse();
        try {
            Optional<Users> optionalUser = usersRepository.findByEmail(loginRequest.getEmail());
            if (optionalUser.isEmpty()) {
                throw new RuntimeException("Wrong email");
            }

            Users user = optionalUser.get();
            boolean isPasswordMatch = BCrypt.checkpw(loginRequest.getPassword(), user.getPassword());
            if (!isPasswordMatch) {
                throw new RuntimeException("Wrong password");
            }

            String token = jwtService.generateToken(user.getEmail());

            response.setStatus("Success");
            response.setMessage("Berhasil Login");
            response.setToken(token);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }



    private String generateAccountNumber() {
        StringBuilder accountNumber = new StringBuilder();
        accountNumber.append(ThreadLocalRandom.current().nextInt(1, 10));
        for (int i = 1; i < 10; i++) {
            accountNumber.append(ThreadLocalRandom.current().nextInt(0, 10));
        }
        return accountNumber.toString();
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

