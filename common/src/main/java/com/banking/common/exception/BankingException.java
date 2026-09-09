package com.banking.common.exception;

public class BankingException extends RuntimeException {

    private final int status;

    public BankingException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
