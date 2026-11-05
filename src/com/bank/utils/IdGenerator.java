package com.bank.utils;

import java.util.concurrent.atomic.AtomicInteger;

public final class IdGenerator {
    private static final AtomicInteger CUSTOMER_SEQUENCE = new AtomicInteger(1000);
    private static final AtomicInteger ACCOUNT_SEQUENCE = new AtomicInteger(1000);
    private static final AtomicInteger TRANSACTION_SEQUENCE = new AtomicInteger(1000);

    private IdGenerator() {
    }

    public static String generateCustomerId() {
        return "CUS" + CUSTOMER_SEQUENCE.incrementAndGet();
    }

    public static String generateAccountNumber() {
        return "ACC" + ACCOUNT_SEQUENCE.incrementAndGet();
    }

    public static String generateTransactionId() {
        return "TXN" + TRANSACTION_SEQUENCE.incrementAndGet();
    }
}
