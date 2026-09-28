# ============================================================
# docs/pipeline-description.md
# Purpose: Comprehensive description of the CI/CD pipeline,
#          branching strategy, and operational procedures.
# Usage: Developer onboarding, architecture reviews, incident
#        response, and audit documentation.
# ============================================================

# CI/CD Pipeline — Complete Description

## Table of Contents

1. [Overview](#overview)
2. [Branching Strategy (GitHub Flow)](#branching-strategy)
3. [Pipeline Architecture](#pipeline-architecture)
4. [Workflow Files](#workflow-files)
5. [Stage Details](#stage-details)
6. [Environment Configuration](#environment-configuration)
7. [Secrets & Variables](#secrets--variables)
8. [Rollback Strategy](#rollback-strategy)
9. [Infrastructure Requirements](#infrastructure-requirements)
10. [Setup Checklist](#setup-checklist)

---

## Overview

This repository uses a **GitHub Flow** branching strategy with three tiers of CI/CD automation:

| Branch Pattern | Pipeline | Deploys To |
|----------------|----------|------------|
| `feature/*` | CI only — Build, Test, Sonar | Nowhere |
| `develop` | Full CI/CD | Staging (AWS App Runner) |
| `main` | Full CI/CD | Production (AWS App Runner) |

All pipelines enforce a **quality gate via SonarCloud** — a failing quality gate blocks deployment. Docker images are stored in **Amazon ECR** and deployed via **AWS App Runner** using OIDC-based authentication (no long-lived AWS keys).

---

## Branching Strategy

```
main          ──────────────────────────────────────────────▶  (Production)
               ↑                                   ↑
               │  PR (reviewed + QG passed)        │  PR (reviewed + QG passed)
develop       ────────────────────────────────────────────▶  (Staging)
               ↑                         ↑
               │  PR merged              │  PR merged
feature/x     ─────────────────────▶  (CI only)
feature/y                ──────────▶  (CI only)
```

### Branch rules (enforce via GitHub Branch Protection)

| Branch | Required checks | Required approvals | Force-push |
|--------|----------------|-------------------|------------|
| `main` | All CI jobs + Sonar quality gate | 2 | Blocked |
| `develop` | All CI jobs + Sonar quality gate | 1 | Blocked |
| `feature/*` | None (optional) | 0 | Allowed |

---

## Pipeline Architecture

### Visual Overview

```
Push to feature/*  ──▶  [Build] ──▶  [Unit Test] ──┐
                                                     ├──▶  [Sonar]
Push to feature/*  ──▶  [Build] ──▶  [Integ Test] ──┘
(stops here — no deploy)

Push to develop    ──▶  [Build] ──▶  [Unit Test]  ──┐
                   │                                  ├──▶  [Sonar] ──▶  [Docker Build+Push] ──▶  [Deploy Staging]
                   └────▶  [Integ Test]  ─────────────┘

Push to main       ──▶  [Build] ──▶  [Unit Test]  ──┐
                   │                                  ├──▶  [Sonar] ──▶  [Docker Build+Push] ──▶  [Deploy Production]
                   └────▶  [Integ Test]  ─────────────┘                                                │
                                                                                             [Tag :stable in ECR]
```

### Job Parallelism

- `unit-test` and `integration-test` run **in parallel** after `build` completes.
- `sonar` waits for **both** test jobs to complete (uses combined coverage).
- `docker-build-push` waits for `sonar` (quality gate must pass first).
- `deploy` waits for `docker-build-push`.

---

## Workflow Files

| File | Trigger | Purpose |
|------|---------|---------|
| `.github/workflows/ci-cd-main.yml` | Push to `main` | Full CI/CD pipeline → Production |
| `.github/workflows/ci-cd-develop.yml` | Push to `develop` | Full CI/CD pipeline → Staging |
| `.github/workflows/ci-cd-feature.yml` | Push to `feature/*`, PRs to `develop`/`main` | CI-only pipeline, no deploy |
| `.github/workflows/rollback.yml` | Manual `workflow_dispatch` | Rollback production or staging |

---

## Stage Details

### 1. Build

**Runs on:** `ubuntu-latest`  
**Key actions:**
- Checks out the full Git history (`fetch-depth: 0`) for accurate Sonar blame data
- Sets up JDK 21 (Eclipse Temurin) with Maven dependency caching
- Runs `mvn clean package -DskipTests`
- Uploads the built JAR as a GitHub Actions artifact (shared with downstream jobs)
- Produces build metadata: short SHA + environment suffix image tag

**Failure impact:** All downstream jobs are blocked.

---

### 2. Unit Tests

**Runs on:** `ubuntu-latest` (parallel with Integration Tests)  
**Key actions:**
- Runs tests tagged with `@Tag("unit")` via Maven Surefire (`-Dgroups="unit"`)
- Generates JaCoCo coverage XML report
- Uploads Surefire reports and JaCoCo report as artifacts

**Coverage:** JaCoCo report is consumed by the Sonar stage.

---

### 3. Integration Tests

**Runs on:** `ubuntu-latest` (parallel with Unit Tests)  
**Service container:** PostgreSQL 16 (Alpine) — started as a GitHub Actions service  
**Key actions:**
- Downloads the build artifact from the Build stage
- Sets `SPRING_DATASOURCE_*` environment variables pointing to the service container
- Runs tests tagged `@Tag("integration")` via Maven Failsafe (`-Dgroups="integration"`)
- Uploads Failsafe reports as artifacts

**Database lifecycle:** The Postgres container is created fresh per run and torn down automatically.

---

### 4. SonarCloud Analysis

**Runs on:** `ubuntu-latest` (after both test jobs complete)  
**Key actions:**
- Downloads the JaCoCo coverage report from the Unit Test stage
- Runs `mvn sonar:sonar` with `sonar.qualitygate.wait=true`
- For PRs on `feature/*`: uses PR decoration mode (`sonar.pullrequest.*` parameters)
- For `develop`/`main`: uses branch analysis mode

**Blocking behaviour:** If the SonarCloud quality gate fails, this job fails and all downstream jobs (Docker Build, Deploy) are blocked. The pipeline **will not deploy code that fails the quality gate**.

---

### 5. Docker Build & Push to ECR

**Runs on:** `ubuntu-latest`  
**Authentication:** OIDC via `aws-actions/configure-aws-credentials@v4` — no static AWS keys  
**Key actions:**
- Authenticates to ECR using the OIDC role
- Builds the Docker image using `docker buildx` (multi-platform target: `linux/amd64`)
- Injects `BUILD_DATE` and `GIT_COMMIT` as OCI labels
- Pushes two tags:
  - `<sha>-prod` / `<sha>-staging` — immutable, identifies the exact build
  - `latest-prod` / `latest-staging` — mutable, always points to the most recent build

**Dockerfile:** See `Dockerfile` — multi-stage build with non-root runtime user.

---

### 6. Deploy to App Runner

**Runs on:** `ubuntu-latest`  
**Authentication:** Same OIDC role as Docker Build  
**Key actions:**
- Calls `aws apprunner start-deployment` to trigger a new revision
- Polls `aws apprunner describe-service` every 20 seconds until `Status: RUNNING`
- On production only: re-tags the deployed image as `:stable` in ECR (rollback anchor)

**Deployment timeout:** 30 attempts × 20 seconds = maximum 10 minutes before failure.

---

## Environment Configuration

### GitHub Environments

Two GitHub Environments must be created under `Settings → Environments`:

| Environment | Deployment target | Optional: Required reviewers |
|-------------|-------------------|------------------------------|
| `production` | AWS App Runner production service | Recommended: 1 senior engineer |
| `staging` | AWS App Runner staging service | Optional |

### Configuration Files

| File | Purpose |
|------|---------|
| `environments/production.env` | Reference for all production Variables |
| `environments/staging.env` | Reference for all staging Variables |
| `environments/feature.env` | Reference for feature branch Variables |
| `app-runner/production.json` | App Runner service definition for production |
| `app-runner/staging.json` | App Runner service definition for staging |
| `sonar-project.properties` | SonarCloud project configuration |

---

## Secrets & Variables

Full reference: **`docs/secrets-reference.md`**

### Quick Summary

| Name | Type | Scope | Required |
|------|------|-------|---------|
| `AWS_DEPLOY_ROLE_ARN` | Secret | Repository | Yes |
| `SONAR_TOKEN` | Secret | Repository | Yes |
| `TEST_DB_NAME` | Secret | Repository | Yes |
| `TEST_DB_USER` | Secret | Repository | Yes |
| `TEST_DB_PASSWORD` | Secret | Repository | Yes |
| `AWS_REGION` | Variable | Repository | Yes |
| `ECR_REPOSITORY` | Variable | Repository | Yes |
| `SONAR_PROJECT_KEY` | Variable | Repository | Yes |
| `SONAR_ORGANIZATION` | Variable | Repository | Yes |
| `APP_RUNNER_SERVICE_ARN_PROD` | Variable | `production` env | Yes |
| `APP_RUNNER_SERVICE_ARN_STAGING` | Variable | `staging` env | Yes |
| `PRODUCTION_URL` | Variable | `production` env | Yes |
| `STAGING_URL` | Variable | `staging` env | Yes |

---

## Rollback Strategy

Full reference: **`docs/rollback-strategy.md`**

### Summary

| Method | When to use | ETA |
|--------|-------------|-----|
| `rollback.yml` workflow dispatch | Standard rollback — most incidents | ~2–5 min |
| AWS Console direct deploy | GitHub Actions unavailable | ~2–5 min |
| Forward fix (hotfix PR to `main`) | Root cause understood, fix is low-risk | ~10–15 min |

**Rollback anchor:** `:stable` ECR tag is set after every successful production deployment. Rolling back to `:stable` re-deploys the last known-good image without pipeline re-execution.

---

## Infrastructure Requirements

### AWS Resources

| Resource | Notes |
|----------|-------|
| Amazon ECR Repository | Named `my-app` (configurable via `ECR_REPOSITORY` variable) |
| AWS App Runner Service (production) | Configured via `app-runner/production.json` |
| AWS App Runner Service (staging) | Configured via `app-runner/staging.json` |
| IAM OIDC Provider | For `token.actions.githubusercontent.com` — see `docs/secrets-reference.md` |
| IAM Role (deploy) | Assumed by GitHub Actions via OIDC — minimum permissions in `docs/secrets-reference.md` |
| AppRunner ECR Access Role | Allows App Runner to pull from ECR — separate from the deploy role |

### SonarCloud

- Account at [sonarcloud.io](https://sonarcloud.io) with a project linked to this GitHub repository
- Project key and organisation key configured as GitHub Variables
- Quality gate configured in SonarCloud project settings

---

## Setup Checklist

Use this list when setting up the pipeline in a new repository.

### AWS

- [ ] Create ECR repository: `my-app`
- [ ] Create OIDC provider for `token.actions.githubusercontent.com` in IAM
- [ ] Create IAM Role with OIDC trust policy (see `docs/secrets-reference.md`)
- [ ] Attach minimum ECR + App Runner permissions to the IAM Role
- [ ] Create App Runner service for production (use `app-runner/production.json` as reference)
- [ ] Create App Runner service for staging (use `app-runner/staging.json` as reference)
- [ ] Create `AppRunnerECRAccessRole` IAM Role with ECR pull permissions

### GitHub Repository

- [ ] Create `production` Environment under `Settings → Environments`
- [ ] Create `staging` Environment under `Settings → Environments`
- [ ] Add all Repository Secrets (see `docs/secrets-reference.md`)
- [ ] Add all Repository Variables (see `docs/secrets-reference.md`)
- [ ] Add Production environment Variables: `APP_RUNNER_SERVICE_ARN_PROD`, `PRODUCTION_URL`
- [ ] Add Staging environment Variables: `APP_RUNNER_SERVICE_ARN_STAGING`, `STAGING_URL`
- [ ] Enable Branch Protection on `main`: require 2 approvals + all CI checks
- [ ] Enable Branch Protection on `develop`: require 1 approval + all CI checks

### SonarCloud

- [ ] Link the GitHub repository to SonarCloud
- [ ] Note the project key and organisation key
- [ ] Generate a project analysis token
- [ ] Add `SONAR_TOKEN` as a GitHub Repository Secret
- [ ] Set `SONAR_PROJECT_KEY` and `SONAR_ORGANIZATION` as GitHub Variables
- [ ] Configure the Quality Gate in SonarCloud (recommended: 80% coverage, 0 critical issues)

### Verify

- [ ] Push a `feature/*` branch — confirm CI passes and no deployment occurs
- [ ] Open a PR to `develop` — confirm SonarCloud PR decoration appears
- [ ] Merge to `develop` — confirm staging deployment completes
- [ ] Merge to `main` — confirm production deployment completes and `:stable` tag is set
- [ ] Run `rollback.yml` against staging with a test reason — confirm rollback works
