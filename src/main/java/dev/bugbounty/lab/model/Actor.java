package dev.bugbounty.lab.model;

public record Actor(long id, String username, String role) {
    public static Actor from(User user) {
        return new Actor(user.id(), user.username(), user.role());
    }
}
