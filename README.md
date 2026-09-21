# 🤖 AI Agent Swarm

> An AI-powered multi-agent software engineering system that plans, generates, tests, reviews, and iteratively improves software projects through specialized AI agents.

## 📌 Overview

**AI Agent Swarm** is a multi-agent AI system designed to automate parts of the software development lifecycle.

Instead of relying on a single AI model to generate an entire application, the system divides software development into specialized responsibilities handled by different AI agents.

The swarm is orchestrated by a central **Spring Boot backend**, which coordinates agents through a structured workflow:

**User Requirement → Planner → Coder → Tester → Reviewer → Iteration → Generated Project**

The goal is to create a system capable of taking a natural-language software requirement and producing a structured, tested, and reviewed project that can be run and deployed by the user.

---

## 🖥️ Project Screenshots

### 🏠 AI Agent Swarm — Main Interface

The main interface allows users to describe the software project they want to build and initialize the multi-agent development workflow.

<img width="959" height="412" alt="Screenshot 2026-09-21 193520" src="https://github.com/user-attachments/assets/ca3c2d63-8cba-4dc5-b149-fb350f63f7e4" />

---

### 🧠 Autonomous Agent Network

The system visualizes the four specialized agents responsible for different stages of the software engineering workflow.

* **Planner** — Analyzes requirements and creates an implementation plan.
* **Coder** — Transforms the plan into a working codebase.
* **Tester** — Runs tests and identifies implementation issues.
* **Reviewer** — Reviews the implementation for correctness and quality.

<img width="959" height="412" alt="Screenshot 2026-09-21 193520" src="https://github.com/user-attachments/assets/450a007a-31ab-40ce-9042-7bf05ffae7da" />

---

### ⚡ Execution Console & Build Artifact

The execution interface provides visibility into the swarm's execution process while the generated project is represented as a build artifact.

<img width="959" height="412" alt="Screenshot 2026-09-21 193414" src="https://github.com/user-attachments/assets/db1a1d85-4d95-465e-bfbd-b3691553047e" />

---

## ✨ Key Features

* 🧠 **Multi-Agent Architecture** — Separate AI agents for planning, coding, testing, and reviewing.
* 📋 **Requirement Planning** — Converts natural-language requirements into a structured implementation plan.
* 💻 **Code Generation** — Generates project files and source code based on the approved plan.
* 🧪 **Automated Testing** — Generates and executes tests against the generated project.
* 🔍 **AI Code Review** — Reviews generated code for potential issues and improvements.
* 🔄 **Iterative Development** — Allows failed tests or review feedback to be sent back into the development cycle.
* 🐳 **Docker Sandbox** — Runs generated projects in isolated containers to reduce risks from executing generated code.
* 📦 **Project Export** — Produces the generated project as a structured project directory that can be packaged and downloaded.
* 🔐 **Backend Orchestration** — Spring Boot manages the agent workflow and communication.
* 🚀 **CI/CD Ready** — Designed with GitHub Actions and Docker-based deployment workflows.
* 🆓 **Free/Local LLM Support** — Can be developed using locally hosted or free-tier LLM providers.

---

## 🏗️ System Architecture

```text
                    ┌─────────────────────┐
                    │       User          │
                    │ Software Requirement│
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Spring Boot API   │
                    │     Orchestrator    │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Planner Agent     │
                    │ Requirements → Plan │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │     Coder Agent     │
                    │   Plan → Source     │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │     Docker Sandbox  │
                    │ Build & Execute Code│
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │     Tester Agent    │
                    │ Generate & Run Tests│
                    └──────────┬──────────┘
                               │
                         Tests Pass?
                         /          \
                       Yes           No
                        │             │
                        ▼             ▼
               ┌──────────────┐   Feedback
               │ Reviewer     │      │
               │    Agent     │◄─────┘
               └──────┬───────┘
                      │
                      ▼
               ┌──────────────┐
               │ Final Project│
               └──────────────┘
```

---

## 🧩 Agents

### 🧠 Planner Agent

Responsible for analyzing the user's requirement and creating a structured development plan.

Responsibilities:

* Understand functional requirements
* Identify application components
* Select an appropriate project structure
* Break the project into implementation tasks
* Define required APIs, database models, and dependencies

### 💻 Coder Agent

Converts the planner's implementation plan into actual project files.

Responsibilities:

* Generate source code
* Create project structure
* Generate configuration files
* Implement APIs and application logic
* Create frontend/backend components
* Update code based on feedback

### 🧪 Tester Agent

Validates the generated project.

Responsibilities:

* Generate test cases
* Execute tests
* Detect build/runtime failures
* Analyze test results
* Return structured feedback to the orchestrator

### 🔍 Reviewer Agent

Performs an additional AI-based review of the generated project.

Responsibilities:

* Review code quality
* Identify potential bugs
* Detect missing requirements
* Analyze maintainability
* Suggest improvements
* Provide feedback to the development loop

---

## 🔄 Agent Workflow

```text
User Requirement
       ↓
Planner
       ↓
Architecture + Task Breakdown
       ↓
Coder
       ↓
Generated Source Code
       ↓
Docker Sandbox
       ↓
Tester
       ↓
Test Results
       ↓
Reviewer
       ↓
Feedback
       ↓
Coder
       ↓
Improved Project
```

This approach moves beyond simple **one-shot code generation** toward an iterative software engineering workflow.

---

## 🛠️ Tech Stack

### Backend

* **Java**
* **Spring Boot**
* **Spring Web**
* **REST APIs**
* **Maven**

### AI / Agents

* LLM-based agent architecture
* Planner Agent
* Coder Agent
* Tester Agent
* Reviewer Agent
* Local/free-tier LLM providers

### Execution

* **Docker**
* Docker Sandbox
* Containerized project execution

### Frontend

* **Next.js**
* **React**
* **Tailwind CSS**

### Development & DevOps

* **Git**
* **GitHub**
* **GitHub Actions**
* **CI/CD**

---

## 📁 Project Structure

```text
ai-agent-swarm/
│
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       └── resources/
│   │
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/
│   ├── app/
│   ├── components/
│   ├── public/
│   ├── package.json
│   └── Dockerfile
│
├── agents/
│   ├── planner/
│   ├── coder/
│   ├── tester/
│   └── reviewer/
│
├── sandbox/
│   └── Docker execution environment
│
├── screenshots/
│   ├── home.png
│   ├── agents.png
│   └── console.png
│
├── docker-compose.yml
│
├── .github/
│   └── workflows/
│       └── ci.yml
│
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

* Java 25+
* Maven
* Node.js
* Docker Desktop
* Git
* Ollama or another supported LLM provider

### Clone the Repository

```bash
git clone https://github.com/Samruddhi192105/ai-agent-swarm.git

cd ai-agent-swarm
```

### Run with Docker

```bash
docker compose up --build
```

Or run in the background:

```bash
docker compose up -d --build
```

Stop the application:

```bash
docker compose down
```

---

## ⚙️ Environment Variables

Create the required environment configuration before running the application.

Example:

```env
LLM_PROVIDER=ollama
LLM_MODEL=<your-model>
LLM_BASE_URL=<your-llm-url>

SPRING_PROFILES_ACTIVE=dev
```

Do not commit API keys or other secrets to GitHub.

---

## 🧪 Testing

Backend:

```bash
cd backend
mvn test
```

Build:

```bash
mvn clean package
```

Frontend:

```bash
cd frontend
npm install
npm run build
```

---

## 🔁 CI/CD

The project is designed around an automated CI/CD workflow:

```text
Git Push
   ↓
GitHub Actions
   ↓
Build
   ↓
Unit Tests
   ↓
Integration Tests
   ↓
Docker Build
   ↓
Validation
   ↓
Deployment
```

---

## 🔐 Security Considerations

Executing AI-generated code introduces security risks.

The project uses isolated Docker-based execution to reduce the risk of generated code directly affecting the host environment.

Potential security controls include:

* Container isolation
* Resource limits
* Execution timeouts
* Restricted filesystem access
* Restricted network access
* Non-root containers
* Input validation
* Secret isolation
* Generated-code validation
