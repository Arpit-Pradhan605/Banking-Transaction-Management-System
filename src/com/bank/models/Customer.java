package com.bank.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Customer {
    private final String customerId;
    private final String username;
    private final String password;
    private final List<Account> accounts = new ArrayList<>();

    public Customer(String customerId, String username, String password) {
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public synchronized void addAccount(Account account) {
        Objects.requireNonNull(account, "account");
        for (Account existing : accounts) {
            if (existing.getAccountNumber().equals(account.getAccountNumber())) {
                return;
            }
        }
        accounts.add(account);
    }

    public synchronized void removeAccount(Account account) {
        accounts.remove(account);
    }

    public synchronized List<Account> getAccounts() {
        return new ArrayList<>(accounts);
    }
}
