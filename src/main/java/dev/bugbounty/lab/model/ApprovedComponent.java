package dev.bugbounty.lab.model;

public record ApprovedComponent(long id, String name, String version, String source, String sha256) {}
