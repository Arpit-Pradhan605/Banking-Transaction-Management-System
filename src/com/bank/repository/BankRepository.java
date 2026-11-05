package com.bank.repository;

import com.bank.exceptions.DuplicateCustomerException;
import com.bank.models.Account;
import com.bank.models.Customer;
import com.bank.models.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class BankRepository {
    private final Map<String, Customer> customers = new HashMap<>();
    private final Map<String, Account> accounts = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();

    public synchronized void addCustomer(Customer customer) {
        Objects.requireNonNull(customer, "customer");
        for (Customer existing : customers.values()) {
            if (existing.getUsername().equalsIgnoreCase(customer.getUsername())) {
                throw new DuplicateCustomerException(customer.getUsername());
            }
        }
        if (customers.containsKey(customer.getCustomerId())) {
            throw new IllegalArgumentException("Customer ID already exists: " + customer.getCustomerId());
        }
        customers.put(customer.getCustomerId(), customer);
    }

    public synchronized Customer getCustomer(String customerId) {
        return customers.get(customerId);
    }

    public synchronized void removeCustomer(String customerId) {
        customers.remove(customerId);
    }

    public synchronized void addAccount(Account account) {
        Objects.requireNonNull(account, "account");
        if (accounts.containsKey(account.getAccountNumber())) {
            throw new IllegalArgumentException("Account number already exists: " + account.getAccountNumber());
        }
        accounts.put(account.getAccountNumber(), account);
    }

    public synchronized Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public synchronized void removeAccount(String accountNumber) {
        accounts.remove(accountNumber);
    }

    public synchronized List<Customer> getAllCustomers() {
        return new ArrayList<>(customers.values());
    }

    public synchronized List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public synchronized void addTransaction(Transaction transaction) {
        transactions.add(Objects.requireNonNull(transaction, "transaction"));
    }

    public synchronized List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public synchronized Customer findCustomerByUsername(String username) {
        String normalizedUsername = username.toLowerCase(Locale.ROOT);
        for (Customer customer : customers.values()) {
            if (customer.getUsername().toLowerCase(Locale.ROOT).equals(normalizedUsername)) {
                return customer;
            }
        }
        return null;
    }
}
