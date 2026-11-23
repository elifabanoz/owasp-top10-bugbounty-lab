package dev.bugbounty.lab.service;

public class LabConflictException extends RuntimeException {
    public LabConflictException(String message) {
        super(message);
    }
}
