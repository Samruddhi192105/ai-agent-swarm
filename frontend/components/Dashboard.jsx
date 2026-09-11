"use client";

import React, { useEffect, useRef, useState } from "react";
import {
Cpu,
Hammer,
FlaskConical,
ShieldCheck,
Download,
Play,
GitBranch,
Terminal,
RotateCcw,
Sparkles,
Activity,
Zap,
CheckCircle2,
Circle,
} from "lucide-react";

const API_URL =
process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const AGENTS = [
{
key: "PLANNER",
label: "PLANNER",
role: "Architecture Intelligence",
desc: "Breaks your idea into a technical blueprint.",
icon: Cpu,
},
{
key: "CODER",
label: "CODER",
role: "Implementation Engine",
desc: "Transforms the blueprint into working code.",
icon: Hammer,
},
{
key: "TESTER",
label: "TESTER",
role: "Quality Engine",
desc: "Builds, executes and validates the project.",
icon: FlaskConical,
},
{
key: "REVIEWER",
label: "REVIEWER",
role: "Code Intelligence",
desc: "Inspects the final system before delivery.",
icon: ShieldCheck,
},
];

const STATUS_TO_AGENT = {
PLANNING: "PLANNER",
CODING: "CODER",
TESTING: "TESTER",
REVIEWING: "REVIEWER",
};

export default function Dashboard() {
const [prompt, setPrompt] = useState(
"Build me a REST API for a Todo application"
);

const [project, setProject] = useState(null);
const [phase, setPhase] = useState("idle");
const [error, setError] = useState(null);
const pollRef = useRef(null);

useEffect(() => {
return () => clearInterval(pollRef.current);
}, []);

const startBuild = async () => {
if (!prompt.trim()) return;

setPhase("running");
setError(null);
setProject(null);

try {
  const res = await fetch(`${API_URL}/api/projects`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      prompt: prompt.trim(),
    }),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));

    throw new Error(
      body.error || `Request failed (${res.status})`
    );
  }

  const data = await res.json();

  setProject(data);

  if (data.status === "PASSED") {
    setPhase("done");
  } else {
    setPhase("error");
  }
} catch (e) {
  setError(e.message);
  setPhase("error");
}

};

const reset = () => {
clearInterval(pollRef.current);
setProject(null);
setPhase("idle");
setError(null);
};

const activeAgent = project
? STATUS_TO_AGENT[project.status]
: null;

const getAgentStatus = (agentKey) => {
if (!project) return "idle";

if (project.status === "PASSED") {
  return "passed";
}

if (project.status === "FAILED") {
  return "failed";
}

if (agentKey === activeAgent) {
  return "working";
}

return "idle";

};

const getStatusText = (status) => {
switch (status) {
case "working":
return "ACTIVE";
case "passed":
return "COMPLETE";
case "failed":
return "FAILED";
default:
return "STANDBY";
}
};

return ( <main className="swarm-page">

  {/* BACKGROUND EFFECTS */}
  <div className="ambient ambient-one" />
  <div className="ambient ambient-two" />
  <div className="grid-background" />

  {/* HEADER */}
  <header className="topbar">

    <div className="brand-area">
      <div className="brand-icon">
        <Sparkles size={20} />
      </div>

      <div>
        <div className="brand-title">
          AGENT<span>SWARM</span>
        </div>

        <div className="brand-subtitle">
          AUTONOMOUS SOFTWARE ENGINEERING SYSTEM
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

    <div className="hero-badge">
      <Zap size={13} />
      MULTI-AGENT DEVELOPMENT ENGINE
    </div>

    <h1>
      Describe it.
      <br />
      <span>Let the swarm build it.</span>
    </h1>

    <p>
      Four specialized AI agents collaborate to plan, code,
      test and review your software automatically.
    </p>

  </section>


  {/* COMMAND CENTER */}
  <section className="command-panel">

    <div className="command-label">
      <Terminal size={14} />
      BUILD COMMAND
    </div>

    <div className="command-row">

      <div className="input-wrapper">
        <span className="prompt-symbol">&gt;</span>

        <input
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          placeholder="Describe the software you want to build..."
          disabled={phase === "running"}
        />
      </div>

      {phase !== "running" ? (
        <button
          className="build-button"
          onClick={startBuild}
        >
          <Play size={16} fill="currentColor" />
          INITIALIZE BUILD
        </button>
      ) : (
        <button
          className="build-button running-button"
          disabled
        >
          <span className="loading-orb" />
          SWARM ACTIVE
        </button>
      )}

      {(phase === "done" || phase === "error") && (
        <button
          className="reset-button"
          onClick={reset}
        >
          <RotateCcw size={15} />
          NEW BUILD
        </button>
      )}

    </div>

  </section>


  {/* AGENT NETWORK */}
  <section className="network-section">

    <div className="section-heading">

      <div>
        <div className="section-kicker">
          <Activity size={14} />
          NEURAL WORKFLOW
        </div>

        <h2>Agent Network</h2>
      </div>

      <div className="iteration">
        ITERATIONS
        <strong>
          {project?.currentIteration || 0}
        </strong>
        <span>/ 2</span>
      </div>

    </div>


    <div className="agent-network">

      {AGENTS.map((agent, index) => {

        const Icon = agent.icon;
        const status = getAgentStatus(agent.key);

        return (
          <React.Fragment key={agent.key}>

            <div
              className={`agent-card agent-${status}`}
            >

              <div className="agent-top">

                <span className="agent-number">
                  0{index + 1}
                </span>

                <span className="agent-state">
                  {status === "working" && (
                    <span className="live-dot" />
                  )}

                  {getStatusText(status)}
                </span>

              </div>


              <div className="agent-icon-wrapper">
                <div className="agent-icon-ring">
                  <Icon size={28} strokeWidth={1.5} />
                </div>
              </div>


              <h3>{agent.label}</h3>

              <div className="agent-role">
                {agent.role}
              </div>

              <p>{agent.desc}</p>


              <div className="agent-footer">

                {status === "passed" ? (
                  <CheckCircle2 size={15} />
                ) : (
                  <Circle size={15} />
                )}

                <span>
                  {status === "working"
                    ? "PROCESSING TASK"
                    : status === "passed"
                    ? "TASK VERIFIED"
                    : status === "failed"
                    ? "TASK FAILED"
                    : "AWAITING INPUT"}
                </span>

              </div>

            </div>


            {index < AGENTS.length - 1 && (
              <div className="network-connector">

                <div className="connector-core" />

                <div className="connector-line" />

                <div className="connector-arrow">
                  →
                </div>

              </div>
            )}

          </React.Fragment>
        );
      })}

    </div>


    {project && (
      <div className="execution-status">

        <span className="execution-pulse" />

        <span>
          SWARM EXECUTION
        </span>

        <strong>
          {project.status}
        </strong>

        <span className="separator">•</span>

        ITERATION{" "}
        {project.currentIteration || 1} / 3

      </div>
    )}

  </section>


  {/* OUTPUT AREA */}
  <section className="output-grid">

    {/* TERMINAL */}
    <div className="glass-panel terminal-panel">

      <div className="panel-header">

        <div className="panel-title">
          <Terminal size={15} />
          EXECUTION CONSOLE
        </div>

        <div className="terminal-dots">
          <span />
          <span />
          <span />
        </div>

      </div>


      <div className="terminal-body">

        <div className="terminal-line">
          <span className="terminal-green">
            swarm@engine
          </span>

          <span className="terminal-white">
            :~$
          </span>

          <span className="terminal-command">
            initialize --agents=4
          </span>
        </div>


        {error && (
          <div className="terminal-error">
            ERROR: {error}
          </div>
        )}


        {!error && !project && (
          <>
            <div className="terminal-muted">
              waiting for build command...
            </div>

            <div className="terminal-muted">
              four agents standing by.
            </div>
          </>
        )}


        {project && (
          <>
            <div className="terminal-success">
              ✓ project pipeline initialized
            </div>

            <div className="terminal-line">
              <span className="terminal-green">
                project:
              </span>

              <span>
                #{project.id}
              </span>
            </div>

            <div className="terminal-line">
              <span className="terminal-green">
                name:
              </span>

              <span>
                {project.projectName || "unnamed"}
              </span>
            </div>

            <div className="terminal-line">
              <span className="terminal-green">
                status:
              </span>

              <span>
                {project.status}
              </span>
            </div>
          </>
        )}

      </div>

    </div>


    {/* OUTPUT */}
    <div className="glass-panel output-panel">

      <div className="panel-header">

        <div className="panel-title">
          <GitBranch size={15} />
          BUILD ARTIFACT
        </div>

        <span className="artifact-state">
          {project?.status === "PASSED"
            ? "READY"
            : "WAITING"}
        </span>

      </div>


      <div className="output-content">

        {project?.status === "PASSED" ? (

          <>
            <div className="artifact-icon">
              <CheckCircle2 size={30} />
            </div>

            <div className="artifact-name">
              {project.projectName}.zip
            </div>

            <div className="artifact-description">
              Build verified successfully.
              Your generated project is ready.
            </div>

            <a
              href={`${API_URL}/api/projects/${project.id}/download`}
              className="download-button"
            >
              <Download size={16} />
              DOWNLOAD PROJECT
            </a>
          </>

        ) : (

          <div className="empty-artifact">

            <div className="empty-icon">
              <Cpu size={25} />
            </div>

            <strong>
              Artifact unavailable
            </strong>

            <span>
              Complete the agent workflow to
              generate your project package.
            </span>

          </div>

        )}

      </div>

    </div>

  </section>


  {/* FOOTER */}
  <footer className="footer">

    <div>
      AGENT SWARM / AUTONOMOUS BUILD SYSTEM
    </div>

    <div>
      <span className="footer-online" />
      ALL SYSTEMS NOMINAL
    </div>

  </footer>

</main>


);
}
