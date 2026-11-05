package com.bank.console;

import com.bank.exceptions.DuplicateCustomerException;
import com.bank.models.Customer;
import com.bank.services.AccountService;
import com.bank.services.AdminService;
import com.bank.services.CustomerService;
import com.bank.services.TransactionService;

import java.util.Scanner;

public class MainMenu {
    private final Scanner scanner;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AdminService adminService;

    public MainMenu(Scanner scanner, CustomerService customerService, AccountService accountService,
                    TransactionService transactionService, AdminService adminService) {
        this.scanner = scanner;
        this.customerService = customerService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.adminService = adminService;
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println("\n===== BANKING SYSTEM =====");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Admin Login");
            System.out.println("4. Exit");
            System.out.print("Select an option: ");

            switch (readChoice()) {
                case 1:
                    register();
                    break;
                case 2:
                    login();
                    break;
                case 3:
                    adminLogin();
                    break;
                case 4:
                    running = false;
                    break;
                default:
                    System.out.println("Please select a valid option.");
            }
        }
        System.out.println("Goodbye.");
    }

    private void register() {
        String username = readLine("Username: ");
        String password = readLine("Password: ");
        try {
            Customer customer = customerService.registerCustomer(username, password);
            System.out.println("Customer registered. Customer ID: " + customer.getCustomerId());
        } catch (DuplicateCustomerException | IllegalArgumentException exception) {
            System.out.println("Registration failed: " + exception.getMessage());
        }
    }

    private void login() {
        String username = readLine("Username: ");
        String password = readLine("Password: ");
        try {
            Customer customer = customerService.login(username, password);
            new CustomerMenu(scanner, customer, accountService, transactionService).run();
        } catch (IllegalArgumentException exception) {
            System.out.println("Login failed: " + exception.getMessage());
        }
    }

    private void adminLogin() {
        String username = readLine("Admin username: ");
        String password = readLine("Admin password: ");
        if (!adminService.login(username, password)) {
            System.out.println("Invalid admin username or password.");
            return;
        }
        new AdminMenu(scanner, adminService).run();
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
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
