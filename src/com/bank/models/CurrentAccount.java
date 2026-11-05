package com.bank.models;

import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;

public class CurrentAccount extends Account {
    public CurrentAccount(String accountNumber, double initialBalance) {
        super(accountNumber, initialBalance, AccountType.CURRENT, AccountStatus.ACTIVE);
    }
}
