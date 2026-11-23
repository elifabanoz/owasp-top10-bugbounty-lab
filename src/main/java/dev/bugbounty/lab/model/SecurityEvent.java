package dev.bugbounty.lab.model;

public record SecurityEvent(long id, String kind, long actorId, long targetId) {}
