package com.bank.services;

import com.bank.exceptions.DuplicateCustomerException;
import com.bank.models.Customer;
import com.bank.repository.BankRepository;
import com.bank.utils.IdGenerator;
import com.bank.utils.ValidationUtil;

import java.util.List;
import java.util.Objects;

public class CustomerService {
    private final BankRepository repository;

    public CustomerService(BankRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public synchronized Customer registerCustomer(String username, String password) {
        if (!ValidationUtil.isValidUsername(username)) {
            throw new IllegalArgumentException("Username must contain at least 4 characters");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must contain at least 6 characters");
        }
        if (repository.findCustomerByUsername(username.trim()) != null) {
            throw new DuplicateCustomerException(username.trim());
        }

        Customer customer = new Customer(IdGenerator.generateCustomerId(), username.trim(), password);
        repository.addCustomer(customer);
        return customer;
    }

    public Customer login(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        Customer customer = repository.findCustomerByUsername(username.trim());
        if (customer == null || !customer.getPassword().equals(password)) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        return customer;
    }

    public Customer getCustomerById(String customerId) {
        Customer customer = repository.getCustomer(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer '" + customerId + "' was not found");
        }
        return customer;
    }

    public Customer getCustomerByUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        Customer customer = repository.findCustomerByUsername(username.trim());
        if (customer == null) {
            throw new IllegalArgumentException("Customer '" + username + "' was not found");
        }
        return customer;
    }

    public List<Customer> getAllCustomers() {
        return repository.getAllCustomers();
    }
}
