# ============================================================
# docs/secrets-reference.md
# Purpose: Authoritative reference for all GitHub Actions
#          Secrets and Variables used across all pipelines.
# Usage: Use this as your setup checklist when onboarding a
#        new repository or rotating credentials.
# Side effect: Out-of-date secrets will cause pipeline failures
#              — rotate before expiry and update this document.
# ============================================================

# Secrets & Variables Reference

This document enumerates every **GitHub Actions Secret** and **GitHub Actions Variable** required by the CI/CD pipelines in this repository. Secrets hold sensitive values and are never logged; Variables hold non-sensitive configuration and are visible in workflow logs.

---

## How to configure

| Type | Location in GitHub UI |
|------|-----------------------|
| Repository Secret | `Settings → Secrets and variables → Actions → Secrets → New repository secret` |
| Repository Variable | `Settings → Secrets and variables → Actions → Variables → New repository variable` |
| Environment Secret | `Settings → Environments → <env> → Add secret` |
| Environment Variable | `Settings → Environments → <env> → Add variable` |

---

## Secrets

Secrets are encrypted at rest and masked in all log output. Never print them with `echo`.

### Repository-Level Secrets
_(Available to all branches and environments)_

| Secret Name | Description | How to obtain |
|-------------|-------------|---------------|
| `AWS_DEPLOY_ROLE_ARN` | ARN of the IAM Role that GitHub Actions assumes via OIDC to authenticate with AWS. Must have permissions to push to ECR and deploy to App Runner. | Create an IAM Role with a trust policy for `token.actions.githubusercontent.com`. See `iam/github-oidc-role.json`. |
| `SONAR_TOKEN` | SonarCloud project analysis token. Used for authenticated Sonar scans and PR decoration. | SonarCloud → My Account → Security → Generate Token |
| `TEST_DB_NAME` | Database name used by the integration-test Postgres service container. | Choose a value (e.g. `testdb`) — consistent across all branches. |
| `TEST_DB_USER` | Username for the integration-test Postgres service container. | Choose a value (e.g. `testuser`). |
| `TEST_DB_PASSWORD` | Password for the integration-test Postgres service container. | Generate a random value (e.g. `openssl rand -base64 24`). Rotate quarterly. |

### Environment Secrets
_(Scoped to a specific GitHub Environment — production or staging)_

| Secret Name | Environment | Description |
|-------------|-------------|-------------|
| _(none additional)_ | production / staging | All sensitive deployment credentials are inherited from repository-level secrets. Environment-specific secrets may be added here if the production IAM role differs from staging. |

> **Note:** If you run separate IAM roles per environment, override `AWS_DEPLOY_ROLE_ARN` at the Environment level.

---

## Variables (Non-Secret Configuration)

Variables are visible in workflow logs and safe to store in plaintext.

### Repository-Level Variables
_(Available to all branches)_

| Variable Name | Example Value | Description |
|---------------|---------------|-------------|
| `AWS_REGION` | `ap-southeast-1` | AWS region for all ECR and App Runner operations. |
| `ECR_REPOSITORY` | `my-app` | Name (not URI) of the ECR repository. |
| `SONAR_PROJECT_KEY` | `my-org_my-app` | SonarCloud project key. Found in the SonarCloud project settings page. |
| `SONAR_ORGANIZATION` | `my-org` | SonarCloud organization key. |

### Production Environment Variables

| Variable Name | Example Value | Description |
|---------------|---------------|-------------|
| `APP_RUNNER_SERVICE_ARN_PROD` | `arn:aws:apprunner:...:service/my-app-prod/xxx` | Full ARN of the production App Runner service. Found in the AWS Console under App Runner. |
| `PRODUCTION_URL` | `https://xxx.ap-southeast-1.awsapprunner.com` | Public URL of the production service. Used as the environment URL in GitHub deployments UI. |

### Staging Environment Variables

| Variable Name | Example Value | Description |
|---------------|---------------|-------------|
| `APP_RUNNER_SERVICE_ARN_STAGING` | `arn:aws:apprunner:...:service/my-app-staging/yyy` | Full ARN of the staging App Runner service. |
| `STAGING_URL` | `https://yyy.ap-southeast-1.awsapprunner.com` | Public URL of the staging service. |

---

## IAM Policy Requirements

The IAM Role assumed via OIDC (`AWS_DEPLOY_ROLE_ARN`) must have the following minimum permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ECRAuth",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECRPush",
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:DescribeImages",
        "ecr:GetDownloadUrlForLayer",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart",
        "ecr:BatchGetImage"
      ],
      "Resource": "arn:aws:ecr:<region>:<account-id>:repository/my-app"
    },
    {
      "Sid": "AppRunnerDeploy",
      "Effect": "Allow",
      "Action": [
        "apprunner:StartDeployment",
        "apprunner:DescribeService",
        "apprunner:UpdateService"
      ],
      "Resource": [
        "arn:aws:apprunner:<region>:<account-id>:service/my-app-prod/*",
        "arn:aws:apprunner:<region>:<account-id>:service/my-app-staging/*"
      ]
    }
  ]
}
```

---

## Secret Rotation Schedule

| Secret | Rotation Frequency | Owner |
|--------|--------------------|-------|
| `AWS_DEPLOY_ROLE_ARN` | No rotation needed (OIDC — no long-lived key) | Platform team |
| `SONAR_TOKEN` | Annually or on personnel change | DevOps |
| `TEST_DB_PASSWORD` | Quarterly | DevOps |

---

## GitHub OIDC Trust Policy

The IAM Role must trust `token.actions.githubusercontent.com`. Example trust policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<account-id>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:<github-org>/<repo-name>:*"
        }
      }
    }
  ]
}
```

Replace `<account-id>`, `<github-org>`, and `<repo-name>` with your actual values.
