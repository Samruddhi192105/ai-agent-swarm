package com.example.agentswarm.dto;

import java.util.List;

public record TestResult(
        String status,          // PASSED | FAILED
        boolean buildSuccessful,
        int testsPassed,
        int testsFailed,
        List<String> errors,
        String rawLog
) {}
