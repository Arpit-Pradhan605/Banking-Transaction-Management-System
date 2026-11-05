package com.bank.exceptions;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountNumber) {
        super("Account '" + accountNumber + "' was not found");
    }
}
