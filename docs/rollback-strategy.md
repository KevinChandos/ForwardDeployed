# ============================================================
# docs/rollback-strategy.md
# Purpose: Documents the complete rollback strategy for
#          production and staging environments.
# Usage: Reference during incidents. Follow procedures in order.
# Side effect: Executing a rollback re-deploys a prior image and
#              resets the :stable tag. Document every rollback
#              in the incident log.
# ============================================================

# Rollback Strategy

This document defines the rollback procedures for the CI/CD pipeline targeting AWS App Runner with images stored in Amazon ECR.

---

## Overview

The pipeline uses an **image-based rollback strategy**. Every successful production deployment tags the deployed ECR image as `:stable`. A rollback re-deploys that image (or any explicit tag) to App Runner without re-running the full CI pipeline.

| Trigger | Who | Mechanism |
|---------|-----|-----------|
| Production incident | On-call engineer | GitHub Actions `rollback.yml` — manual `workflow_dispatch` |
| Staging regression | Developer / QA | GitHub Actions `rollback.yml` — manual `workflow_dispatch` |
| Emergency (AWS Console) | Platform team | Direct App Runner redeployment via AWS Console |

---

## ECR Image Tag Conventions

| Tag | When set | Meaning |
|-----|----------|---------|
| `<sha>-prod` | Every push to `main` | Immutable; identifies exact build |
| `<sha>-staging` | Every push to `develop` | Immutable; identifies exact build |
| `latest-prod` | Every push to `main` | Mutable; most recent production build |
| `latest-staging` | Every push to `develop` | Mutable; most recent staging build |
| `stable` | After successful production deployment | Last known-good production image |

> **Key rule:** `:stable` is only moved forward after a deployment is confirmed `RUNNING`. It is the primary rollback anchor for production.

---

## Rollback Decision Tree

```
Incident detected
      │
      ▼
Is it a code/application defect? ──No──▶ Check infrastructure (App Runner / RDS / VPC)
      │
     Yes
      │
      ▼
Is :stable tag healthy (last deploy was good)?
      │
    ──┴──────────────┐
   Yes               No
    │                │
    ▼                ▼
Use :stable     Identify last known-good <sha>-prod tag from ECR console
tag                  │
    │                ▼
    └────────────▶ Run rollback.yml workflow_dispatch
                       (set rollback_target to specific tag if not using :stable)
```

---

## Procedure 1 — Rollback via GitHub Actions (Preferred)

This is the standard procedure. It is auditable, requires MFA, and logs the reason.

### Steps

1. Go to `Actions → Rollback — Manual` in the GitHub repository.
2. Click **Run workflow**.
3. Fill in the inputs:
   - **environment**: `production` or `staging`
   - **rollback_target**: Leave blank to roll back to `:stable` (production) or `:latest-staging` (staging). Provide a specific `<sha>-prod` tag to roll back to a specific build.
   - **reason**: Required — enter a short description (e.g. `NullPointerException in /api/orders after deploy abc1234`).
4. Click **Run workflow**.
5. Monitor the job in the Actions tab.
6. Verify the service URL after the rollback completes.

### Expected duration

| Phase | Typical Duration |
|-------|-----------------|
| Image validation in ECR | ~10 seconds |
| App Runner update-service call | ~5 seconds |
| App Runner convergence to RUNNING | 1–4 minutes |
| Total | **~2–5 minutes** |

---

## Procedure 2 — Emergency Rollback via AWS Console

Use this procedure only when GitHub Actions is unavailable or the OIDC authentication is broken.

### Steps

1. Open the **AWS App Runner Console** → select the affected service.
2. Go to the **Deployments** tab.
3. Click **Deploy** and update the image URI to a known-good tag from ECR.
4. Monitor the deployment status until `RUNNING`.
5. Update the `:stable` ECR tag manually:
   ```bash
   # Get the manifest of the known-good image
   MANIFEST=$(aws ecr batch-get-image \
     --repository-name my-app \
     --image-ids imageTag=<known-good-sha>-prod \
     --query 'images[0].imageManifest' \
     --output text)

   # Re-tag it as :stable
   aws ecr put-image \
     --repository-name my-app \
     --image-tag stable \
     --image-manifest "$MANIFEST"
   ```
6. Document the rollback in the incident log.

---

## Procedure 3 — Forward Fix (Preferred over repeated rollback)

If the root cause is understood and a fix is low-risk:

1. Push a hotfix to a `feature/hotfix-<issue>` branch.
2. Open a PR directly to `main` (skip `develop` for critical production fixes).
3. Require at least 1 approver with CODEOWNERS enforcement.
4. Merge triggers the full `ci-cd-main.yml` pipeline.
5. If the hotfix pipeline passes quality gate and deploys, close the incident.

> **When to forward-fix vs. roll back:** Roll back first to restore service. Forward-fix in parallel to address root cause. Never stay on a rolled-back version longer than one business day without a forward fix plan.

---

## Post-Rollback Checklist

- [ ] Service URL is responding correctly
- [ ] Health check endpoint `/actuator/health` returns `200 OK`
- [ ] Error rate has returned to baseline (check CloudWatch)
- [ ] `:stable` ECR tag verified to point to the rolled-back image
- [ ] Incident log updated with: timestamp, trigger, image tag deployed, engineer who executed rollback
- [ ] Root cause investigation opened (Jira / GitHub Issue)
- [ ] Forward fix scheduled or PR opened

---

## Rollback Audit Trail

Every rollback performed via `rollback.yml` is automatically recorded in the GitHub Actions run history with:
- The GitHub username who triggered the workflow
- The environment targeted
- The image tag deployed
- The reason provided
- The exact timestamp

For rollbacks performed via the AWS Console, record the details manually in your incident tracking system.
