package com.example.agentswarm.docker;

public record SandboxResult(boolean buildSuccessful, int exitCode, String log) {}
