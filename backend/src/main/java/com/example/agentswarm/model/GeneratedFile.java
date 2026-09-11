package com.example.agentswarm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "generated_files")
@Getter
@Setter
public class GeneratedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    private String path;

    @Column(columnDefinition = "TEXT")
    private String content;

    private int iteration;
}
