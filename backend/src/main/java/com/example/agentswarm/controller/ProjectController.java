package com.example.agentswarm.controller;

import com.example.agentswarm.model.Project;
import com.example.agentswarm.repository.ProjectRepository;
import com.example.agentswarm.service.OrchestratorService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final OrchestratorService orchestrator;
    private final ProjectRepository projectRepository;

    public ProjectController(OrchestratorService orchestrator, ProjectRepository projectRepository) {
        this.orchestrator = orchestrator;
        this.projectRepository = projectRepository;
    }

    // Synchronous for simplicity; swap for @Async + WebSocket progress events
    // once the dashboard needs live updates instead of polling.
    @PostMapping
    public ResponseEntity<Project> build(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Project result = orchestrator.run(prompt);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> get(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long id) {
        Project project = projectRepository.findById(id).orElseThrow();
        if (project.getZipPath() == null) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(project.getZipPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + project.getProjectName() + ".zip\"")
                .body(resource);
    }
}
