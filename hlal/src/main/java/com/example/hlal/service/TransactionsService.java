package com.example.hlal.service;

import com.example.hlal.dto.request.FavoriteAccountRequest;
import com.example.hlal.dto.request.RecipientCheckRequest;
import com.example.hlal.dto.request.TransactionsRequest;
import com.example.hlal.dto.response.FavoriteAccountResponse;
import com.example.hlal.dto.response.RecipientCheckResponse;
import com.example.hlal.dto.response.TransactionsResponse;
import com.example.hlal.model.*;
import com.example.hlal.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionsService {

    private final TransactionsRepository transactionsRepository;
    private final WalletsRepository walletsRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TopUpMethodRepository topUpMethodRepository;
    private final UsersRepository usersRepository;
    private final FavoriteAccountRepository favoriteAccountRepository;
    private final JWTService jwtService;

    @Transactional
    public TransactionsResponse createTransaction(TransactionsRequest request, HttpServletRequest httpRequest) {
        String jwt = jwtService.extractToken(httpRequest);
        String userEmail = jwtService.extractUsername(jwt);

        Wallets senderWallet = walletsRepository.findByUsersEmail(userEmail)
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
                throw new RuntimeException("Top-up method is required for top-up transactions");
            }

            TopUpMethod topUpMethod = topUpMethodRepository.findById(request.getTopUpMethodId())
                    .orElseThrow(() -> new RuntimeException("Top-up method not found"));

            transaction.setTopUpMethod(topUpMethod);
            transaction.setRecipientWallet(null);

            senderWallet.setBalance(senderWallet.getBalance().add(request.getAmount()));
            walletsRepository.save(senderWallet);

        } else if (transactionType.getId() == 2) { // TRANSFER
            Wallets recipientWallet = null;

            if (request.getRecipientAccountNumber() != null && !request.getRecipientAccountNumber().isEmpty()) {
                recipientWallet = walletsRepository.findByAccountNumber(request.getRecipientAccountNumber())
                        .orElseThrow(() -> new RuntimeException("Recipient wallet not found with account number"));
            } else if (request.getRecipientPhoneNumber() != null && !request.getRecipientPhoneNumber().isEmpty()) {
                recipientWallet = walletsRepository.findByUsersPhoneNumber(request.getRecipientPhoneNumber())
                        .orElseThrow(() -> new RuntimeException("Recipient wallet not found with phone number"));
            } else {
                throw new RuntimeException("Recipient account number or phone number is required for transfer transactions");
            }

            if (recipientWallet.getId().equals(senderWallet.getId())) {
                throw new RuntimeException("You can't transfer to your own wallet");
            }

            if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            transaction.setRecipientWallet(recipientWallet);
            transaction.setTopUpMethod(null);

            senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
            recipientWallet.setBalance(recipientWallet.getBalance().add(request.getAmount()));
            walletsRepository.save(senderWallet);
            walletsRepository.save(recipientWallet);

        } else {
            throw new RuntimeException("Unsupported transaction type");
        }

        transactionsRepository.save(transaction);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"));
        String formattedDate = transaction.getTransactionDate().format(formatter);

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
    }

    public Map<String, Object> getMyTransactions(
            String search,
            String sortBy,
            String sortDir,
            Integer page,
            Integer limit,
            HttpServletRequest httpRequest) {

        String jwt = jwtService.extractToken(httpRequest);
        String userEmail = jwtService.extractUsername(jwt);
        Wallets wallet = walletsRepository.findByUsersEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        List<Transactions> sentTransactions = transactionsRepository.findByWalletId(wallet.getId());
        List<Transactions> receivedTransactions = transactionsRepository.findByRecipientWalletId(wallet.getId());

        List<Transactions> allTransactions = new ArrayList<>();
        allTransactions.addAll(sentTransactions);
        allTransactions.addAll(receivedTransactions);

        List<TransactionsResponse> filtered = allTransactions.stream()
                .filter(tx -> {
                    if (search == null || search.isEmpty()) return true;
                    return tx.getDescription().toLowerCase().contains(search.toLowerCase()) ||
                            tx.getTransactionType().getName().toLowerCase().contains(search.toLowerCase()) ||
                            (tx.getRecipientWallet() != null &&
                                    tx.getRecipientWallet().getUsers().getFullname().toLowerCase().contains(search.toLowerCase()));
                })
                .map(tx -> {
                    TransactionsResponse res = new TransactionsResponse();
                    res.setTransactionId(tx.getId());
                    res.setTransactionType(tx.getTransactionType().getName());
                    res.setAmount(tx.getAmount());
                    res.setSender(tx.getWallet().getUsers().getFullname());
                    res.setRecipient(tx.getRecipientWallet() != null ? tx.getRecipientWallet().getUsers().getFullname() : null);
                    res.setDescription(tx.getDescription());
                    res.setTransactionDate(tx.getTransactionDate());
                    res.setTransactionDateFormatted(tx.getTransactionDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm")));
                    return res;
                })
                .collect(Collectors.toList());

        Comparator<TransactionsResponse> comparator = Comparator.comparing(TransactionsResponse::getTransactionDate);
        if ("desc".equalsIgnoreCase(sortDir)) comparator = comparator.reversed();
        filtered.sort(comparator);

        if (filtered.isEmpty()) {
            return Map.of(
                    "status", false,
                    "code", 404,
                    "message", "No data found",
                    "totalData", 0,
                    "totalIncome", BigDecimal.ZERO,
                    "totalOutcome", BigDecimal.ZERO,
                    "data", Collections.emptyList()
            );
        }

        // Calculate totalIncome and totalOutcome
        BigDecimal totalIncome = allTransactions.stream()
                .filter(tx -> (tx.getRecipientWallet() != null && tx.getRecipientWallet().getId().equals(wallet.getId()))
                        || (tx.getTransactionType().getId() == 1))
                .map(Transactions::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutcome = allTransactions.stream()
                .filter(tx -> tx.getWallet().getId().equals(wallet.getId()) && tx.getTransactionType().getId() != 1)
                .map(Transactions::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalData = filtered.size();
        int fromIndex = Math.min((page - 1) * limit, totalData);
        int toIndex = Math.min(fromIndex + limit, totalData);
        List<TransactionsResponse> paginated = filtered.subList(fromIndex, toIndex);

        return Map.of(
                "status", true,
                "code", 200,
                "message", "Data retrieved successfully",
                "totalData", totalData,
                "totalIncome", totalIncome,
                "totalOutcome", totalOutcome,
                "data", paginated
        );
    }

    public Map<String, Object> getTransactionsByTimeRange(
            String type,
            Integer year,
            Integer month,
            Integer week,
            Integer quarter,
            Integer startYear,
            Integer endYear,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new LinkedHashMap<>();
        try {
            String jwt = jwtService.extractToken(httpRequest);
            String userEmail = jwtService.extractUsername(jwt);
            Wallets wallet = walletsRepository.findByUsersEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            LocalDateTime start;
            LocalDateTime end;

            switch (type.toLowerCase()) {
                case "daily":
                    if (week == null || month == null || year == null)
                        throw new IllegalArgumentException("Week, month, and year are required for daily filter");
                    LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
                    LocalDate weekStart = firstDayOfMonth.with(DayOfWeek.MONDAY).plusWeeks(week - 1);
                    start = weekStart.atStartOfDay();
                    end = start.plusDays(6).withHour(23).withMinute(59).withSecond(59);
                    break;
                case "weekly":
                    if (month == null || year == null)
                        throw new IllegalArgumentException("Month and year are required for weekly filter");
                    LocalDate first = LocalDate.of(year, month, 1);
                    start = first.with(DayOfWeek.MONDAY).atStartOfDay();
                    end = first.plusMonths(1).minusDays(1).with(DayOfWeek.SUNDAY).atTime(23, 59, 59);
                    break;
                case "monthly":
                    if (year == null)
                        throw new IllegalArgumentException("Year is required for monthly filter");
                    start = LocalDateTime.of(year, 1, 1, 0, 0);
                    end = LocalDateTime.of(year, 12, 31, 23, 59, 59);
                    break;
                case "quarterly":
                    if (year == null)
                        throw new IllegalArgumentException("Year is required for quarterly filter");
                    start = LocalDateTime.of(year, 1, 1, 0, 0);
                    end = LocalDateTime.of(year, 12, 31, 23, 59, 59);
                    break;
                case "yearly":
                    if (startYear == null || endYear == null)
                        throw new IllegalArgumentException("Start year and end year are required for yearly filter");
                    start = LocalDateTime.of(startYear, 1, 1, 0, 0);
                    end = LocalDateTime.of(endYear, 12, 31, 23, 59, 59);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid type: must be 'daily', 'weekly', 'monthly', 'quarterly', or 'yearly'");
            }

            List<Transactions> sentTransactions = transactionsRepository.findByWalletIdAndTransactionDateBetween(wallet.getId(), start, end);
            List<Transactions> receivedTransactions = transactionsRepository.findByRecipientWalletIdAndTransactionDateBetween(wallet.getId(), start, end);

            List<Transactions> allTransactions = new ArrayList<>();
            allTransactions.addAll(sentTransactions);
            allTransactions.addAll(receivedTransactions);

            if (allTransactions.isEmpty()) {
                response.put("status", false);
                response.put("code", 404);
                response.put("message", "No data found");
                return response;
            }

            BigDecimal totalIncome = allTransactions.stream()
                    .filter(tx -> (tx.getRecipientWallet() != null && tx.getRecipientWallet().getId().equals(wallet.getId()))
                            || (tx.getTransactionType().getId() == 1))
                    .map(Transactions::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalOutcome = allTransactions.stream()
                    .filter(tx -> tx.getWallet().getId().equals(wallet.getId()) && tx.getTransactionType().getId() != 1)
                    .map(Transactions::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<TransactionsResponse> responseList = allTransactions.stream().map(tx -> {
                TransactionsResponse res = new TransactionsResponse();
                res.setTransactionId(tx.getId());
                res.setTransactionType(tx.getTransactionType().getName());
                res.setAmount(tx.getAmount());
                res.setSender(tx.getWallet().getUsers().getFullname());
                res.setRecipient(tx.getRecipientWallet() != null ? tx.getRecipientWallet().getUsers().getFullname() : null);
                res.setDescription(tx.getDescription());
                res.setTransactionDate(tx.getTransactionDate());
                res.setTransactionDateFormatted(tx.getTransactionDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm")));
                return res;
            }).collect(Collectors.toList());

            response.put("status", true);
            response.put("code", 200);
            response.put("message", "Data retrieved successfully");
            response.put("totalData", responseList.size());
            response.put("totalIncome", totalIncome);
            response.put("totalOutcome", totalOutcome);
            response.put("data", responseList);
            return response;

        } catch (Exception e) {
            response.put("status", false);
            response.put("code", 500);
            response.put("message", "Error fetching transactions: " + e.getMessage());
            return response;
        }
    }


    public FavoriteAccountResponse addFavoriteAccount(FavoriteAccountRequest request, HttpServletRequest httpRequest) {
        try {
            // Ambil email dari JWT token
            String email = jwtService.extractUsername(jwtService.extractToken(httpRequest));
            Users currentUser = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Cari wallet berdasarkan account number yang diberikan
            Wallets favoriteWallet = walletsRepository.findByAccountNumber(request.getFavoriteAccountNumber())
                    .orElseThrow(() -> new RuntimeException("Favorite account not found"));

            Users favoriteUser = favoriteWallet.getUsers();

            // Tidak boleh menambahkan diri sendiri sebagai favorite
            if (favoriteUser.getId().equals(currentUser.getId())) {
                throw new RuntimeException("You cannot add your own account as a favorite");
            }

            // Cek jika sudah ada favorite yang sama
            boolean exists = favoriteAccountRepository.existsByUserAndFavoriteUser(currentUser, favoriteUser);
            if (exists) {
                throw new RuntimeException("This account is already in your favorites");
            }

            // Buat dan simpan FavoriteAccount baru
            FavoriteAccount favoriteAccount = new FavoriteAccount();
            favoriteAccount.setUser(currentUser);
            favoriteAccount.setFavoriteUser(favoriteUser);
            favoriteAccount.setCreatedAt(LocalDateTime.now());
            favoriteAccountRepository.save(favoriteAccount);

            return buildResponse(favoriteAccount);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add favorite account: " + e.getMessage());
        }
    }

    public List<FavoriteAccountResponse> getFavoriteAccounts(HttpServletRequest httpRequest) {
        try {
            String email = jwtService.extractUsername(jwtService.extractToken(httpRequest));
            Users currentUser = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<FavoriteAccount> favorites = favoriteAccountRepository.findByUser(currentUser);

            return favorites.stream()
                    .map(this::buildResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch favorite accounts: " + e.getMessage());
        }
    }

    public void deleteFavoriteAccount(String favoriteAccountNumber, HttpServletRequest httpRequest) {
        try {
            String email = jwtService.extractUsername(jwtService.extractToken(httpRequest));
            Users currentUser = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Wallets favoriteWallet = walletsRepository.findByAccountNumber(favoriteAccountNumber)
                    .orElseThrow(() -> new RuntimeException("Favorite account not found"));

            Users favoriteUser = favoriteWallet.getUsers();

            FavoriteAccount favoriteAccount = favoriteAccountRepository.findByUserAndFavoriteUser(currentUser, favoriteUser)
                    .orElseThrow(() -> new RuntimeException("Favorite account not found"));

            favoriteAccountRepository.delete(favoriteAccount);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete favorite account: " + e.getMessage());
        }
    }

    private FavoriteAccountResponse buildResponse(FavoriteAccount favoriteAccount) {
        Users favoriteUser = favoriteAccount.getFavoriteUser();
        Wallets favoriteWallet = favoriteUser.getWallets();

        return new FavoriteAccountResponse(
                favoriteAccount.getId(),
                favoriteUser.getId(),
                favoriteUser.getFullname(),
                favoriteUser.getUsername(),
                favoriteWallet.getAccountNumber(),
                favoriteUser.getPhoneNumber(),
                favoriteUser.getAvatarUrl()
        );
    }
    public List<TopUpMethod> getTopUpMethods(HttpServletRequest httpRequest) {
        try{
            String jwt = jwtService.extractToken(httpRequest);
            String userEmail = jwtService.extractUsername(jwt);
            usersRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return topUpMethodRepository.findAll();
        } catch (Exception e) {
            throw new RuntimeException("Failed to show list of topup methods: " + e.getMessage());
        }
    }

    public RecipientCheckResponse checkingRecepientAccountNumber(RecipientCheckRequest recipientCheckRequest, HttpServletRequest httpRequest) {
        RecipientCheckResponse response = new RecipientCheckResponse();
        try{
            String jwt = jwtService.extractToken(httpRequest);
            String userEmail = jwtService.extractUsername(jwt);
            usersRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String CheckAccount = recipientCheckRequest.getRecipientAccountNumber().trim();
            Optional<Wallets> checkrecipwallet;
            checkrecipwallet = walletsRepository.findByAccountNumber(CheckAccount);
            if(checkrecipwallet.isPresent()){
                Wallets wallets = checkrecipwallet.get();
                String recipientName = wallets.getUsers().getFullname();
                response.setStatus("Success");
                response.setMessage("Account found: " + recipientName);
                response.setRecipientName(recipientName);
            }else {
                response.setStatus("Failed");
                response.setMessage("Account not found: " + CheckAccount);
            }

        }catch (Exception e){
            throw new RuntimeException("Failed to checking recepient account number: " + e.getMessage());
        }
        return response;
    }

}
