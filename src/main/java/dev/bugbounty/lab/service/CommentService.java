package dev.bugbounty.lab.service;

import dev.bugbounty.lab.model.Comment;
import dev.bugbounty.lab.model.Actor;
import dev.bugbounty.lab.repository.CommentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CommentService {
    private final CommentRepository comments;
    private final ReportService reports;
    private final ContentPolicy content;

    public CommentService(CommentRepository comments, ReportService reports, ContentPolicy content) {
        this.comments = comments;
        this.reports = reports;
        this.content = content;
    }

    public List<Comment> forReport(long reportId, Actor actor) {
        reports.detail(reportId, actor);
        return comments.forReport(reportId);
    }

    public void add(long reportId, Actor actor, String body) {
        reports.detail(reportId, actor);
        comments.create(reportId, actor.id(), content.onWrite(body));
    }
}
