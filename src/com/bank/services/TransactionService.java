package com.bank.services;

import com.bank.enums.AccountStatus;
import com.bank.enums.TransactionType;
import com.bank.exceptions.AccountInactiveException;
import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InvalidTransactionAmountException;
import com.bank.exceptions.SameAccountTransferException;
import com.bank.models.Account;
import com.bank.models.Transaction;
import com.bank.repository.BankRepository;
import com.bank.utils.IdGenerator;
import com.bank.utils.ValidationUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransactionService {
    private static final int MINI_STATEMENT_SIZE = 5;

    private final BankRepository repository;

    public TransactionService(BankRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public synchronized Transaction deposit(String accountNumber, double amount) {
        validateAmount(amount);
        Account account = getAccount(accountNumber);
        ensureActive(account);
        if (amount > Double.MAX_VALUE - account.getBalance()) {
            throw new IllegalArgumentException("Deposit would exceed the maximum supported balance");
        }

        account.deposit(amount);
        Transaction transaction = createTransaction(accountNumber, TransactionType.DEPOSIT, amount, null);
        repository.addTransaction(transaction);
        return transaction;
    }

    public synchronized Transaction withdraw(String accountNumber, double amount) {
        validateAmount(amount);
        Account account = getAccount(accountNumber);
        ensureActive(account);

        account.withdraw(amount);
        Transaction transaction = createTransaction(accountNumber, TransactionType.WITHDRAW, amount, null);
        repository.addTransaction(transaction);
        return transaction;
    }

    public synchronized List<Transaction> transfer(String fromAccount, String toAccount, double amount) {
        validateAmount(amount);
        if (Objects.equals(fromAccount, toAccount)) {
            throw new SameAccountTransferException(fromAccount);
        }

        Account source = getAccount(fromAccount);
        Account destination = getAccount(toAccount);
        Account firstLock = fromAccount.compareTo(toAccount) < 0 ? source : destination;
        Account secondLock = firstLock == source ? destination : source;

        synchronized (firstLock) {
            synchronized (secondLock) {
                ensureActive(source);
                ensureActive(destination);
                if (amount > Double.MAX_VALUE - destination.getBalance()) {
                    throw new IllegalArgumentException("Transfer would exceed the destination's maximum balance");
                }

                source.withdraw(amount);
                destination.deposit(amount);

                String referenceId = IdGenerator.generateTransactionId();
                LocalDateTime timestamp = LocalDateTime.now();
                Transaction debit = createTransaction(
                        fromAccount, TransactionType.TRANSFER, -amount, referenceId, timestamp);
                Transaction credit = createTransaction(
                        toAccount, TransactionType.TRANSFER, amount, referenceId, timestamp);
                repository.addTransaction(debit);
                repository.addTransaction(credit);
                List<Transaction> transactions = new ArrayList<>();
                transactions.add(debit);
                transactions.add(credit);
                return transactions;
            }
        }
    }

    public List<Transaction> getTransactionHistory() {
        return repository.getAllTransactions();
    }

    public List<Transaction> getTransactionHistory(String accountNumber) {
        getAccount(accountNumber);
        List<Transaction> history = new ArrayList<>();
        for (Transaction transaction : repository.getAllTransactions()) {
            if (transaction.getAccountNumber().equals(accountNumber)) {
                history.add(transaction);
            }
        }
        return history;
    }

    public List<Transaction> getMiniStatement() {
        List<Transaction> history = repository.getAllTransactions();
        return lastTransactions(history);
    }

    public List<Transaction> getMiniStatement(String accountNumber) {
        return lastTransactions(getTransactionHistory(accountNumber));
    }

    private List<Transaction> lastTransactions(List<Transaction> history) {
        int firstIndex = Math.max(0, history.size() - MINI_STATEMENT_SIZE);
        return new ArrayList<>(history.subList(firstIndex, history.size()));
    }

    private Account getAccount(String accountNumber) {
        Account account = repository.getAccount(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    private void ensureActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(account.getAccountNumber());
        }
    }

    private void validateAmount(double amount) {
        if (!ValidationUtil.isValidAmount(amount)) {
            throw new InvalidTransactionAmountException(amount);
        }
    }

    private Transaction createTransaction(String accountNumber, TransactionType type, double amount,
                                          String referenceId) {
        return createTransaction(accountNumber, type, amount, referenceId, LocalDateTime.now());
    }

    private Transaction createTransaction(String accountNumber, TransactionType type, double amount,
                                          String referenceId, LocalDateTime timestamp) {
        return new Transaction(
                IdGenerator.generateTransactionId(), accountNumber, type, amount, timestamp, referenceId);
    }
}
