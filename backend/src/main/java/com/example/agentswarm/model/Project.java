package com.example.agentswarm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String prompt;

    private String projectName;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.PENDING;

    private int currentIteration = 0;

    private int maxIterations = 3;

    @Column(columnDefinition = "TEXT")
    private String plannerSpecJson;

    private String zipPath;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgentRun> agentRuns = new ArrayList<>();

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public enum ProjectStatus {
        PENDING, PLANNING, CODING, TESTING, REVIEWING, PASSED, FAILED
    }
}
