package com.bank.threads;

import com.bank.models.Transaction;
import com.bank.services.TransactionService;

import java.util.Objects;
import java.util.concurrent.Callable;

public class DepositTask implements Callable<Transaction> {
    private final TransactionService transactionService;
    private final String accountNumber;
    private final double amount;

    public DepositTask(TransactionService transactionService, String accountNumber, double amount) {
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.amount = amount;
    }

    @Override
    public Transaction call() {
        return transactionService.deposit(accountNumber, amount);
    }
}
