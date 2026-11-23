package dev.bugbounty.lab.service;

import dev.bugbounty.lab.config.LabMode;
import dev.bugbounty.lab.model.Actor;
import dev.bugbounty.lab.model.SecurityEvent;
import dev.bugbounty.lab.model.UserSummary;
import dev.bugbounty.lab.repository.LabExerciseRepository;
import dev.bugbounty.lab.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository users;
    private final LabExerciseRepository exercises;
    private final LabMode mode;

    public AdminService(UserRepository users, LabExerciseRepository exercises, LabMode mode) {
        this.users = users;
        this.exercises = exercises;
        this.mode = mode;
    }

    public List<UserSummary> users() {
        return users.all().stream().map(UserSummary::from).toList();
    }

    @Transactional
    public void changeRole(long id, String role, Actor actor) {
        if (!List.of("RESEARCHER", "TRIAGER", "ADMIN").contains(role)) {
            throw new InvalidRequestException("Invalid role");
        }
        var target = users.byId(id).orElseThrow(ResourceNotFoundException::new);
        if (target.role().equals(role)) return;
        users.changeRole(id, role);
        if (!mode.vulnerable()) {
            exercises.recordSecurity("ROLE_CHANGED", actor.id(), id);
            if ("ADMIN".equals(role)) exercises.recordSecurity("ROLE_PROMOTION_ALERT", actor.id(), id);
        }
    }

    public List<SecurityEvent> securityEvents() {
        return exercises.securityEvents();
    }
}
