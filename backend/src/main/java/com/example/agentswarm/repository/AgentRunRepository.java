package com.example.agentswarm.repository;

import com.example.agentswarm.model.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {
    List<AgentRun> findByProjectIdOrderByStartedAtAsc(Long projectId);
}
