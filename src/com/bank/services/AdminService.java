package com.bank.services;

import com.bank.enums.AccountStatus;
import com.bank.enums.TransactionType;
import com.bank.exceptions.AccountNotFoundException;
import com.bank.models.Account;
import com.bank.models.Customer;
import com.bank.models.Transaction;
import com.bank.repository.BankRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AdminService {
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final BankRepository repository;
    private final String username;
    private final String password;

    public AdminService(BankRepository repository) {
        this(repository, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    public AdminService(BankRepository repository, String username, String password) {
        this.repository = Objects.requireNonNull(repository, "repository");
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Admin username and password must not be blank");
        }
        this.username = username;
        this.password = password;
    }

    public boolean login(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }

    public void freezeAccount(String accountNumber) {
        getAccount(accountNumber).setStatus(AccountStatus.FROZEN);
    }

    public void unfreezeAccount(String accountNumber) {
        getAccount(accountNumber).setStatus(AccountStatus.ACTIVE);
    }

    public List<Account> viewAllAccounts() {
        return repository.getAllAccounts();
    }

    public List<Customer> viewAllCustomers() {
        return repository.getAllCustomers();
    }

    public Map<String, Number> getStatistics() {
        List<Customer> customers = repository.getAllCustomers();
        List<Account> accounts = repository.getAllAccounts();
        double totalDeposits = 0;
        for (Transaction transaction : repository.getAllTransactions()) {
            if (transaction.getType() == TransactionType.DEPOSIT) {
                totalDeposits += transaction.getAmount();
            }
        }

        Map<String, Number> statistics = new LinkedHashMap<>();
        statistics.put("totalCustomers", customers.size());
        statistics.put("totalAccounts", accounts.size());
        statistics.put("totalDeposits", totalDeposits);
        return statistics;
    }

    private Account getAccount(String accountNumber) {
        Account account = repository.getAccount(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }
}
