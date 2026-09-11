package com.example.agentswarm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "agent_runs")
@Getter
@Setter
public class AgentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    private int iteration;

    @Enumerated(EnumType.STRING)
    private RunStatus status;

    @Column(columnDefinition = "TEXT")
    private String outputJson;

    @Column(columnDefinition = "TEXT")
    private String logs;

    private Instant startedAt = Instant.now();
    private Instant finishedAt;

    public enum AgentType { PLANNER, CODER, TESTER, REVIEWER }
    public enum RunStatus { RUNNING, SUCCESS, FAILED }
}
