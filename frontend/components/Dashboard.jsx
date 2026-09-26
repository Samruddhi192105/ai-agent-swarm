"use client";

import CinematicBackground from "./CinematicBackground";

import { useState } from "react";
import {
  Activity,
  ArrowRight,
  Bot,
  Brain,
  CheckCircle2,
  CircleDot,
  Code2,
  Download,
  FileCode2,
  GitBranch,
  Layers3,
  Play,
  RotateCcw,
  Search,
  ShieldCheck,
  Sparkles,
  Terminal,
  TestTube2,
  XCircle,
} from "lucide-react";

const API_URL = "http://localhost:8080";

const agents = [
  {
    id: "01",
    name: "PLANNER",
    role: "ARCHITECT",
    description:
      "Analyzes the requirement and creates a structured implementation plan.",
    icon: Brain,
  },
  {
    id: "02",
    name: "CODER",
    role: "IMPLEMENTER",
    description:
      "Transforms the approved plan into a complete working codebase.",
    icon: Code2,
  },
  {
    id: "03",
    name: "TESTER",
    role: "VALIDATOR",
    description:
      "Runs tests, checks generated code and identifies implementation issues.",
    icon: TestTube2,
  },
  {
    id: "04",
    name: "REVIEWER",
    role: "QUALITY ENGINEER",
    description:
      "Reviews the final implementation for correctness and quality.",
    icon: ShieldCheck,
  },
];

const statusLabels = {
  IDLE: "STANDBY",
  PLANNING: "WORKING",
  CODING: "WORKING",
  TESTING: "WORKING",
  REVIEWING: "WORKING",
  PASSED: "PASSED",
  FAILED: "FAILED",
};

const statusForAgent = (agentIndex, status) => {
  if (!status) return "IDLE";

  const order = ["PLANNING", "CODING", "TESTING", "REVIEWING"];

  if (status === "FAILED") {
    return "FAILED";
  }

  if (status === "PASSED") {
    return "PASSED";
  }

  const currentIndex = order.indexOf(status);

  if (currentIndex === -1) {
    return "IDLE";
  }

  if (agentIndex < currentIndex) {
    return "PASSED";
  }

  if (agentIndex === currentIndex) {
    return status;
  }

  return "IDLE";
};

export default function Dashboard() {
  const [prompt, setPrompt] = useState("");
  const [status, setStatus] = useState("IDLE");
  const [projectId, setProjectId] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const [executionLogs, setExecutionLogs] = useState([
    {
      type: "command",
      text: "initialize --agents=4",
    },
    {
      type: "muted",
      text: "Awaiting project initialization...",
    },
  ]);

  const addLog = (type, text) => {
    setExecutionLogs((previous) => [
      ...previous,
      {
        type,
        text,
      },
    ]);
  };

  const buildProject = async () => {
    if (!prompt.trim() || loading) {
      return;
    }

    setLoading(true);
    setError("");
    setProjectId(null);
    setStatus("PLANNING");

    setExecutionLogs([
      {
        type: "command",
        text: `initialize --agents=4`,
      },
      {
        type: "green",
        text: "Swarm initialized.",
      },
      {
        type: "muted",
        text: "Planner agent started...",
      },
    ]);

    try {
      const response = await fetch(`${API_URL}/api/projects`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          prompt: prompt.trim(),
        }),
      });

      const rawText = await response.text();

      let data = null;

      try {
        data = rawText ? JSON.parse(rawText) : null;
      } catch {
        data = null;
      }

      if (!response.ok) {
        const backendError =
          data?.message ||
          data?.error ||
          data?.details ||
          rawText ||
          `Request failed with status ${response.status}`;

        throw new Error(backendError);
      }

      const returnedId = data?.id ?? data?.projectId ?? data?.project?.id;

      if (returnedId !== undefined && returnedId !== null) {
        setProjectId(returnedId);
      }

      const returnedStatus =
        data?.status ||
        data?.project?.status ||
        "PASSED";

      setStatus(returnedStatus);

      addLog("green", "Project orchestration completed.");
      addLog("green", `Final status: ${returnedStatus}`);

      if (returnedId !== undefined && returnedId !== null) {
        addLog("muted", `Project ID: ${returnedId}`);
      }
    } catch (err) {
      const message =
        err instanceof Error ? err.message : String(err);

      setStatus("FAILED");
      setError(message);

      addLog("error", `ERROR: ${message}`);
    } finally {
      setLoading(false);
    }
  };

  const resetDashboard = () => {
    setPrompt("");
    setStatus("IDLE");
    setProjectId(null);
    setError("");
    setLoading(false);

    setExecutionLogs([
      {
        type: "command",
        text: "initialize --agents=4",
      },
      {
        type: "muted",
        text: "Awaiting project initialization...",
      },
    ]);
  };

  const downloadProject = () => {
    if (!projectId || status !== "PASSED") return;

    window.open(
      `${API_URL}/api/projects/${projectId}/download`,
      "_blank"
    );
  };

  const artifactAvailable = projectId !== null && status === "PASSED";
  const artifactState = loading
    ? "BUILDING"
    : status === "FAILED"
    ? "FAILED"
    : artifactAvailable
    ? "AVAILABLE"
    : "WAITING";

  return (
    <main className="swarm-page">
      {/* BACKGROUND IMAGE */}
      {/* CINEMATIC VIDEO BACKGROUND */}
      <CinematicBackground />

      {/* BACKGROUND LAYERS */}
      <div className="background-overlay" />
      <div className="grid-background" />

      <div className="ambient ambient-one" />
      <div className="ambient ambient-two" />
      <div className="ambient ambient-three" />

      {/* HEADER */}
      <header className="topbar">
        <div className="brand-area">
          <div className="brand-icon">
            <Layers3 size={21} strokeWidth={2.2} />
          </div>

          <div>
            <div className="brand-title">
              AI <span>AGENT SWARM</span>
            </div>

            <div className="brand-subtitle">
              AUTONOMOUS SOFTWARE DEVELOPEMENT SYSTEM
            </div>
          </div>
        </div>

        <div className="system-status">
          <span className="status-light" />
          SYSTEM ONLINE
        </div>
      </header>

      {/* HERO */}
      <section className="hero-section">
        <div className="hero-orbit hero-orbit-one" />
        <div className="hero-orbit hero-orbit-two" />

        <div className="hero-badge">
          <Sparkles size={12} />
          MULTI-AGENT DEVELOPMENT ENGINE
        </div>

        <h1>
          Build software
          <br />
          <span>with autonomous agents.</span>
        </h1>

        <p>
          Describe what you want to build. The swarm plans,
          implements, tests and reviews the project automatically.
        </p>
      </section>

      {/* COMMAND PANEL */}
      <section className="command-panel">
        <div className="command-label">
          <Terminal size={13} />
          PROJECT COMMAND
        </div>

        <div className="command-row">
          <div className="input-wrapper">
            <span className="prompt-symbol">&gt;_</span>

            <input
              type="text"
              value={prompt}
              onChange={(event) => setPrompt(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  buildProject();
                }
              }}
              placeholder="Describe the project you want the swarm to build..."
              disabled={loading}
            />
          </div>
          <div className="command-actions">
          <button
            className={`build-button ${
              loading ? "running-button" : ""
            }`}
            onClick={buildProject}
            disabled={loading || !prompt.trim()}
          >
            {loading ? (
              <>
                <span className="loading-orb" />
                SWARM RUNNING
              </>
            ) : (
              <>
                <Play size={14} fill="currentColor" />
                INITIALIZE SWARM
              </>
            )}
          </button>

          <button
            className="reset-button"
            onClick={resetDashboard}
            disabled={loading}
          >
            <RotateCcw size={13} />
            RESET
          </button>
        </div>
        </div>
      </section>

      {/* AGENT NETWORK */}
      <section className="network-section">
        <div className="section-heading">
          <div>
            <div className="section-kicker">
              <Activity size={11} />
              AGENT NETWORK
            </div>

            <h2>Autonomous execution pipeline</h2>
          </div>

          <div className="iteration">
            ITERATION
            <strong>01</strong>
            <span>/ 01</span>
          </div>
        </div>

        <div className="agent-network">
          {agents.map((agent, index) => {
            const AgentIcon = agent.icon;
            const agentStatus = statusForAgent(index, status);

            return (
              <div
                className={`agent-card agent-${agentStatus.toLowerCase()}`}
                key={agent.id}
              >
                <div className="agent-top">
                  <span className="agent-number">
                    NODE_{agent.id}
                  </span>

                  <span className="agent-state">
                    <span className="live-dot" />
                    {statusLabels[agentStatus]}
                  </span>
                </div>

                <div className="agent-icon-wrapper">
                  <div className="agent-icon-ring">
                    <AgentIcon size={25} strokeWidth={1.8} />
                  </div>
                </div>

                <h3>{agent.name}</h3>

                <div className="agent-role">
                  {agent.role}
                </div>

                <p>{agent.description}</p>

                <div className="agent-footer">
                  {agentStatus === "PASSED" ? (
                    <>
                      <CheckCircle2 size={11} />
                      EXECUTION COMPLETE
                    </>
                  ) : agentStatus === "FAILED" ? (
                    <>
                      <XCircle size={11} />
                      EXECUTION FAILED
                    </>
                  ) : agentStatus === "PLANNING" ||
                    agentStatus === "CODING" ||
                    agentStatus === "TESTING" ||
                    agentStatus === "REVIEWING" ? (
                    <>
                      <CircleDot size={11} />
                      PROCESSING
                    </>
                  ) : (
                    <>
                      <CircleDot size={11} />
                      STANDBY
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        <div className="execution-status">
          <span className="execution-pulse" />

          SWARM STATUS:
          <strong>
            {status === "IDLE"
              ? "READY"
              : status === "FAILED"
              ? "FAILED"
              : status === "PASSED"
              ? "COMPLETE"
              : status}
          </strong>

          <span className="separator">/</span>

          <span>4 AGENTS</span>
        </div>
      </section>

      {/* OUTPUT */}
      <section className="output-grid">
        {/* EXECUTION CONSOLE */}
        <div className="glass-panel console-panel">
          <div className="panel-header">
            <div className="panel-title">
              <Terminal size={13} />
              EXECUTION CONSOLE
            </div>

            <div className="terminal-dots">
              <span />
              <span />
              <span />
            </div>
          </div>

          {/* IMPORTANT:
              This wrapper makes long errors scrollable.
          */}
          <div className="terminal-body">
            <div className="console-output">
              {executionLogs.map((log, index) => (
                <div
                  className={`terminal-line terminal-${log.type}`}
                  key={`${log.type}-${index}`}
                >
                  {log.type === "command" ? (
                    <>
                      <span className="terminal-green">
                        swarm@engine
                      </span>

                      <span className="terminal-white">
                        :~$
                      </span>

                      <span className="terminal-command">
                        {log.text}
                      </span>
                    </>
                  ) : (
                    <span>{log.text}</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* BUILD ARTIFACT */}
        <div className="glass-panel">
          <div className="panel-header">
            <div className="panel-title">
              <FileCode2 size={13} />
              BUILD ARTIFACT
            </div>

            <div className={`artifact-state artifact-state-${artifactState.toLowerCase()}`}>
              <span className="artifact-state-dot" />
              {artifactState}
            </div>
          </div>

          {artifactAvailable ? (
            <div className="output-content artifact-ready">
              <div className="artifact-icon">
                <FileCode2 size={28} />
              </div>

              <div className="artifact-name">
                Generated Project
              </div>

              <div className="artifact-description">
                Your autonomous software project has been
                generated by the agent swarm.
              </div>

              <button
                className="download-button"
                onClick={downloadProject}
              >
                <Download size={13} />
                DOWNLOAD PROJECT
              </button>
            </div>
          ) : artifactState === "BUILDING" ? (
            <div className="output-content">
              <div className="empty-artifact artifact-building">
                <div className="empty-icon">
                  <CircleDot size={23} />
                </div>
                <strong>Building artifact</strong>
                <span>The swarm is generating and validating your project.</span>
              </div>
            </div>
          ) : artifactState === "FAILED" ? (
            <div className="output-content">
              <div className="empty-artifact artifact-failed">
                <div className="empty-icon">
                  <XCircle size={23} />
                </div>
                <strong>Artifact unavailable</strong>
                <span>{error || "The project did not pass validation."}</span>
              </div>
            </div>
          ) : (
            <div className="output-content">
              <div className="empty-artifact">
                <div className="empty-icon">
                  <Bot size={23} />
                </div>

                <strong>No artifact generated</strong>

                <span>
                  Start a swarm execution to generate
                  your project.
                </span>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* FOOTER */}
      <footer className="footer">
        <div>
          AI AGENT SWARM
          <span className="separator"> • </span>
          AUTONOMOUS DEVELOPMENT
        </div>

        <div>
          <span className="footer-online" />
          ALL SYSTEMS OPERATIONAL
        </div>
      </footer>
    </main>
  );
}