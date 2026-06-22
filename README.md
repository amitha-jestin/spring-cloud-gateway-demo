# Spring Cloud Gateway Demo

A production-grade Spring Cloud Gateway demo demonstrating how to address cross-cutting concerns — security, resilience, and observability — at the gateway layer for any microservice architecture.

---

## Architecture

```
Client
  │
  │ Bearer JWT (issued by any OIDC provider)
  ▼
┌─────────────────────────────────────────────────┐
│                  Edge Service (8080)              │
│                                                  │
│  Security                                       │
│   • OAuth2 Resource Server — offline JWT via    │
│     JWKS (works with Keycloak/Cognito/Entra ID) │
│   • Token stripped after validation             │
│   • Verified claims → trusted headers           │
│   • Redis rate limiter — per user identity      │
│                                                  │
│  Resilience (Resilience4J)                      │
│   • Circuit breaker per route                   │
│   • Slow calls (>3s) counted as failures        │
│   • Retry — GET routes only                     │
│   • Structured fallback responses               │
│                                                  │
│  Observability                                  │
│   • Micrometer → Prometheus                     │
│   • OpenTelemetry → Tempo                       │
│   • Structured logs → Loki                      │
│   • Separate audit log stream                   │
└───────────────────┬─────────────────────────────┘
                    │
                    ┼
                    ▼           
            ┌───────────────┐
            │Booking Service│ 
            │  (port 9001)  │ 
            └───────────────┘ 

┌─────────────────────────────────────────────────┐
│         Infrastructure (Docker Compose)         │
│  Keycloak :8180 → Redis :6379                   │
│  Prometheus :9090 → Grafana :3001                │
│  Loki :3100 → Tempo :3200                       │
└─────────────────────────────────────────────────┘
```

---

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| Java | 21+ | `java -version` |
| Docker Desktop | Latest | `docker --version` |
| make | Any | `make --version` |
| kubectl | Any | `kubectl version` (optional — for K8s deploy) |
| minikube | Any | `minikube version` (optional) |

No Maven install required — `mvnw` downloads it automatically.

---

## Quick Start

```bash
make up            # start Keycloak, Redis, Prometheus, Grafana, Loki, Tempo
make build         # build gateway + booking-service
make run-edge   # terminal 1 — port 8080
make run-booking   # terminal 2 — port 9001
```

Wait ~60 seconds after `make up` for Keycloak to finish importing the realm.

---


