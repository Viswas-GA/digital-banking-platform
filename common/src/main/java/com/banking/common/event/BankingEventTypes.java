package com.banking.common.event;

public final class BankingEventTypes {

    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String KYC_APPROVED = "KYC_APPROVED";
    public static final String KYC_REJECTED = "KYC_REJECTED";
    public static final String TRANSFER_COMPLETED = "TRANSFER_COMPLETED";
    public static final String SCHEDULED_TRANSFER_COMPLETED = "SCHEDULED_TRANSFER_COMPLETED";
    public static final String SCHEDULED_TRANSFER_FAILED = "SCHEDULED_TRANSFER_FAILED";

    private BankingEventTypes() {
    }
}
