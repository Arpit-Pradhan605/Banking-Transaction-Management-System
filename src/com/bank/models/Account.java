package com.bank.models;

import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.exceptions.AccountInactiveException;
import com.bank.exceptions.InsufficientBalanceException;
import com.bank.exceptions.InvalidTransactionAmountException;
import com.bank.utils.ValidationUtil;

import java.util.Objects;

public abstract class Account {
    private final String accountNumber;
    private double balance;
    private final AccountType accountType;
    private AccountStatus status;

    protected Account(String accountNumber, double initialBalance, AccountType accountType,
                      AccountStatus status) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("accountNumber cannot be blank");
        }
        if (!Double.isFinite(initialBalance) || initialBalance < 0) {
            throw new IllegalArgumentException("initialBalance cannot be negative or non-finite");
        }
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.accountType = Objects.requireNonNull(accountType, "accountType");
        this.status = Objects.requireNonNull(status, "status");
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public synchronized AccountStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(AccountStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public synchronized void deposit(double amount) {
        validateAmount(amount);
        ensureActive();
        balance += amount;
    }

    public synchronized void withdraw(double amount) {
        validateAmount(amount);
        ensureActive();
        if (amount > balance) {
            throw new InsufficientBalanceException("Insufficient balance in account '" + accountNumber + "'");
        }
        balance -= amount;
    }

    protected final void validateAmount(double amount) {
        if (!ValidationUtil.isValidAmount(amount)) {
            throw new InvalidTransactionAmountException(amount);
        }
    }

    protected final synchronized void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(accountNumber);
        }
    }
}
