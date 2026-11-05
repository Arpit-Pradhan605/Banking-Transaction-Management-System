package com.bank.threads;

import com.bank.models.Transaction;
import com.bank.services.TransactionService;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

public class TransferTask implements Callable<List<Transaction>> {
    private final TransactionService transactionService;
    private final String fromAccount;
    private final String toAccount;
    private final double amount;

    public TransferTask(TransactionService transactionService, String fromAccount, String toAccount,
                        double amount) {
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.fromAccount = Objects.requireNonNull(fromAccount, "fromAccount");
        this.toAccount = Objects.requireNonNull(toAccount, "toAccount");
        this.amount = amount;
    }

    @Override
    public List<Transaction> call() {
        return transactionService.transfer(fromAccount, toAccount, amount);
    }
}
