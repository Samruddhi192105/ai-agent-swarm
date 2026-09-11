package com.example.agentswarm.dto;

import java.util.List;

public record ReviewDecision(
        String decision,   // PASS | RETRY
        List<Issue> issues
) {
    public record Issue(String file, String problem, String suggestedFix) {}
}
