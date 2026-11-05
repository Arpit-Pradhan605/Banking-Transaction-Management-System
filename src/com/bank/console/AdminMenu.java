package com.bank.console;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.models.Account;
import com.bank.models.Customer;
import com.bank.services.AdminService;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AdminMenu {
    private final Scanner scanner;
    private final AdminService adminService;

    public AdminMenu(Scanner scanner, AdminService adminService) {
        this.scanner = scanner;
        this.adminService = adminService;
    }

    public void run() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n===== ADMIN MENU =====");
            System.out.println("1. View Customers");
            System.out.println("2. View Accounts");
            System.out.println("3. Freeze Account");
            System.out.println("4. Unfreeze Account");
            System.out.println("5. Statistics");
            System.out.println("6. Logout");
            System.out.print("Select an option: ");

            switch (readChoice()) {
                case 1:
                    viewCustomers();
                    break;
                case 2:
                    viewAccounts();
                    break;
                case 3:
                    changeAccountStatus(true);
                    break;
                case 4:
                    changeAccountStatus(false);
                    break;
                case 5:
                    showStatistics();
                    break;
                case 6:
                    loggedIn = false;
                    break;
                default:
                    System.out.println("Please select a valid option.");
            }
        }
    }

    private void viewCustomers() {
        List<Customer> customers = adminService.viewAllCustomers();
        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }
        for (Customer customer : customers) {
            System.out.println(customer.getCustomerId() + " | " + customer.getUsername());
        }
    }

    private void viewAccounts() {
        List<Account> accounts = adminService.viewAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        for (Account account : accounts) {
            System.out.println(account.getAccountNumber() + " | " + account.getAccountType()
                    + " | " + account.getStatus() + " | balance: " + account.getBalance());
        }
    }

    private void changeAccountStatus(boolean freeze) {
        System.out.print("Account number: ");
        String accountNumber = scanner.nextLine().trim();
        try {
            if (freeze) {
                adminService.freezeAccount(accountNumber);
                System.out.println("Account frozen.");
            } else {
                adminService.unfreezeAccount(accountNumber);
                System.out.println("Account unfrozen.");
            }
        } catch (AccountNotFoundException exception) {
            System.out.println("Unable to update account: " + exception.getMessage());
        }
    }

    private void showStatistics() {
        for (Map.Entry<String, Number> statistic : adminService.getStatistics().entrySet()) {
            System.out.println(formatLabel(statistic.getKey()) + ": " + statistic.getValue());
        }
    }

    private String formatLabel(String key) {
        StringBuilder label = new StringBuilder();
        for (int index = 0; index < key.length(); index++) {
            char character = key.charAt(index);
            if (Character.isUpperCase(character)) {
                label.append(' ');
            }
            label.append(character);
        }
        return label.substring(0, 1).toUpperCase() + label.substring(1);
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
