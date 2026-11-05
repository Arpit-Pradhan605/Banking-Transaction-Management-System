package com.bank.models;

import com.bank.enums.TransactionType;

import java.time.LocalDateTime;
import java.util.Objects;

public class Transaction {
    private final String transactionId;
    private final String accountNumber;
    private final TransactionType type;
    private final double amount;
    private final LocalDateTime timestamp;
    private final String referenceId;

    public Transaction(String transactionId, String accountNumber, TransactionType type, double amount,
                       LocalDateTime timestamp, String referenceId) {
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.type = Objects.requireNonNull(type, "type");
        this.amount = amount;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.referenceId = referenceId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
