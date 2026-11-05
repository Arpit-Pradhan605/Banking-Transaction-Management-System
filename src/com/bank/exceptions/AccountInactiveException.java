package com.bank.exceptions;

public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException(String accountNumber) {
        super("Account '" + accountNumber + "' is not active");
    }
}
