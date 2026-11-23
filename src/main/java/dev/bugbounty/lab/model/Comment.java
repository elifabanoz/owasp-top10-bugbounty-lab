package dev.bugbounty.lab.model;

import java.time.OffsetDateTime;

public record Comment(long id, long reportId, long authorId, String body, OffsetDateTime createdAt) {}
