package dev.bugbounty.lab.model;

public record User(long id, String username, String email, String passwordHash, String role) {}
