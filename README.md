# Banking Transaction & Account Management

## Run

From the repository root, compile and launch the console application:

```powershell
javac -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })
java -cp out com.bank.Main
```

The application stores data in memory for the duration of a run. The demo admin
login is `admin` / `admin123`; set `BANK_ADMIN_USERNAME` and
`BANK_ADMIN_PASSWORD` to override those credentials.

Savings accounts require an initial balance of at least ₹500. Customer
transaction history is limited to accounts owned by the logged-in customer.

## Concurrency checks

Compile the sources using the command above, then run the standalone concurrency
checks:

```powershell
java -cp out com.bank.services.ConcurrencyTestService
```

By default, the service runs 1,000 deposits, 1,000 withdrawals, and 1,000
transfers in each direction between the same two accounts, using a worker pool.
Each scenario uses a fresh in-memory repository and verifies final account
balances and transaction-record counts. Transfer checks also verify that every
transfer returns a matching debit/credit pair. The service prints expected and
actual values for each scenario and exits with a non-zero status if a check
fails.

The operation count per direction and worker count can be supplied as arguments:
both values must be at least 2.

```powershell
java -cp out com.bank.services.ConcurrencyTestService 500 8
```

The checks exercise the public `TransactionService` APIs (`deposit`, `withdraw`,
and `transfer`) concurrently, so the same entry points can be used by runnable
task classes.

## Phase 1: Customer and Account Foundation

Phase 1 provides the in-memory customer and account foundation used by the
console application and later transaction/admin features. Java sources are
under `src/com/bank/`.

### Phase 1 scope

| Area | Package | Responsibility |
| --- | --- | --- |
| Domain models | `com.bank.models` | Customers, accounts, account subtypes, and the transaction data entity |
| Enumerations | `com.bank.enums` | Account type/status and transaction type/status |
| Exceptions | `com.bank.exceptions` | Domain errors used by customer, account, and transaction operations |
| In-memory storage | `com.bank.repository` | Customer, account, and transaction collections |
| Customer/account APIs | `com.bank.services` | Registration, login, account creation, and lookups |
| Shared helpers | `com.bank.utils` | ID generation, input validation, and account constants |

Phase 1 includes `Transaction` as a data model and transaction-related enum and
exception types. Transaction processing itself belongs to the transaction
service and is outside the customer/account foundation.

### Customer API

Create one repository and share it with the services so they operate on the
same in-memory data:

```java
import java.util.List;

import com.bank.models.Customer;
import com.bank.repository.BankRepository;
import com.bank.services.AccountService;
import com.bank.services.CustomerService;

BankRepository repository = new BankRepository();
CustomerService customerService = new CustomerService(repository);
AccountService accountService = new AccountService(repository);

Customer customer = customerService.registerCustomer("mohan", "secret1");
Customer loggedIn = customerService.login("mohan", "secret1");
Customer found = customerService.getCustomerById(customer.getCustomerId());
Customer byUsername = customerService.getCustomerByUsername("mohan");
List<Customer> customers = customerService.getAllCustomers();
```

`registerCustomer(username, password)` trims the username, requires at least
four characters in the trimmed username and at least six password characters,
and rejects duplicate usernames case-insensitively. It returns the registered
`Customer` and generates IDs in the `CUS1001`, `CUS1002`, ... format.

`login(username, password)` looks up usernames case-insensitively and checks
the supplied password. Invalid or missing credentials result in
`IllegalArgumentException`; successful login returns the `Customer`.

`getCustomerById` and `getCustomerByUsername` throw `IllegalArgumentException`
when no matching customer exists. `getAllCustomers` returns a new list
containing the repository's customers.

### Account API

```java
SavingsAccount savings =
        accountService.createSavingsAccount(customer, 1000.0);
CurrentAccount current =
        accountService.createCurrentAccount(customer, 1000.0);

Account account = accountService.getAccountByNumber(savings.getAccountNumber());
List<Account> customerAccounts = accountService.getAccountsByCustomer(customer);
List<Account> allAccounts = accountService.getAllAccounts();
```

Both creation methods require the exact customer instance to already be
registered in the shared repository and require a positive, finite initial
balance. They generate `ACC1001`, `ACC1002`, ... account numbers, add the
account to the repository, and associate it with the customer's account list.

Savings accounts additionally require an initial balance of at least 500.00.
Withdrawals must leave at least 500.00 in a savings account. Current accounts
have no minimum balance beyond the positive initial balance required by the
service. New accounts are `ACTIVE`; deposits and withdrawals are rejected for
non-active accounts.

`getAccountByNumber` throws `AccountNotFoundException` if the account does not
exist. Customer account lists and repository list methods return list copies;
the account/customer objects themselves are shared in-memory objects.

### Models and enums

- `Customer` stores its generated ID, username, password, and associated
  accounts. Account-list access is synchronized and `getAccounts()` returns a
  copy.
- `Account` is the abstract base class. It provides synchronized balance
  access, deposit and withdrawal operations, and a mutable `AccountStatus`.
- `SavingsAccount` applies the 500.00 minimum-balance rules.
- `CurrentAccount` uses the base account behavior without a minimum balance.
- `Transaction` is an entity only; it stores transaction ID, account number,
  type, amount, timestamp, and optional reference ID. It does not implement
  transaction processing.
- `AccountType`: `SAVINGS`, `CURRENT`.
- `AccountStatus`: `ACTIVE`, `FROZEN`, `CLOSED`.
- `TransactionType`: `DEPOSIT`, `WITHDRAW`, `TRANSFER`.
- `TransactionStatus`: `SUCCESS`, `FAILED`.

### Validation, exceptions, and IDs

`ValidationUtil` provides the shared checks:

| Method | Rule |
| --- | --- |
| `isValidUsername(username)` | Non-null; at least 4 characters after trimming |
| `isValidPassword(password)` | Non-null; at least 6 characters |
| `isValidAmount(amount)` | Finite and greater than zero |

Invalid customer/account-service inputs throw `IllegalArgumentException` or
`InvalidTransactionAmountException`, as applicable. Account operations throw
`InvalidTransactionAmountException` for invalid amounts,
`AccountInactiveException` for non-active accounts, and
`InsufficientBalanceException` when a withdrawal cannot be completed.
Duplicate usernames result in `DuplicateCustomerException`.
`TransactionLimitExceededException` and `SameAccountTransferException` are
available for transaction-layer use.

`IdGenerator` uses separate `AtomicInteger` sequences for customer, account,
and transaction IDs, formatted as `CUS####`, `ACC####`, and `TXN####`. These
sequences are thread-safe within one JVM, but are in-memory only: they restart
when the application restarts and do not guarantee uniqueness across separate
application processes.

### Repository behavior and limitations

`BankRepository` is the in-memory store. It supports adding, retrieving, and
removing customers/accounts; listing customers/accounts; looking up a customer
by username; and adding/listing transactions. Repository methods are
synchronized. Data is lost when the process exits, and this repository is not
a persistent database.

Passwords are currently stored and compared as plain text to match the
foundation model. This is suitable only for a learning/demo application; a
production system must store salted password hashes and use an appropriate
authentication design.

### Phase 1 completion checklist

- [x] Customer registration, unique usernames, and generated customer IDs
- [x] Customer login and customer lookups
- [x] Savings/current account creation and generated account numbers
- [x] Account repository storage/search and customer-account association
- [x] Account balance operations, account status checks, and savings minimum
- [x] Transaction entity, enums, repository storage, and shared utilities
- [x] Java source compilation

No persistent database or committed automated Phase 1 test suite is included.
The application can be compiled using the command in [Run](#run).

## Phase 2: Concurrent Transaction Tasks (in progress)

The `com.bank.threads` package provides `Callable` task wrappers around
`TransactionService`. Each task performs one operation and returns its
transaction result; exceptions are not swallowed and are surfaced by
`Future.get()` as an `ExecutionException`.

```java
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.bank.models.Transaction;
import com.bank.services.TransactionService;
import com.bank.threads.DepositTask;
import com.bank.threads.TransferTask;
import com.bank.threads.WithdrawTask;

ExecutorService executor = Executors.newFixedThreadPool(4);
try {
    Future<Transaction> deposit = executor.submit(
            new DepositTask(transactionService, accountNumber, 100.0));
    Future<Transaction> withdrawal = executor.submit(
            new WithdrawTask(transactionService, accountNumber, 25.0));
    Future<List<Transaction>> transfer = executor.submit(
            new TransferTask(transactionService, fromAccount, toAccount, 50.0));

    Transaction depositResult = deposit.get();
    Transaction withdrawalResult = withdrawal.get();
    List<Transaction> transferResults = transfer.get();
} finally {
    executor.shutdown();
}
```

`DepositTask` and `WithdrawTask` return one `Transaction`.
`TransferTask` returns the debit and credit transactions created for the
transfer. All tasks use the existing `TransactionService` for validation,
account updates, and transaction recording. The service currently synchronizes
its transaction operations, so concurrent task submissions preserve the
service's serialized operation behavior rather than running those operations
in parallel.

Phase 2 ownership split:

- **Mohan (70%)**: `DepositTask`, `WithdrawTask`, and `TransferTask`.
- **Arpit (30%)**: `ConcurrencyTestService`, including repeatable concurrent
  scenarios and assertions for final balances and transaction records.

The concurrency test service and a committed automated Phase 2 test suite have
not yet been added.
