package dev.bugbounty.lab.controller;

import dev.bugbounty.lab.config.LabMode;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta")
public class MetaController {
    private final LabMode mode;

    public MetaController(LabMode mode) {
        this.mode = mode;
    }

    @GetMapping
    public Map<String, String> meta() {
        return Map.of("profile", mode.name());
    }
}
