package com.bank.exceptions;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException(String accountNumber) {
        super("A transfer cannot use the same source and destination account: " + accountNumber);
    }
}
