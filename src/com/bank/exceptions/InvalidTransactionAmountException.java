package com.bank.exceptions;

public class InvalidTransactionAmountException extends RuntimeException {
    public InvalidTransactionAmountException(double amount) {
        super("Amount must be greater than zero and finite: " + amount);
    }
}
