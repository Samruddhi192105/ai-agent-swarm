package com.example.agentswarm.service;

import com.example.agentswarm.dto.CoderOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZipServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void zipKeepsGeneratedDirectoryTreeUnderProjectFolder() throws Exception {
        ZipService zipService = new ZipService();
        ReflectionTestUtils.setField(zipService, "outputDir", tempDir.toString());
        CoderOutput project = new CoderOutput(List.of(
                new CoderOutput.GeneratedFileDto("pom.xml", "<project/>"),
                new CoderOutput.GeneratedFileDto("src/main/java/app/Main.java", "class Main {}")
        ));

        Path zipPath = Path.of(zipService.zip(project, "Parking Management System", 7));

        try (InputStream input = Files.newInputStream(zipPath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry firstEntry = zip.getNextEntry();
            assertEquals("Parking-Management-System/pom.xml", firstEntry.getName());
            ZipEntry secondEntry = zip.getNextEntry();
            assertEquals("Parking-Management-System/src/main/java/app/Main.java", secondEntry.getName());
        }
    }

    @Test
    void zipRejectsPathsThatEscapeProjectFolder() {
        ZipService zipService = new ZipService();
        ReflectionTestUtils.setField(zipService, "outputDir", tempDir.toString());
        CoderOutput project = new CoderOutput(List.of(
                new CoderOutput.GeneratedFileDto("../outside.txt", "unsafe")
        ));

        assertThrows(IllegalArgumentException.class,
                () -> zipService.zip(project, "sample", 8));
    }
}