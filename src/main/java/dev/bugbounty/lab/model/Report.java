package dev.bugbounty.lab.model;

import java.time.OffsetDateTime;

public record Report(long id, long programId, long authorId, String title, String description,
                     String severity, String status, OffsetDateTime createdAt) {}
