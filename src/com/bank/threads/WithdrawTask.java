package com.bank.threads;

import com.bank.models.Transaction;
import com.bank.services.TransactionService;

import java.util.Objects;
import java.util.concurrent.Callable;

public class WithdrawTask implements Callable<Transaction> {
    private final TransactionService transactionService;
    private final String accountNumber;
    private final double amount;

    public WithdrawTask(TransactionService transactionService, String accountNumber, double amount) {
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.amount = amount;
    }

    @Override
    public Transaction call() {
        return transactionService.withdraw(accountNumber, amount);
    }
}
