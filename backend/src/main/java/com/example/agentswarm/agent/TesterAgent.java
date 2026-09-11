package com.example.agentswarm.agent;

import com.example.agentswarm.docker.DockerSandboxService;
import com.example.agentswarm.docker.SandboxResult;
import com.example.agentswarm.dto.CoderOutput;
import com.example.agentswarm.dto.TestResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TesterAgent {

    private final DockerSandboxService sandbox;

    private static final Pattern SUREFIRE_SUMMARY =
            Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+)");

    public TesterAgent(DockerSandboxService sandbox) {
        this.sandbox = sandbox;
    }

    // Writes the generated project to disk, builds + tests it inside an
    // isolated, disposable container, and turns the raw output into a
    // structured TestResult the Reviewer can reason about.
    public TestResult test(CoderOutput project, long projectId, int iteration) {
        SandboxResult result = sandbox.buildAndTest(project, projectId, iteration);

        List<String> errors = new ArrayList<>();
        int passed = 0;
        int failed = 0;

        Matcher m = SUREFIRE_SUMMARY.matcher(result.log());
        while (m.find()) {
            int run = Integer.parseInt(m.group(1));
            int failures = Integer.parseInt(m.group(2));
            int err = Integer.parseInt(m.group(3));
            failed += failures + err;
            passed += run - failures - err;
        }

        if (!result.buildSuccessful()) {
            errors.add("Build failed - see raw log");
        }
        if (failed > 0) {
            errors.add(failed + " test(s) failed");
        }

        String status = (result.buildSuccessful() && failed == 0) ? "PASSED" : "FAILED";

        return new TestResult(status, result.buildSuccessful(), passed, failed, errors, result.log());
    }
}
