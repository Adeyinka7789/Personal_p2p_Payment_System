package com.example.ppps.exception;

public class WalletNotFoundException extends TransferException {
    public WalletNotFoundException(String message) {
        super(message);
    }
}