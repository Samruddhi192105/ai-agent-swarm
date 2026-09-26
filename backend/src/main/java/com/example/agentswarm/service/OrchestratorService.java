package com.example.agentswarm.service;

import com.example.agentswarm.agent.*;
import com.example.agentswarm.dto.*;
import com.example.agentswarm.model.*;
import com.example.agentswarm.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// This is Phase 9 from the project plan: Planner -> Coder -> Tester -> Reviewer,
// looping Coder<->Tester<->Reviewer up to MAX_ITERATIONS on RETRY, then packaging
// the result once Reviewer says PASS.
@Service
public class OrchestratorService {

    private final PlannerAgent planner;
    private final CoderAgent coder;
    private final TesterAgent tester;
    private final ReviewerAgent reviewer;
    private final ZipService zipService;

    private final ProjectRepository projectRepository;
    private final AgentRunRepository agentRunRepository;
    private final GeneratedFileRepository generatedFileRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrchestratorService(PlannerAgent planner, CoderAgent coder, TesterAgent tester,
                                ReviewerAgent reviewer, ZipService zipService,
                                ProjectRepository projectRepository,
                                AgentRunRepository agentRunRepository,
                                GeneratedFileRepository generatedFileRepository) {
        this.planner = planner;
        this.coder = coder;
        this.tester = tester;
        this.reviewer = reviewer;
        this.zipService = zipService;
        this.projectRepository = projectRepository;
        this.agentRunRepository = agentRunRepository;
        this.generatedFileRepository = generatedFileRepository;
    }

    public Project run(String userRequirement) {
        Project project = new Project();
        project.setPrompt(userRequirement);
        project.setStatus(Project.ProjectStatus.PLANNING);
        project = projectRepository.save(project);
        final Long projectId = project.getId();

        // --- Planner (runs once) ---
        PlannerSpec spec = record(project, AgentRun.AgentType.PLANNER, 0, () -> planner.plan(userRequirement));
        project.setProjectName(spec.projectName());
        project.setPlannerSpecJson(toJson(spec));
        project.setStatus(Project.ProjectStatus.CODING);
        projectRepository.save(project);

        CoderOutput code = null;
        ReviewDecision lastDecision = null;

        for (int iteration = 1; iteration <= project.getMaxIterations(); iteration++) {
            project.setCurrentIteration(iteration);
            project.setStatus(Project.ProjectStatus.CODING);
            projectRepository.save(project);

            final int iter = iteration;
            final ReviewDecision feedback = lastDecision;
            code = record(project, AgentRun.AgentType.CODER, iter, () ->
        feedback == null
                ? coder.generate(spec)
                : coder.regenerate(spec, toJson(feedback)));
            persistFiles(project, code, iteration);

            project.setStatus(Project.ProjectStatus.TESTING);
            projectRepository.save(project);
            final CoderOutput finalCode = code;
            
            TestResult testResult = record(project, AgentRun.AgentType.TESTER, iter, () ->
        tester.test(finalCode, projectId, iter));

        if ("PASSED".equalsIgnoreCase(testResult.status())) {
            String zipPath = zipService.zip(
                    code,
                    project.getProjectName(),
                    project.getId()
            );

            project.setZipPath(zipPath);
            project.setStatus(Project.ProjectStatus.PASSED);
            projectRepository.save(project);

            return project;
        }

        // Only call Reviewer when something actually failed.
        project.setStatus(Project.ProjectStatus.REVIEWING);
        projectRepository.save(project);

        ReviewDecision decision = record(
                project,
                AgentRun.AgentType.REVIEWER,
                iter,
                () -> reviewer.review(userRequirement, finalCode, testResult)
        );

        if ("PASS".equalsIgnoreCase(decision.decision())) {
            String zipPath = zipService.zip(
                    code,
                    project.getProjectName(),
                    project.getId()
            );

            project.setZipPath(zipPath);
            project.setStatus(Project.ProjectStatus.PASSED);
            projectRepository.save(project);

            return project;
        }

        lastDecision = decision;
        }

        project.setStatus(Project.ProjectStatus.FAILED);
        return projectRepository.save(project);
    }

    private void persistFiles(Project project, CoderOutput code, int iteration) {
        for (CoderOutput.GeneratedFileDto f : code.files()) {
            GeneratedFile gf = new GeneratedFile();
            gf.setProject(project);
            gf.setPath(f.path());
            gf.setContent(f.content());
            gf.setIteration(iteration);
            generatedFileRepository.save(gf);
        }
    }

    private <T> T record(Project project, AgentRun.AgentType type, int iteration, java.util.function.Supplier<T> work) {
        AgentRun run = new AgentRun();
        run.setProject(project);
        run.setAgentType(type);
        run.setIteration(iteration);
        run.setStatus(AgentRun.RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run = agentRunRepository.save(run);

        try {
            T result = work.get();
            run.setStatus(AgentRun.RunStatus.SUCCESS);
            run.setOutputJson(toJson(result));
            run.setFinishedAt(Instant.now());
            agentRunRepository.save(run);
            return result;
        } catch (Exception e) {
            run.setStatus(AgentRun.RunStatus.FAILED);
            run.setLogs(e.getMessage());
            run.setFinishedAt(Instant.now());
            agentRunRepository.save(run);
            throw e;
        }
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
