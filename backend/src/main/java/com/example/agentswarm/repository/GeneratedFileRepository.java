package com.example.agentswarm.repository;

import com.example.agentswarm.model.GeneratedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GeneratedFileRepository extends JpaRepository<GeneratedFile, Long> {
    List<GeneratedFile> findByProjectIdAndIteration(Long projectId, int iteration);
    List<GeneratedFile> findByProjectId(Long projectId);
}
