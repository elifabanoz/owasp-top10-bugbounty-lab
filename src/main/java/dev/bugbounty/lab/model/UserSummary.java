package dev.bugbounty.lab.model;

public record UserSummary(long id, String username, String email, String role) {
    public static UserSummary from(User user) {
        return new UserSummary(user.id(), user.username(), user.email(), user.role());
    }
}
