package com.bank.services;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InvalidTransactionAmountException;
import com.bank.models.Account;
import com.bank.models.CurrentAccount;
import com.bank.models.Customer;
import com.bank.models.SavingsAccount;
import com.bank.repository.BankRepository;
import com.bank.utils.Constants;
import com.bank.utils.IdGenerator;
import com.bank.utils.ValidationUtil;

import java.util.List;
import java.util.Objects;

public class AccountService {
    private final BankRepository repository;

    public AccountService(BankRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public synchronized SavingsAccount createSavingsAccount(Customer customer, double initialBalance) {
        validateCustomer(customer);
        validateInitialBalance(initialBalance);
        if (initialBalance < Constants.SAVINGS_MINIMUM_BALANCE) {
            throw new IllegalArgumentException(
                    "Savings accounts require an initial balance of at least "
                            + Constants.SAVINGS_MINIMUM_BALANCE);
        }
        SavingsAccount account = new SavingsAccount(IdGenerator.generateAccountNumber(), initialBalance);
        repository.addAccount(account);
        customer.addAccount(account);
        return account;
    }

    public synchronized CurrentAccount createCurrentAccount(Customer customer, double initialBalance) {
        validateCustomer(customer);
        validateInitialBalance(initialBalance);
        CurrentAccount account = new CurrentAccount(IdGenerator.generateAccountNumber(), initialBalance);
        repository.addAccount(account);
        customer.addAccount(account);
        return account;
    }

    public Account getAccountByNumber(String accountNumber) {
        Account account = repository.getAccount(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    public List<Account> getAccountsByCustomer(Customer customer) {
        validateCustomer(customer);
        return customer.getAccounts();
    }

    public List<Account> getAllAccounts() {
        return repository.getAllAccounts();
    }

    private void validateCustomer(Customer customer) {
        Objects.requireNonNull(customer, "customer");
        Customer registeredCustomer = repository.getCustomer(customer.getCustomerId());
        if (registeredCustomer == null || registeredCustomer != customer) {
            throw new IllegalArgumentException("Customer must be registered before creating an account");
        }
    }

    private void validateInitialBalance(double initialBalance) {
        if (!ValidationUtil.isValidAmount(initialBalance)) {
            throw new InvalidTransactionAmountException(initialBalance);
        }
    }
}
