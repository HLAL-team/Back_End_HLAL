package com.example.hlal.service;

import com.example.hlal.dto.request.TransactionsRequest;
import com.example.hlal.dto.response.TransactionsResponse;
import com.example.hlal.model.TopUpMethod;
import com.example.hlal.model.Transactions;
import com.example.hlal.model.Wallets;
import com.example.hlal.repository.TransactionsRepository;
import com.example.hlal.repository.WalletsRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        try {
            Wallets senderWallet = walletsRepository.findById(request.getSenderWalletId())
                    .orElseThrow(() -> new RuntimeException("Sender wallet not found"));
            TransactionType transactionType = transactionTypeRepository.findById(request.getTransactionTypeId())
                    .orElseThrow(() -> new RuntimeException("Transaction type not found"));

            Transactions transaction = new Transactions();
            transaction.setWallet(senderWallet);
            transaction.setTransactionType(transactionType);
            transaction.setAmount(request.getAmount());
            transaction.setDescription(request.getDescription());
            transaction.setTransactionDate(LocalDateTime.now());

            if (transactionType.getId() == 1) { // TOP UP
                if (request.getTopUpMethodId() == null) {
                    throw new RuntimeException("Top up method is required for top up transactions");
                }

                TopUpMethod topUpMethod = topUpMethodRepository.findById(request.getTopUpMethodId())
                        .orElseThrow(() -> new RuntimeException("Top up method not found"));

                transaction.setTopUpMethod(topUpMethod);
                transaction.setRecipientWallet(null); // gak perlu recipient

                senderWallet.setBalance(senderWallet.getBalance().add(request.getAmount()));
                walletsRepository.save(senderWallet);
            } else if (transactionType.getId() == 2) { // TRANSFER
                if (request.getRecipientWalletId() == null) {
                    throw new RuntimeException("Recipient wallet is required for transfer transactions");
                }

                Wallets recipientWallet = walletsRepository.findById(request.getRecipientWalletId())
                        .orElseThrow(() -> new RuntimeException("Recipient wallet not found"));

                if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
                    throw new RuntimeException("Insufficient balance");
                }

                transaction.setRecipientWallet(recipientWallet);
                transaction.setTopUpMethod(null); // gak perlu top up method

                senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
                recipientWallet.setBalance(recipientWallet.getBalance().add(request.getAmount()));
                walletsRepository.save(senderWallet);
                walletsRepository.save(recipientWallet);
            } else {
                throw new RuntimeException("Unsupported transaction type");
            }

            transactionsRepository.save(transaction);

            // Format tanggal ke "18 April 2025 19:00"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"));
            String formattedDate = transaction.getTransactionDate().format(formatter);

            // Build response
            TransactionsResponse response = new TransactionsResponse();
            response.setTransactionId(transaction.getId());
            response.setTransactionType(transactionType.getName());
            response.setAmount(transaction.getAmount());
            response.setSender(senderWallet.getUsers().getFullname());
            response.setRecipient(transaction.getRecipientWallet() != null
                    ? transaction.getRecipientWallet().getUsers().getFullname()
                    : null);
            response.setDescription(transaction.getDescription());
            response.setTransactionDate(transaction.getTransactionDate());
            response.setTransactionDateFormatted(formattedDate);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create transaction: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getMyTransactions(Long walletId, String keyword, String sortBy, String order,
                                                 int page, int size, Integer limit) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Transactions> allTransactions = transactionsRepository.findAll();

            // Filter by walletId
            allTransactions = allTransactions.stream()
                    .filter(tx -> tx.getWallet().getId().equals(walletId))
                    .collect(Collectors.toList());

            // Filter keyword
            String lowerKeyword = keyword != null ? keyword.toLowerCase() : null;
            List<Transactions> filtered = allTransactions.stream().filter(tx -> {
                if (lowerKeyword == null || lowerKeyword.isEmpty()) return true;

                String description = Optional.ofNullable(tx.getDescription()).orElse("").toLowerCase();
                String amount = tx.getAmount() != null ? tx.getAmount().toPlainString() : "";
                String transactionType = tx.getTransactionType() != null ? tx.getTransactionType().getName().toLowerCase() : "";
                String sender = tx.getWallet() != null && tx.getWallet().getUsers() != null
                        ? tx.getWallet().getUsers().getFullname().toLowerCase() : "";
                String recipient = tx.getRecipientWallet() != null && tx.getRecipientWallet().getUsers() != null
                        ? tx.getRecipientWallet().getUsers().getFullname().toLowerCase() : "";

                return description.contains(lowerKeyword) ||
                        amount.contains(lowerKeyword) ||
                        transactionType.contains(lowerKeyword) ||
                        sender.contains(lowerKeyword) ||
                        recipient.contains(lowerKeyword);
            }).collect(Collectors.toList());

            // Sort
            filtered.sort((tx1, tx2) -> {
                int result;
                switch (sortBy.toLowerCase()) {
                    case "amount":
                        result = tx1.getAmount().compareTo(tx2.getAmount());
                        break;
                    case "description":
                        result = Optional.ofNullable(tx1.getDescription()).orElse("")
                                .compareToIgnoreCase(Optional.ofNullable(tx2.getDescription()).orElse(""));
                        break;
                    case "recipient":
                        String r1 = tx1.getRecipientWallet() != null && tx1.getRecipientWallet().getUsers() != null
                                ? tx1.getRecipientWallet().getUsers().getFullname() : "";
                        String r2 = tx2.getRecipientWallet() != null && tx2.getRecipientWallet().getUsers() != null
                                ? tx2.getRecipientWallet().getUsers().getFullname() : "";
                        result = r1.compareToIgnoreCase(r2);
                        break;
                    case "transactiontype":
                        String t1 = tx1.getTransactionType() != null ? tx1.getTransactionType().getName() : "";
                        String t2 = tx2.getTransactionType() != null ? tx2.getTransactionType().getName() : "";
                        result = t1.compareToIgnoreCase(t2);
                        break;
                    case "date":
                    default:
                        result = tx1.getTransactionDate().compareTo(tx2.getTransactionDate());
                        break;
                }
                return "desc".equalsIgnoreCase(order) ? -result : result;
            });

            // Pagination
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, filtered.size());
            if (fromIndex >= filtered.size()) {
                response.put("status", true);
                response.put("code", 201);
                response.put("data", Collections.emptyList());
                return response;
            }

            List<Transactions> paginated = filtered.subList(fromIndex, toIndex);

            // Apply limit only if provided
            if (limit != null && limit > 0 && limit < paginated.size()) {
                paginated = paginated.subList(0, limit);
            }


            // Final response list
            List<Map<String, Object>> data = paginated.stream().map(tx -> {
                Map<String, Object> item = new LinkedHashMap<>();

                String name = tx.getTransactionType().getId() == 1
                        ? tx.getWallet().getUsers().getFullname()
                        : tx.getRecipientWallet() != null
                        ? tx.getRecipientWallet().getUsers().getFullname()
                        : "-";

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd MMMM yyyy", new Locale("en"));
                String formattedDate = tx.getTransactionDate().format(formatter);

                item.put("transaction_id", tx.getId());
                item.put("date", formattedDate);
                item.put("type", tx.getTransactionType().getName());
                item.put("from_to", name);
                item.put("notes", tx.getDescription());
                item.put("amount", tx.getAmount());

                return item;
            }).collect(Collectors.toList());

            response.put("status", true);
            response.put("code", 201);
            response.put("totalData", filtered.size());
            response.put("data", data);
            return response;

        } catch (Exception e) {
            response.put("status", false);
            response.put("code", 500);
            response.put("message", "Failed to get transactions: " + e.getMessage());
            return response;
        }
    }


}
