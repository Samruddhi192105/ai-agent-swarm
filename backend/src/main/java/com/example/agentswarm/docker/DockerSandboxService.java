package com.example.agentswarm.docker;

import com.example.agentswarm.dto.CoderOutput;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.core.DockerClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

// Runs every generated project inside a fresh, disposable container instead
// of executing AI-generated code on the host. Each call gets: a clean temp
// workspace, a resource-limited container, a hard timeout, and guaranteed cleanup.
@Service
public class DockerSandboxService {

    @Value("${sandbox.build-image:maven:3.9-eclipse-temurin-21}")
    private String buildImage;

    @Value("${sandbox.timeout-seconds:180}")
    private long timeoutSeconds;

    @Value("${sandbox.memory-limit-mb:1024}")
    private long memoryLimitMb;

    @Value("${sandbox.workspace-root:/tmp/agentswarm-runs}")
    private String workspaceRoot;

    private final DockerClient dockerClient;

    public DockerSandboxService() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    public SandboxResult buildAndTest(CoderOutput project, long projectId, int iteration) {
        Path workDir = null;
        String containerId = null;
        try {
            workDir = materializeProject(project, projectId, iteration);

            String containerName = "agentswarm-run-" + projectId + "-" + iteration + "-" + System.currentTimeMillis();

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(com.github.dockerjava.api.model.Bind.parse(workDir.toAbsolutePath() + ":/workspace"))
                    .withMemory(memoryLimitMb * 1024 * 1024)
                    .withNetworkMode("none")          // no network inside the sandbox
                    .withAutoRemove(false);            // we remove manually after collecting logs

            CreateContainerResponse container = dockerClient.createContainerCmd(buildImage)
                    .withName(containerName)
                    .withWorkingDir("/workspace")
                    .withCmd("sh", "-c", "mvn -q -B compile test")
                    .withHostConfig(hostConfig)
                    .exec();

            containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            StringBuilder logBuffer = new StringBuilder();
            LogCollector collector = new LogCollector(logBuffer);
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(collector);

            Integer exitCode = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS);

            boolean success = exitCode != null && exitCode == 0;
            return new SandboxResult(success, exitCode == null ? -1 : exitCode, logBuffer.toString());

        } catch (Exception e) {
            return new SandboxResult(false, -1, "Sandbox execution failed: " + e.getMessage());
        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) {}
            }
            if (workDir != null) {
                deleteRecursively(workDir);
            }
        }
    }

    private Path materializeProject(CoderOutput project, long projectId, int iteration) throws IOException {
        Path dir = Paths.get(workspaceRoot, "project-" + projectId, "iter-" + iteration);
        Files.createDirectories(dir);
        for (CoderOutput.GeneratedFileDto file : project.files()) {
            Path target = dir.resolve(file.path());
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.content());
        }
        return dir;
    }

    private void deleteRecursively(Path path) {
        try {
            if (!Files.exists(path)) return;
            Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }

    private static class LogCollector extends ResultCallback.Adapter<Frame> {
        private final StringBuilder buffer;
        LogCollector(StringBuilder buffer) { this.buffer = buffer; }
        @Override
        public void onNext(Frame frame) {
            buffer.append(new String(frame.getPayload()));
        }
    }
}
