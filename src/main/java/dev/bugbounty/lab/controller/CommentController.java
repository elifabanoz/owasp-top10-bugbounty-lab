package dev.bugbounty.lab.controller;

import dev.bugbounty.lab.model.Actor;
import dev.bugbounty.lab.model.Comment;
import dev.bugbounty.lab.service.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/{id}/comments")
public class CommentController {
    public record NewComment(@NotBlank @Size(max = 5000) String body) {}

    private final CommentService comments;

    public CommentController(CommentService comments) {
        this.comments = comments;
    }

    @GetMapping
    public List<Comment> all(@PathVariable long id, Authentication authentication) {
        return comments.forReport(id, (Actor) authentication.getPrincipal());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@PathVariable long id, @Valid @RequestBody NewComment request,
                    Authentication authentication) {
        comments.add(id, (Actor) authentication.getPrincipal(), request.body());
    }
}
