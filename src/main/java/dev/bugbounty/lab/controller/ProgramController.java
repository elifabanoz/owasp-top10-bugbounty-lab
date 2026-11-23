package dev.bugbounty.lab.controller;

import dev.bugbounty.lab.model.Program;
import dev.bugbounty.lab.service.ProgramService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/programs")
public class ProgramController {
    private final ProgramService programs;

    public ProgramController(ProgramService programs) {
        this.programs = programs;
    }

    @GetMapping
    public List<Program> all() {
        return programs.all();
    }
}
