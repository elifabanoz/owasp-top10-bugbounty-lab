package dev.bugbounty.lab.service;

public class LabUnavailableException extends RuntimeException {
    public LabUnavailableException(String message) {
        super(message);
    }
}
