package dev.bugbounty.lab.service;

import org.springframework.stereotype.Component;

@Component
public class ReviewGateway {
    public void verify(boolean simulateFailure) {
        if (simulateFailure) {
            throw new LabUnavailableException("Review service unavailable; report was not closed");
        }
    }
}
