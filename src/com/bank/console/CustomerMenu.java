package com.bank.console;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.AccountInactiveException;
import com.bank.exceptions.InsufficientBalanceException;
import com.bank.exceptions.InvalidTransactionAmountException;
import com.bank.exceptions.SameAccountTransferException;
import com.bank.models.Account;
import com.bank.models.Customer;
import com.bank.models.Transaction;
import com.bank.services.AccountService;
import com.bank.services.TransactionService;

import java.util.List;
import java.util.Scanner;

public class CustomerMenu {
    private final Scanner scanner;
    private final Customer customer;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public CustomerMenu(Scanner scanner, Customer customer, AccountService accountService,
                        TransactionService transactionService) {
        this.scanner = scanner;
        this.customer = customer;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    public void run() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n===== CUSTOMER MENU (" + customer.getUsername() + ") =====");
            System.out.println("1. Create Account");
            System.out.println("2. View Accounts");
            System.out.println("3. Deposit");
            System.out.println("4. Withdraw");
            System.out.println("5. Transfer");
            System.out.println("6. Transaction History");
            System.out.println("7. Mini Statement");
            System.out.println("8. Logout");
            System.out.print("Select an option: ");

            switch (readChoice()) {
                case 1:
                    createAccount();
                    break;
                case 2:
                    viewAccounts();
                    break;
                case 3:
                    deposit();
                    break;
                case 4:
                    withdraw();
                    break;
                case 5:
                    transfer();
                    break;
                case 6:
                    showTransactions(false);
                    break;
                case 7:
                    showTransactions(true);
                    break;
                case 8:
                    loggedIn = false;
                    break;
                default:
                    System.out.println("Please select a valid option.");
            }
        }
    }

    private void createAccount() {
        System.out.println("1. Savings account");
        System.out.println("2. Current account");
        int type = readChoice("Account type: ");
        if (type != 1 && type != 2) {
            System.out.println("Please select a valid account type.");
            return;
        }
        double initialBalance = readAmount("Initial balance: ");
        if (!Double.isFinite(initialBalance)) {
            System.out.println("Please enter a valid finite amount.");
            return;
        }
        try {
            Account account = type == 1
                    ? accountService.createSavingsAccount(customer, initialBalance)
                    : accountService.createCurrentAccount(customer, initialBalance);
            System.out.println("Account created: " + account.getAccountNumber()
                    + " (balance: " + account.getBalance() + ")");
        } catch (IllegalArgumentException exception) {
            System.out.println("Account creation failed: " + exception.getMessage());
        }
    }

    private void viewAccounts() {
        List<Account> accounts = accountService.getAccountsByCustomer(customer);
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        for (Account account : accounts) {
            printAccount(account);
        }
    }

    private void deposit() {
        String accountNumber = readAccountNumber();
        if (!ownsAccount(accountNumber)) {
            return;
        }
        double amount = readAmount("Amount to deposit: ");
        try {
            transactionService.deposit(accountNumber, amount);
            System.out.println("Deposit successful.");
        } catch (AccountInactiveException | AccountNotFoundException | InsufficientBalanceException
                 | InvalidTransactionAmountException | IllegalArgumentException exception) {
            System.out.println("Deposit failed: " + exception.getMessage());
        }
    }

    private void withdraw() {
        String accountNumber = readAccountNumber();
        if (!ownsAccount(accountNumber)) {
            return;
        }
        double amount = readAmount("Amount to withdraw: ");
        try {
            transactionService.withdraw(accountNumber, amount);
            System.out.println("Withdrawal successful.");
        } catch (AccountInactiveException | AccountNotFoundException | InsufficientBalanceException
                 | InvalidTransactionAmountException | IllegalArgumentException exception) {
            System.out.println("Withdrawal failed: " + exception.getMessage());
        }
    }

    private void transfer() {
        String fromAccount = readLine("Source account number: ");
        if (!ownsAccount(fromAccount)) {
            return;
        }
        String toAccount = readLine("Destination account number: ");
        double amount = readAmount("Amount to transfer: ");
        try {
            transactionService.transfer(fromAccount, toAccount, amount);
            System.out.println("Transfer successful.");
        } catch (AccountInactiveException | AccountNotFoundException | InsufficientBalanceException
                 | InvalidTransactionAmountException | SameAccountTransferException
                 | IllegalArgumentException exception) {
            System.out.println("Transfer failed: " + exception.getMessage());
        }
    }

    private void showTransactions(boolean miniStatement) {
        String accountNumber = readAccountNumber();
        if (!ownsAccount(accountNumber)) {
            return;
        }
        List<Transaction> transactions = miniStatement
                ? transactionService.getMiniStatement(accountNumber)
                : transactionService.getTransactionHistory(accountNumber);
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        for (Transaction transaction : transactions) {
            System.out.println(transaction.getTimestamp() + " | " + transaction.getType()
                    + " | " + transaction.getAmount() + " | " + transaction.getTransactionId());
        }
    }

    private boolean ownsAccount(String accountNumber) {
        for (Account account : accountService.getAccountsByCustomer(customer)) {
            if (account.getAccountNumber().equals(accountNumber)) {
                return true;
            }
        }
        System.out.println("That account does not belong to the logged-in customer.");
        return false;
    }

    private String readAccountNumber() {
        return readLine("Account number: ");
    }

    private void printAccount(Account account) {
        System.out.println(account.getAccountNumber() + " | " + account.getAccountType()
                + " | " + account.getStatus() + " | balance: " + account.getBalance());
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private double readAmount(String prompt) {
        String input = readLine(prompt);
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            return Double.NaN;
        }
    }

    private int readChoice(String prompt) {
        System.out.print(prompt);
        return readChoice();
    }

    private int readChoice() {
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
