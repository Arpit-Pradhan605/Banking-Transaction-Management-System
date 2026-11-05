package com.bank;

import com.bank.console.MainMenu;
import com.bank.repository.BankRepository;
import com.bank.services.AccountService;
import com.bank.services.AdminService;
import com.bank.services.CustomerService;
import com.bank.services.TransactionService;

import java.util.Scanner;

public class Main {
    private Main() {
    }

    public static void main(String[] args) {
        BankRepository repository = new BankRepository();
        CustomerService customerService = new CustomerService(repository);
        AccountService accountService = new AccountService(repository);
        TransactionService transactionService = new TransactionService(repository);
        AdminService adminService = new AdminService(
                repository,
                environmentOrDefault("BANK_ADMIN_USERNAME", "admin"),
                environmentOrDefault("BANK_ADMIN_PASSWORD", "admin123"));

        try (Scanner scanner = new Scanner(System.in)) {
            new MainMenu(scanner, customerService, accountService, transactionService, adminService).run();
        }
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}