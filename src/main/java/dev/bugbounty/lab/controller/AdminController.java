package dev.bugbounty.lab.controller;

import dev.bugbounty.lab.model.Actor;
import dev.bugbounty.lab.model.UserSummary;
import dev.bugbounty.lab.service.AdminService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {
    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    public List<UserSummary> users() {
        return admin.users();
    }

    @PostMapping("/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(@PathVariable long id, @RequestParam String role, Authentication authentication) {
        admin.changeRole(id, role, (Actor) authentication.getPrincipal());
    }
}
