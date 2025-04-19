package com.example.hlal.service;

import com.example.hlal.dto.request.TransactionsRequest;
import com.example.hlal.dto.response.TransactionsResponse;
import com.example.hlal.model.Transactions;
import com.example.hlal.model.Wallets;
import com.example.hlal.repository.TransactionsRepository;
import com.example.hlal.repository.WalletsRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.example.hlal.model.TransactionType;
import com.example.hlal.repository.TransactionTypeRepository;
import com.example.hlal.repository.TopUpMethodRepository;
import java.util.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class TransactionsService {

    private final TransactionsRepository transactionsRepository;
    private final WalletsRepository walletsRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TopUpMethodRepository topUpMethodRepository;

    @Transactional
    public TransactionsResponse createTransaction(TransactionsRequest request) {
        // Ambil data dari request
        Wallets senderWallet = walletsRepository.findById(request.getSenderWalletId())
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));
        Wallets recipientWallet = walletsRepository.findById(request.getRecipientWalletId())
                .orElseThrow(() -> new RuntimeException("Recipient wallet not found"));
        TransactionType transactionType = transactionTypeRepository.findById(request.getTransactionTypeId())
                .orElseThrow(() -> new RuntimeException("Transaction type not found"));

        // Validasi dan update balance
        if ("Top Up".equalsIgnoreCase(transactionType.getName())) {
            if (request.getTopUpMethodId() == null) {
                throw new RuntimeException("Top up method is required for top up transactions");
            }

            senderWallet.setBalance(senderWallet.getBalance().add(request.getAmount()));
            walletsRepository.save(senderWallet);
        } else if ("Transfer".equalsIgnoreCase(transactionType.getName())) {
            if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
            recipientWallet.setBalance(recipientWallet.getBalance().add(request.getAmount()));
            walletsRepository.save(senderWallet);
            walletsRepository.save(recipientWallet);
        }

        // Simpan transaksi
        Transactions transaction = new Transactions();
        transaction.setWallet(senderWallet);
        transaction.setRecipientWallet(recipientWallet);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(LocalDateTime.now());

        transactionsRepository.save(transaction);

        // Balikin response
        TransactionsResponse response = new TransactionsResponse();
        response.setTransactionId(transaction.getId());
        response.setTransactionType(transactionType.getName());
        response.setAmount(transaction.getAmount());
        response.setSender(senderWallet.getUsers().getFullname());
        response.setRecipient(recipientWallet.getUsers().getFullname());
        response.setDescription(transaction.getDescription());
        response.setTransactionDate(transaction.getTransactionDate());

        return response;
    }

//
//    public List<Transactions> getFilteredTransactions(Long walletId, String keyword, String sortBy, String transactionType, Integer page, Integer limit) {
//        List<Transactions> transactions = transactionsRepository.findAll().stream()
//                .filter(tx -> tx.getWallet().getId().equals(walletId) ||
//                        (tx.getRecipientWallet() != null && tx.getRecipientWallet().getId().equals(walletId)))
//                .collect(Collectors.toList());
//
//        // Filter by transaction type if provided
//        if (transactionType != null && !transactionType.isEmpty()) {
//            transactions = transactions.stream()
//                    .filter(tx -> tx.getTransactionType() != null && transactionType.equalsIgnoreCase(tx.getTransactionType().getName()))
//                    .collect(Collectors.toList());
//        }
//
//        // Search by recipient name or description
//        if (keyword != null && !keyword.isEmpty()) {
//            transactions = transactions.stream()
//                    .filter(tx -> (tx.getRecipientWallet() != null && tx.getRecipientWallet().getUser().getFullname().toLowerCase().contains(keyword.toLowerCase())) ||
//                            (tx.getDescription() != null && tx.getDescription().toLowerCase().contains(keyword.toLowerCase())))
//                    .collect(Collectors.toList());
//        }
//
//        // Sort logic
//        if (sortBy != null) {
//            switch (sortBy) {
//                case "date":
//                    transactions.sort(Comparator.comparing(Transactions::getTransactionDate).reversed());
//                    break;
//                case "amount":
//                    transactions.sort(Comparator.comparing(Transactions::getAmount).reversed());
//                    break;
//                case "recipient":
//                    transactions.sort(Comparator.comparing(tx -> tx.getRecipientWallet() != null ? tx.getRecipientWallet().getUser().getFullname() : ""));
//                    break;
//                case "description":
//                    transactions.sort(Comparator.comparing(tx -> tx.getDescription() != null ? tx.getDescription() : ""));
//                    break;
//                default:
//                    break;
//            }
//        }
//
//        // Pagination and limit
//        int currentPage = (page != null && page >= 0) ? page : 0;
//        int pageSize = (limit != null && limit > 0) ? limit : 10; // default 10
//        int fromIndex = currentPage * pageSize;
//        int toIndex = Math.min(fromIndex + pageSize, transactions.size());
//
//        if (fromIndex >= transactions.size()) {
//            return new ArrayList<>();
//        }
//
//        return transactions.subList(fromIndex, toIndex);
//    }

}
