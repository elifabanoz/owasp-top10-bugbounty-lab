package dev.bugbounty.lab.model;

public record IntegrationEvent(long id, long reportId, String decision) {}
