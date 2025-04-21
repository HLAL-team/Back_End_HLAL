package com.example.hlal.controller;

import com.example.hlal.dto.request.FavoriteAccountRequest;
import com.example.hlal.dto.request.TransactionsRequest;
import com.example.hlal.dto.response.FavoriteAccountResponse;
import com.example.hlal.dto.response.TransactionsResponse;
import com.example.hlal.service.TransactionsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionsController {

    private final TransactionsService transactionsService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTransaction(@RequestBody TransactionsRequest request,
                                                                 HttpServletRequest httpRequest) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            TransactionsResponse result = transactionsService.createTransaction(request, httpRequest);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"));
            String formattedDate = result.getTransactionDate().format(formatter);
            result.setTransactionDateFormatted(formattedDate);

            response.put("status", true);
            response.put("code", 201);
            response.put("data", result);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException ex) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"));
            response.put("timestamp", LocalDateTime.now().format(formatter));
            response.put("status", false);
            response.put("code", 400);
            response.put("error", "Bad Request");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyTransactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest
    ) {
        try {
            Map<String, Object> result = transactionsService.getMyTransactions(
                    keyword, sortBy, order, page, size, httpRequest
            );
            int statusCode = (int) result.getOrDefault("code", 200);
            return ResponseEntity.status(statusCode).body(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("status", false);
            errorResponse.put("code", 500);
            errorResponse.put("message", "Error fetching transactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> getTransactionsByTimeRange(
            @RequestParam String type,
            @RequestParam int year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer week,
            @RequestParam(required = false) Integer quarter,
            HttpServletRequest httpRequest
    ) {
        try {
            Map<String, Object> result = transactionsService.getTransactionsByTimeRange(type, year, month, week, quarter, httpRequest);
            int statusCode = (int) result.getOrDefault("code", 200);
            return ResponseEntity.status(statusCode).body(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("status", false);
            errorResponse.put("code", 500);
            errorResponse.put("message", "Error fetching transactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/favorite")
    public ResponseEntity<Map<String, Object>> addFavoriteAccount(
            @RequestBody FavoriteAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            FavoriteAccountResponse result = transactionsService.addFavoriteAccount(request, httpRequest);

            response.put("status", true);
            response.put("code", 201);
            response.put("data", result);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException ex) {
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"))));
            response.put("status", false);
            response.put("code", 400);
            response.put("error", "Bad Request");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/favorite")
    public ResponseEntity<Map<String, Object>> getFavoriteAccounts(HttpServletRequest httpRequest) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            List<FavoriteAccountResponse> favorites = transactionsService.getFavoriteAccounts(httpRequest);
            response.put("status", true);
            response.put("code", 200);
            response.put("data", favorites);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"))));
            response.put("status", false);
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/favorite")
    public ResponseEntity<Map<String, Object>> deleteFavoriteAccount(
            @RequestParam Long favoriteUserId,
            HttpServletRequest httpRequest
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            transactionsService.deleteFavoriteAccount(favoriteUserId, httpRequest);

            response.put("status", true);
            response.put("code", 200);
            response.put("message", "Favorite account deleted successfully");
            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"))));
            response.put("status", false);
            response.put("code", 400);
            response.put("error", "Bad Request");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }



}
