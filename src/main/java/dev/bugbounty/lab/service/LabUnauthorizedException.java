package dev.bugbounty.lab.service;

public class LabUnauthorizedException extends RuntimeException {
    public LabUnauthorizedException(String message) {
        super(message);
    }
}
