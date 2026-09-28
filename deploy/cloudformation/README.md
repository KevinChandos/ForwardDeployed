# BookWorm API — CloudFormation Templates

Production-ready AWS CloudFormation templates for deploying the `bookworm-api` Spring Boot application on AWS App Runner with RDS PostgreSQL, Secrets Manager, ECR, IAM, and CloudWatch.

> **Region:** `ap-southeast-2` (Sydney) — change `AwsRegion` parameters if deploying elsewhere.

---

## Directory Structure

```
deploy/cloudformation/
├── ecr.yaml              # Amazon ECR private repository
├── rds.yaml              # RDS PostgreSQL 16 primary + read replica
├── secrets.yaml          # AWS Secrets Manager (all runtime secrets)
├── iam.yaml              # IAM roles: App Runner instance, access, CI/CD
├── cloudwatch.yaml       # CloudWatch log groups, alarms, dashboard
├── apprunner.yaml        # App Runner service, VPC connector, security groups
└── parameters/
    ├── dev/              # Parameter files for the dev environment
    │   ├── ecr.json
    │   ├── rds.json
    │   ├── secrets.json
    │   ├── iam.json
    │   ├── cloudwatch.json
    │   └── apprunner.json
    ├── staging/          # Parameter files for the staging environment
    └── prod/             # Parameter files for the production environment
```

---

## Stack Deployment Order

The stacks have implicit dependencies via exported outputs.  Deploy in this exact sequence:

| # | Template | Stack Name Pattern | Key Exports |
|---|----------|--------------------|-------------|
| 1 | `ecr.yaml` | `bookworm-ecr-<env>` | `RepositoryUri`, `RepositoryArn` |
| 2 | `iam.yaml` | `bookworm-iam-<env>` | `AppRunnerInstanceRoleArn`, `AppRunnerAccessRoleArn`, `CICDRoleArn` |
| 3 | `rds.yaml` | `bookworm-rds-<env>` | `DBPrimaryEndpoint`, `DBReplicaEndpoint`, `RDSSecurityGroupId` |
| 4 | `secrets.yaml` | `bookworm-secrets-<env>` | All secret ARNs |
| 5 | `cloudwatch.yaml` | `bookworm-cloudwatch-<env>` | Log group names, SNS topic ARNs |
| 6 | `apprunner.yaml` | `bookworm-apprunner-<env>` | `ServiceUrl`, `ServiceArn` |

After deploying step 6, update `parameters/<env>/iam.json` with the `AppRunnerServiceArn` output and run a `update-stack` on `bookworm-iam-<env>` to tighten the CI/CD role's `UpdateService` resource ARN.

---

## Prerequisites

1. **VPC with private subnets** — two private subnets in different AZs are required for RDS Multi-AZ and the VPC connector.  Provide their IDs in each parameter file.
2. **Customer-managed KMS key** — create a KMS key (`bookworm-<env>-secrets-cmk`) in the target account and supply its ARN to `secrets.yaml` and `cloudwatch.yaml` parameter files.
3. **Secrets Manager rotation Lambda** — for production, deploy the AWS-managed `SecretsManagerRDSPostgreSQLRotationSingleUser` Lambda and supply its ARN in `parameters/prod/secrets.json`.
4. **AWS CLI / credentials** — ensure the deploying principal has `cloudformation:*`, `iam:*`, `rds:*`, `ecr:*`, `apprunner:*`, `secretsmanager:*`, and `logs:*` on the target account.
5. **Interface VPC Endpoints** — the templates assume VPC endpoints for `secretsmanager`, `xray`, `logs`, `ecr.api`, and `ecr.dkr` already exist in the VPC.  The App Runner egress SG allows TCP 443 to `0.0.0.0/0` to reach them.

---

## Deploying with the AWS CLI

Replace `<ENV>` with `dev`, `staging`, or `prod`.

```bash
ENV=prod
REGION=ap-southeast-2
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# 1 — ECR
aws cloudformation deploy \
  --template-file deploy/cloudformation/ecr.yaml \
  --stack-name bookworm-ecr-${ENV} \
  --parameter-overrides file://deploy/cloudformation/parameters/${ENV}/ecr.json \
  --region ${REGION} \
  --tags Application=bookworm-api Environment=${ENV}

# 2 — IAM  (requires --capabilities because it creates named IAM roles)
aws cloudformation deploy \
  --template-file deploy/cloudformation/iam.yaml \
  --stack-name bookworm-iam-${ENV} \
  --parameter-overrides file://deploy/cloudformation/parameters/${ENV}/iam.json \
  --capabilities CAPABILITY_NAMED_IAM \
  --region ${REGION} \
  --tags Application=bookworm-api Environment=${ENV}

# 3 — RDS
aws cloudformation deploy \
  --template-file deploy/cloudformation/rds.yaml \
  --stack-name bookworm-rds-${ENV} \
  --parameter-overrides file://deploy/cloudformation/parameters/${ENV}/rds.json \
  --region ${REGION} \
  --tags Application=bookworm-api Environment=${ENV}

# 4 — Secrets Manager
aws cloudformation deploy \
  --template-file deploy/cloudformation/secrets.yaml \
  --stack-name bookworm-secrets-${ENV} \
  --parameter-overrides file://deploy/cloudformation/parameters/${ENV}/secrets.json \
  --region ${REGION} \
  --tags Application=bookworm-api Environment=${ENV}

# 5 — CloudWatch
aws cloudformation deploy \
  --template-file deploy/cloudformation/cloudwatch.yaml \
  --stack-name bookworm-cloudwatch-${ENV} \
  --parameter-overrides file://deploy/cloudformation/parameters/${ENV}/cloudwatch.json \
  --region ${REGION} \
  --tags Application=bookworm-api Environment=${ENV}

# 6 — App Runner
aws cloudformation deploy \
  --template-file deploy/cloudformation/apprunner.yaml \
  --stack-name bookworm-apprunner-${ENV} \
  --parameter-overrides file://deploy/cloudformation/parameters/${ENV}/apprunner.json \
  --region ${REGION} \
  --tags Application=bookworm-api Environment=${ENV}
```

---

## Tearing Down (Non-Production Only)

```bash
# Reverse order; skipping prod to prevent accidental data loss
ENV=dev
REGION=ap-southeast-2

for STACK in apprunner cloudwatch secrets rds iam ecr; do
  aws cloudformation delete-stack \
    --stack-name bookworm-${STACK}-${ENV} \
    --region ${REGION}
  aws cloudformation wait stack-delete-complete \
    --stack-name bookworm-${STACK}-${ENV} \
    --region ${REGION}
done
```

> **Warning:** `rds.yaml` and `secrets.yaml` resources have `DeletionPolicy: Retain` and `DeletionPolicy: Snapshot`.  Deleting the stack does **not** delete the database or secrets — you must remove them manually to avoid ongoing charges.

---

## Environment Differences

| Setting | dev | staging | prod |
|---------|-----|---------|------|
| RDS instance | `db.t4g.micro` | `db.t4g.small` | `db.t4g.medium` |
| RDS Multi-AZ | ❌ | ❌ | ✅ |
| Read replica | ❌ | ❌ | ✅ |
| Deletion protection | ❌ | ❌ | ✅ |
| App Runner min instances | `0` (paused) | `0` (paused) | `1` |
| App Runner CPU/Memory | `0.25 vCPU / 0.5 GB` | `0.5 vCPU / 1 GB` | `1 vCPU / 2 GB` |
| Backup retention | `1 day` | `3 days` | `7 days` |
| App log retention | `7 days` | `14 days` | `30 days` |
| X-Ray enabled | ❌ | ✅ | ✅ |
| PagerDuty alarm | ❌ | ❌ | ✅ |
| KMS encryption | ❌ (AWS-managed) | ❌ (AWS-managed) | ✅ (CMK) |

---

## Security Notes

- **No secrets in parameter files committed to VCS.** All `REPLACE_WITH_SECURE_VALUE` and `REPLACE_ME` placeholders must be substituted at deploy time using AWS SSM Parameter Store, a secrets manager integration, or CI/CD environment variables — never stored in plaintext in source control.
- **Least-privilege IAM.** The App Runner instance role grants only `GetSecretValue` on `bookworm/<env>/*`, no wildcard resource principals.
- **KMS encryption.** In production, all secrets and log groups use a customer-managed KMS key.  Create the key separately (`kms:KeyAdministratorRole` → your ops team) and supply its ARN before deploying stacks 4 and 5.
- **No public RDS access.** `PubliclyAccessible: false` is hardcoded on all RDS instances.  The only network path to PostgreSQL is from the App Runner VPC connector security group.
