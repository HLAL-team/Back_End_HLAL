package com.example.hlal.controller;

import com.example.hlal.dto.request.TransactionsRequest;
import com.example.hlal.dto.response.TransactionsResponse;
import com.example.hlal.model.Transactions;
import com.example.hlal.service.TransactionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionsController {

    private final TransactionsService transactionsService;

    @PostMapping("/create")
    public ResponseEntity<TransactionsResponse> createTransaction(@RequestBody TransactionsRequest request) {
        System.out.println(request);
        return ResponseEntity.ok(transactionsService.createTransaction(request));
    }
//
//    @GetMapping("/list")
//    public ResponseEntity<List<TransactionsResponse>> getTransactions(
//            @RequestParam(required = false) String keyword,
//            @RequestParam(required = false) String sortBy,
//            @RequestParam(required = false) String sortDir,
//            @RequestParam(required = false) String transactionType,
//            @RequestParam(required = false, defaultValue = "0") int page,
//            @RequestParam(required = false, defaultValue = "10") int size
//    ) {
//        return ResponseEntity.ok(transactionsService.getFilteredTransactions(keyword, sortBy, sortDir, transactionType, page, size));
//    }
//
//    @GetMapping("/limit")
//    public ResponseEntity<List<TransactionsResponse>> getRecentTransactions(
//            @RequestParam(defaultValue = "10") int limit
//    ) {
//        return ResponseEntity.ok(transactionsService.getRecentTransactions(limit));
//    }
}
