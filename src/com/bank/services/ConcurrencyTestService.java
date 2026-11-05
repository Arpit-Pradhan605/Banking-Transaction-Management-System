package com.bank.services;

import com.bank.enums.TransactionType;
import com.bank.models.Account;
import com.bank.models.CurrentAccount;
import com.bank.models.Transaction;
import com.bank.repository.BankRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ConcurrencyTestService {
    private static final int DEFAULT_OPERATIONS = 1000;
    private static final int DEFAULT_THREADS =
            Math.min(8, Math.max(2, Runtime.getRuntime().availableProcessors()));
    private static final long EXECUTION_TIMEOUT_SECONDS = 30;
    private static final double OPERATION_AMOUNT = 1.0;
    private static final double INITIAL_BALANCE = 1000.0;

    public List<CheckResult> runChecks(int operationsPerDirection, int threadCount) {
        if (operationsPerDirection < 2) {
            throw new IllegalArgumentException("operationsPerDirection must be at least 2");
        }
        if (threadCount < 2) {
            throw new IllegalArgumentException("threadCount must be at least 2");
        }
        if (operationsPerDirection > Integer.MAX_VALUE / 4) {
            throw new IllegalArgumentException("operationsPerDirection is too large");
        }

        List<CheckResult> results = new ArrayList<>();
        results.add(checkDeposits(operationsPerDirection, threadCount));
        results.add(checkWithdrawals(operationsPerDirection, threadCount));
        results.add(checkTransfers(operationsPerDirection, threadCount));
        return Collections.unmodifiableList(results);
    }

    private CheckResult checkDeposits(int operations, int threadCount) {
        String accountNumber = "TEST-DEPOSIT";
        BankRepository repository = new BankRepository();
        Account account = new CurrentAccount(accountNumber, INITIAL_BALANCE);
        repository.addAccount(account);
        TransactionService transactionService = new TransactionService(repository);

        List<Callable<Transaction>> tasks = new ArrayList<>(operations);
        for (int i = 0; i < operations; i++) {
            tasks.add(() -> transactionService.deposit(accountNumber, OPERATION_AMOUNT));
        }

        String failure = null;
        try {
            executeConcurrently(tasks, threadCount);
        } catch (IllegalStateException exception) {
            failure = exception.getMessage();
        }

        Map<String, Double> expected = balances(accountNumber, INITIAL_BALANCE + operations);
        return result("Concurrent deposits", expected, repository, operations, failure);
    }

    private CheckResult checkWithdrawals(int operations, int threadCount) {
        String accountNumber = "TEST-WITHDRAW";
        double initialBalance = INITIAL_BALANCE + operations;
        BankRepository repository = new BankRepository();
        Account account = new CurrentAccount(accountNumber, initialBalance);
        repository.addAccount(account);
        TransactionService transactionService = new TransactionService(repository);

        List<Callable<Transaction>> tasks = new ArrayList<>(operations);
        for (int i = 0; i < operations; i++) {
            tasks.add(() -> transactionService.withdraw(accountNumber, OPERATION_AMOUNT));
        }

        String failure = null;
        try {
            executeConcurrently(tasks, threadCount);
        } catch (IllegalStateException exception) {
            failure = exception.getMessage();
        }

        Map<String, Double> expected = balances(accountNumber, INITIAL_BALANCE);
        return result("Concurrent withdrawals", expected, repository, operations, failure);
    }

    private CheckResult checkTransfers(int operationsPerDirection, int threadCount) {
        String firstAccountNumber = "TEST-TRANSFER-A";
        String secondAccountNumber = "TEST-TRANSFER-B";
        double initialBalance = INITIAL_BALANCE + operationsPerDirection;
        BankRepository repository = new BankRepository();
        repository.addAccount(new CurrentAccount(firstAccountNumber, initialBalance));
        repository.addAccount(new CurrentAccount(secondAccountNumber, initialBalance));
        TransactionService transactionService = new TransactionService(repository);

        List<Callable<List<Transaction>>> tasks = new ArrayList<>(operationsPerDirection * 2);
        for (int i = 0; i < operationsPerDirection; i++) {
            tasks.add(() -> transactionService.transfer(
                    firstAccountNumber, secondAccountNumber, OPERATION_AMOUNT));
            tasks.add(() -> transactionService.transfer(
                    secondAccountNumber, firstAccountNumber, OPERATION_AMOUNT));
        }

        String failure = null;
        try {
            List<List<Transaction>> transfers = executeConcurrently(tasks, threadCount);
            failure = validateTransferPairs(transfers, firstAccountNumber, secondAccountNumber);
        } catch (IllegalStateException exception) {
            failure = exception.getMessage();
        }

        Map<String, Double> expected = balances(
                firstAccountNumber, initialBalance,
                secondAccountNumber, initialBalance);
        return result("Concurrent bidirectional transfers", expected, repository,
                operationsPerDirection * 4, failure);
    }

    private <T> List<T> executeConcurrently(List<? extends Callable<T>> tasks, int threadCount) {
        int workerCount = Math.min(threadCount, tasks.size());
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch ready = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>(tasks.size());

        try {
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new TimeoutException("Timed out waiting to start concurrent operations");
                    }
                    return task.call();
                }));
            }

            if (!ready.await(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new TimeoutException("Timed out waiting for concurrent workers");
            }
            start.countDown();

            List<T> results = new ArrayList<>(tasks.size());
            for (Future<T> future : futures) {
                results.add(future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return results;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent checks were interrupted", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("A concurrent transaction failed", exception.getCause());
        } catch (TimeoutException exception) {
            for (Future<T> future : futures) {
                future.cancel(true);
            }
            throw new IllegalStateException("Concurrent checks timed out", exception);
        } finally {
            start.countDown();
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent workers did not terminate");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String validateTransferPairs(List<List<Transaction>> transfers,
                                         String firstAccountNumber, String secondAccountNumber) {
        for (List<Transaction> transfer : transfers) {
            if (transfer.size() != 2) {
                return "A transfer returned " + transfer.size() + " records instead of 2";
            }
            Transaction first = transfer.get(0);
            Transaction second = transfer.get(1);
            if (first.getType() != TransactionType.TRANSFER
                    || second.getType() != TransactionType.TRANSFER
                    || first.getReferenceId() == null
                    || !first.getReferenceId().equals(second.getReferenceId())
                    || first.getAmount() != -OPERATION_AMOUNT
                    || second.getAmount() != OPERATION_AMOUNT
                    || first.getTimestamp() == null
                    || !first.getTimestamp().equals(second.getTimestamp())
                    || !isAccountPair(first.getAccountNumber(), second.getAccountNumber(),
                    firstAccountNumber, secondAccountNumber)) {
                return "A transfer did not produce a matching debit/credit pair";
            }
        }
        return null;
    }

    private boolean isAccountPair(String first, String second, String accountA, String accountB) {
        return (first.equals(accountA) && second.equals(accountB))
                || (first.equals(accountB) && second.equals(accountA));
    }

    private CheckResult result(String name, Map<String, Double> expectedBalances,
                               BankRepository repository, int expectedTransactionCount, String failure) {
        Map<String, Double> actualBalances = new LinkedHashMap<>();
        for (String accountNumber : expectedBalances.keySet()) {
            Account account = repository.getAccount(accountNumber);
            actualBalances.put(accountNumber, account.getBalance());
        }
        int actualTransactionCount = repository.getAllTransactions().size();

        StringBuilder details = new StringBuilder(failure == null ? "" : failure);
        if (!expectedBalances.equals(actualBalances)) {
            appendFailure(details, "balance mismatch");
        }
        if (expectedTransactionCount != actualTransactionCount) {
            appendFailure(details, "transaction count mismatch");
        }

        return new CheckResult(name, expectedBalances, actualBalances,
                expectedTransactionCount, actualTransactionCount, details.toString());
    }

    private Map<String, Double> balances(String accountNumber, double balance) {
        Map<String, Double> result = new LinkedHashMap<>();
        result.put(accountNumber, balance);
        return result;
    }

    private Map<String, Double> balances(String firstAccountNumber, double firstBalance,
                                         String secondAccountNumber, double secondBalance) {
        Map<String, Double> result = new LinkedHashMap<>();
        result.put(firstAccountNumber, firstBalance);
        result.put(secondAccountNumber, secondBalance);
        return result;
    }

    private void appendFailure(StringBuilder details, String message) {
        if (details.length() > 0) {
            details.append("; ");
        }
        details.append(message);
    }

    public static final class CheckResult {
        private final String name;
        private final Map<String, Double> expectedBalances;
        private final Map<String, Double> actualBalances;
        private final int expectedTransactionCount;
        private final int actualTransactionCount;
        private final String failure;

        private CheckResult(String name, Map<String, Double> expectedBalances,
                            Map<String, Double> actualBalances, int expectedTransactionCount,
                            int actualTransactionCount, String failure) {
            this.name = name;
            this.expectedBalances = Collections.unmodifiableMap(new LinkedHashMap<>(expectedBalances));
            this.actualBalances = Collections.unmodifiableMap(new LinkedHashMap<>(actualBalances));
            this.expectedTransactionCount = expectedTransactionCount;
            this.actualTransactionCount = actualTransactionCount;
            this.failure = failure;
        }

        public String getName() {
            return name;
        }

        public Map<String, Double> getExpectedBalances() {
            return expectedBalances;
        }

        public Map<String, Double> getActualBalances() {
            return actualBalances;
        }

        public int getExpectedTransactionCount() {
            return expectedTransactionCount;
        }

        public int getActualTransactionCount() {
            return actualTransactionCount;
        }

        public boolean isSuccessful() {
            return failure.isEmpty();
        }

        public String getFailure() {
            return failure;
        }

        @Override
        public String toString() {
            return name + ": " + (isSuccessful() ? "PASS" : "FAIL")
                    + "; balances expected=" + expectedBalances + ", actual=" + actualBalances
                    + "; transactions expected=" + expectedTransactionCount
                    + ", actual=" + actualTransactionCount
                    + (isSuccessful() ? "" : "; failure=" + failure);
        }
    }

    public static void main(String[] args) {
        if (args.length > 2) {
            throw new IllegalArgumentException(
                    "Usage: java com.bank.services.ConcurrencyTestService [operationsPerDirection] [threadCount]");
        }

        int operations = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_OPERATIONS;
        int threads = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_THREADS;
        List<CheckResult> results = new ConcurrencyTestService().runChecks(operations, threads);
        for (CheckResult result : results) {
            System.out.println(result);
        }
        if (results.stream().anyMatch(result -> !result.isSuccessful())) {
            System.exit(1);
        }
    }
}
