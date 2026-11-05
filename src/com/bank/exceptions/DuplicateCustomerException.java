package com.bank.exceptions;

public class DuplicateCustomerException extends RuntimeException {
    public DuplicateCustomerException(String username) {
        super("A customer with username '" + username + "' already exists");
    }
}
