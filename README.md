# Spring Cloud Gateway — Resilience, Security & Observability

Kubernetes routes your traffic. But who authenticates it? Who stops a single failing service from cascading into a system-wide outage? Who enforces rate limits across every replica without inconsistency? While Kubernetes handles infrastructure-level routing and load balancing, it stops at the network layer. **Spring Cloud Gateway**, built on Project Reactor, Spring WebFlux, and Spring Boot, picks up where Kubernetes leaves off — giving you a single enforced entry point at the application layer to handle authentication, rate limiting, circuit breaking, retries, fallbacks, and observability — so your downstream services never have to.

---

## Stack

| Concern | Technology |
|---|---|
| Gateway | Spring Cloud Gateway (WebFlux / Reactor) |
| Security | Spring Security · OAuth2 · OpenID Connect · JWT · Keycloak 24 |
| Resilience | Resilience4J — Circuit Breaker · Retry · Fallback |
| Rate Limiting | Redis Token Bucket (per-user, cluster-safe) |
| Observability | OTel Java Agent · OTel Collector · Loki · Tempo · Prometheus · Grafana |
| Infrastructure | Kubernetes (Rancher Desktop) |
| Runtime | Java 21 · Spring Boot 3.x · Maven |

---

## Getting Started

### Prerequisites
- Rancher Desktop with Kubernetes enabled
- `kubectl` pointing to `rancher-desktop` context
- Docker + Maven

### Deploy

```bash
# Step 1 — Build Docker images
make k8s-build

# Step 2 — Deploy everything to Kubernetes
make k8s-deploy
```

### Endpoints

| Service | URL | Credentials |
|---|---|---|
| API Gateway | http://localhost:30080 | — |
| Keycloak | http://localhost:30180 | admin / admin |
| Grafana | http://localhost:30001 | admin / admin |

### Test Users

| Username | Password | Role |
|---|---|---|
| regular-user | user123 | USER |
| admin-user | admin123 | USER, ADMIN |

---

## Testing

### Get a Token
```bash
make token
```

### Authentication — `401`
```bash
# No token — blocked at gateway, never reaches downstream
curl -i http://localhost:30080/booking/bookings/123
```

### Authenticated Request — `201`
```bash
curl -i -X POST http://localhost:30080/booking/bookings \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"event-123\",\"userName\":\"Demo User\"}"

### Rate Limiting — `429`
```bash
# Fire 10 requests rapidly — burst exhausted after 3, rest get 429
for /L %i in (1,1,10) do curl -s -o NUL -w "%{http_code}\n" ^
  -X POST http://localhost:30080/booking/bookings ^
  -H "Authorization: Bearer <token>" ^
  -H "Content-Type: application/json" ^
  -d "{\"eventId\":\"event-123\",\"userName\":\"Demo User\"}"
```

### Circuit Breaker — `503`
```bash
# Simulate outage
kubectl scale deployment/booking-service --replicas=0 -n gateway-demo

# Gateway returns structured fallback immediately
curl -i -X POST http://localhost:30080/booking/bookings \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"event-123\",\"userName\":\"Demo User\"}"

# Restore
kubectl scale deployment/booking-service --replicas=1 -n gateway-demo
```