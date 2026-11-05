# Banking Transaction & Account Management System

A Java console application for customer banking, account management, deposits,
withdrawals, transfers, administration, and concurrent transaction checks.
The application currently uses an in-memory repository; data is cleared when
the program exits.

## Contents

- [Features](#features)
- [Project structure](#project-structure)
- [Requirements](#requirements)
- [Build and run](#build-and-run)
- [Default admin credentials](#default-admin-credentials)
- [Concurrency checks](#concurrency-checks)
- [Core behavior](#core-behavior)
- [Phase 2 ownership](#phase-2-ownership)
- [Limitations](#limitations)

## Features

- Customer registration, login, and account listing
- Savings and current account creation
- Deposits, withdrawals, transfers, transaction history, and mini statements
- Admin views for customers and accounts, account freeze/unfreeze, and summary
  statistics
- Concurrent deposit, withdrawal, and bidirectional transfer checks

## Project structure

```text
Banking-Transaction-Management-System/
├── README.md
├── .gitignore
├── .vscode/
│   └── settings.json
└── src/
    └── com/
        └── bank/
            ├── Main.java
            ├── console/
            │   ├── AdminMenu.java
            │   ├── CustomerMenu.java
            │   └── MainMenu.java
            ├── enums/
            │   ├── AccountStatus.java
            │   ├── AccountType.java
            │   ├── TransactionStatus.java
            │   └── TransactionType.java
            ├── exceptions/
            │   ├── AccountInactiveException.java
            │   ├── AccountNotFoundException.java
            │   ├── DuplicateCustomerException.java
            │   ├── InsufficientBalanceException.java
            │   ├── InvalidTransactionAmountException.java
            │   ├── SameAccountTransferException.java
            │   └── TransactionLimitExceededException.java
            ├── models/
            │   ├── Account.java
            │   ├── CurrentAccount.java
            │   ├── Customer.java
            │   ├── SavingsAccount.java
            │   └── Transaction.java
            ├── repository/
            │   └── BankRepository.java
            ├── services/
            │   ├── AccountService.java
            │   ├── AdminService.java
            │   ├── ConcurrencyTestService.java
            │   ├── CustomerService.java
            │   └── TransactionService.java
            ├── threads/
            │   ├── DepositTask.java
            │   ├── TransferTask.java
            │   └── WithdrawTask.java
            └── utils/
                ├── Constants.java
                ├── IdGenerator.java
                └── ValidationUtil.java
```

### Package responsibilities

| Package | Responsibility |
| --- | --- |
| `com.bank.console` | Interactive customer, admin, and main menus |
| `com.bank.enums` | Account and transaction types/statuses |
| `com.bank.exceptions` | Domain-specific operation failures |
| `com.bank.models` | Customer, account, and transaction entities |
| `com.bank.repository` | In-memory storage for customers, accounts, and transactions |
| `com.bank.services` | Customer, account, transaction, admin, and concurrency-check operations |
| `com.bank.threads` | `Callable` task wrappers for transactions |
| `com.bank.utils` | ID generation, validation, and shared constants |

## Requirements

- JDK 8 or later
- PowerShell for the commands below (or adapt the compile command to your
  shell)

No external libraries or build tool are required.

## Build and run

Run these commands from the repository root in PowerShell:

```powershell
$sources = Get-ChildItem .\src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d .\out $sources
java -cp .\out com.bank.Main
```

The console menu lets customers register or log in and lets administrators
log in. Customers can create accounts, view their accounts, make transactions,
and view transaction records. Administrators can view customers/accounts,
freeze or unfreeze accounts, and view statistics.

## Default admin credentials

The default demo admin login is `admin` / `admin123`. Override these values
using the `BANK_ADMIN_USERNAME` and `BANK_ADMIN_PASSWORD` environment variables
before starting the application.

## Concurrency checks

After compiling, run the standalone concurrency scenarios:

```powershell
java -cp .\out com.bank.services.ConcurrencyTestService
```

By default, the service runs 1,000 deposits, 1,000 withdrawals, and 1,000
transfers in each direction between two accounts. Each scenario creates a
fresh in-memory repository and verifies expected balances and transaction
counts. Transfer checks also verify that each operation returns a matching
debit/credit pair. A failed check is reported and causes a non-zero process
exit code.

To set the operation count per direction and worker count:

```powershell
java -cp .\out com.bank.services.ConcurrencyTestService 500 8
```

Both arguments must be at least `2`.

The `DepositTask`, `WithdrawTask`, and `TransferTask` classes implement
`Callable` and delegate to `TransactionService`. Use an `ExecutorService` to
submit them; retrieve the result with `Future.get()`. Operation failures are
propagated through the future rather than converted to successful results.

## Core behavior

### Customers and identifiers

- Usernames must contain at least four characters after trimming and are
  unique case-insensitively.
- Passwords must contain at least six characters.
- Customer login compares usernames case-insensitively and passwords exactly.
- Customer IDs, account numbers, and transaction IDs use separate
  thread-safe-in-process counters: `CUS1001`, `ACC1001`, and `TXN1001`, etc.

### Accounts and transactions

- Savings accounts require an initial balance of at least `500.00`; a
  withdrawal must leave at least that minimum balance.
- Current accounts have no minimum-balance rule, but withdrawals cannot exceed
  the available balance.
- Deposits and withdrawals require finite amounts greater than zero.
- New accounts are `ACTIVE`. Transactions on frozen or closed accounts are
  rejected.
- Transfers create paired debit and credit transaction records that share a
  reference ID and timestamp.
- The customer menu limits deposits, withdrawals, transfer source accounts,
  and viewed transaction history to the logged-in customer's accounts.

### In-memory storage

`BankRepository` stores customers, accounts, and transactions in memory.
Repository operations are synchronized and list methods return list copies.
The `AtomicInteger` ID counters and repository contents reset when the
application restarts; they are not shared across separate JVM processes.

## Phase 2 ownership

- **Mohan (70%)**: `DepositTask`, `WithdrawTask`, and `TransferTask`.
- **Arpit (30%)**: `ConcurrencyTestService`, including concurrent scenarios and
  balance/transaction-count checks.

The concurrency check service exercises the synchronized public
`TransactionService` methods. Task submission is concurrent, but each
transaction operation is currently serialized by that service.

## Limitations

- There is no persistent database; all customer, account, and transaction data
  is lost at process exit.
- Customer passwords are stored as plain text in this demo implementation.
  Production authentication must use a suitable password-hashing and
  credential-management design.
- `ConcurrencyTestService` is a standalone verification runner. The project
  does not currently include a separate automated unit-test framework/suite.
