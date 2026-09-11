package com.example.agentswarm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoderOutput(List<GeneratedFileDto> files) {

    public record GeneratedFileDto(
            String path,
            String content
    ) {}
}