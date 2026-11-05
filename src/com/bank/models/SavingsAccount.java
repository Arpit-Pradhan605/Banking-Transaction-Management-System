package com.bank.models;

import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.exceptions.InsufficientBalanceException;
import com.bank.utils.Constants;

public class SavingsAccount extends Account {
    public SavingsAccount(String accountNumber, double initialBalance) {
        super(accountNumber, validateInitialBalance(initialBalance), AccountType.SAVINGS, AccountStatus.ACTIVE);
    }

    private static double validateInitialBalance(double initialBalance) {
        if (!Double.isFinite(initialBalance) || initialBalance < Constants.SAVINGS_MINIMUM_BALANCE) {
            throw new IllegalArgumentException(
                    "Savings accounts require an initial balance of at least "
                            + Constants.SAVINGS_MINIMUM_BALANCE);
        }
        return initialBalance;
    }

    @Override
    public synchronized void withdraw(double amount) {
        validateAmount(amount);
        ensureActive();
        if (getBalance() - amount < Constants.SAVINGS_MINIMUM_BALANCE) {
            throw new InsufficientBalanceException(
                    "Withdrawal would reduce the savings balance below "
                            + Constants.SAVINGS_MINIMUM_BALANCE);
        }
        super.withdraw(amount);
    }
}
