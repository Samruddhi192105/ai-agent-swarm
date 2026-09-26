package com.example.agentswarm.service;

import com.example.agentswarm.dto.CoderOutput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    @Value("${zip.output-dir:/tmp/agentswarm-zips}")
    private String outputDir;

    public String zip(CoderOutput project, String projectName, long projectId) {
        try {
            Files.createDirectories(Paths.get(outputDir));
            String safeName = (projectName == null ? "project" : projectName)
                    .replaceAll("[^a-zA-Z0-9-_]", "-");
            if (safeName.isBlank()) {
                safeName = "project";
            }
            Path zipPath = Paths.get(outputDir, safeName + "-" + projectId + ".zip");

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                for (CoderOutput.GeneratedFileDto file : project.files()) {
                    Path relativePath = Paths.get(file.path().replace('\\', '/')).normalize();
                    if (relativePath.isAbsolute()
                            || relativePath.startsWith("..")
                            || relativePath.toString().isBlank()
                            || relativePath.toString().equals(".")
                            || file.path().matches("^[A-Za-z]:.*")) {
                        throw new IllegalArgumentException("Unsafe generated file path: " + file.path());
                    }

                    String entryName = safeName + "/" + relativePath.toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.write(file.content().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }
            return zipPath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to zip project", e);
        }
    }
}
