package dev.bugbounty.lab.service;

import dev.bugbounty.lab.model.Program;
import dev.bugbounty.lab.repository.ProgramRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProgramService {
    private final ProgramRepository programs;

    public ProgramService(ProgramRepository programs) {
        this.programs = programs;
    }

    public List<Program> all() {
        return programs.all();
    }
}
