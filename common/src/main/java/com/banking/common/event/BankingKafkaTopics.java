package com.banking.common.event;

public final class BankingKafkaTopics {

    public static final String AUTH_EVENTS = "banking.auth.events";
    public static final String TRANSACTION_EVENTS = "banking.transaction.events";
    public static final String AUDIT_EVENTS = "banking.audit.events";

    private BankingKafkaTopics() {
    }
}
