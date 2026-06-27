# ─────────────────────────────────────────────────────────────────────────────
# Spring Cloud Gateway demo — Makefile
#
# make k8s-deploy      Deploy to Kubernetes
# make k8s-delete      Remove all resources from Kubernetes
# make k8s-status      Show pod/service status
# make token           Get a test JWT from Keycloak
# make help            Show all commands
# ─────────────────────────────────────────────────────────────────────────────

.PHONY: help \
        k8s-build k8s-deploy k8s-delete k8s-status \
        k8s-logs-edge-service k8s-logs-booking k8s-logs-keycloak \
        k8s-logs-prometheus k8s-logs-grafana k8s-logs-redis \
        k8s-logs-loki k8s-logs-tempo k8s-logs-otel-collector \
        token token-admin



GREEN  := \033[0;32m
YELLOW := \033[0;33m
BLUE   := \033[0;34m
RESET  := \033[0m

# ── Help ──────────────────────────────────────────────────────────────────────

help: ## Show all available commands
	@echo ""
	@echo "Spring Cloud Gateway Demo — Kubernetes"
	@echo "======================================="
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-35s$(RESET) %s\n", $$1, $$2}'
	@echo ""

# ── Kubernetes ────────────────────────────────────────────────────────────────

k8s-build: ## Build Docker images
	@echo "$(BLUE)Building Docker images...$(RESET)"
	docker build -f edge-service/Dockerfile -t edge-service:latest .
	docker build -f booking-service/Dockerfile -t booking-service:latest .
	@echo "$(GREEN)✅ Images built$(RESET)"

k8s-deploy: k8s-build ## Build images and deploy all resources to Kubernetes
	@echo "$(BLUE)Creating namespace...$(RESET)"
	kubectl apply -f k8s/namespace/namespace.yaml

	@echo "$(BLUE)Applying secrets...$(RESET)"
	kubectl apply -f k8s/secrets/app-secrets.yaml

	@echo "$(BLUE)Applying configmaps...$(RESET)"
	kubectl apply -f k8s/configmaps/

	@echo "$(BLUE)Deploying infrastructure (redis, keycloak, loki, tempo, prometheus)...$(RESET)"
	kubectl apply -f k8s/deployments/redis.yaml
	kubectl apply -f k8s/deployments/keycloak.yaml
	kubectl apply -f k8s/deployments/loki.yaml
	kubectl apply -f k8s/deployments/tempo.yaml
	kubectl apply -f k8s/deployments/prometheus.yaml

	@echo "$(BLUE)Waiting for infrastructure to be ready...$(RESET)"
	kubectl wait --for=condition=ready pod -l app=redis -n gateway-demo --timeout=180s
	kubectl wait --for=condition=ready pod -l app=loki -n gateway-demo --timeout=180s
	kubectl wait --for=condition=ready pod -l app=tempo -n gateway-demo --timeout=180s
	kubectl wait --for=condition=ready pod -l app=prometheus -n gateway-demo --timeout=180s

	@echo "$(BLUE)Deploying observability (grafana, otel-collector)...$(RESET)"
	kubectl apply -f k8s/deployments/grafana.yaml
	kubectl apply -f k8s/deployments/otel-collector.yaml

	@echo "$(BLUE)Waiting for otel-collector to be ready...$(RESET)"
	kubectl wait --for=condition=ready pod -l app=otel-collector -n gateway-demo --timeout=180s

	@echo "$(BLUE)Deploying applications...$(RESET)"
	kubectl apply -f k8s/deployments/booking-service.yaml
	kubectl apply -f k8s/deployments/edge-service.yaml

	@echo ""
	@echo "$(GREEN)✅ Deployment complete!$(RESET)"
	@echo ""
	@echo "  Edge Service  → http://localhost:30080"
	@echo "  Grafana       → http://localhost:30001"
	@echo "  Keycloak      → http://localhost:30180"
	@echo ""

k8s-delete: ## Remove all resources from Kubernetes
	@echo "$(YELLOW)Deleting namespace gateway-demo...$(RESET)"
	kubectl delete namespace gateway-demo --ignore-not-found=true
	@echo "$(GREEN)✅ Teardown complete$(RESET)"

k8s-status: ## Show Kubernetes pod/service status
	kubectl get pods,services -n gateway-demo

k8s-restart-edge: ## Restart edge-service deployment
	kubectl rollout restart deployment/edge-service -n gateway-demo

k8s-restart-booking: ## Restart booking-service deployment
	kubectl rollout restart deployment/booking-service -n gateway-demo

k8s-restart-all: ## Restart all application deployments
	kubectl rollout restart deployment/edge-service deployment/booking-service -n gateway-demo

# ── Logs ──────────────────────────────────────────────────────────────────────

k8s-logs-edge-service: ## Tail edge-service pod logs
	kubectl logs -f deployment/edge-service -n gateway-demo

k8s-logs-booking: ## Tail booking-service pod logs
	kubectl logs -f deployment/booking-service -n gateway-demo

k8s-logs-keycloak: ## Tail keycloak pod logs
	kubectl logs -f deployment/keycloak -n gateway-demo

k8s-logs-prometheus: ## Tail prometheus pod logs
	kubectl logs -f deployment/prometheus -n gateway-demo

k8s-logs-grafana: ## Tail grafana pod logs
	kubectl logs -f deployment/grafana -n gateway-demo

k8s-logs-redis: ## Tail redis pod logs
	kubectl logs -f deployment/redis -n gateway-demo

k8s-logs-loki: ## Tail loki pod logs
	kubectl logs -f deployment/loki -n gateway-demo

k8s-logs-tempo: ## Tail tempo pod logs
	kubectl logs -f deployment/tempo -n gateway-demo

k8s-logs-otel-collector: ## Tail otel-collector pod logs
	kubectl logs -f deployment/otel-collector -n gateway-demo

# ── Tokens ────────────────────────────────────────────────────────────────────

token: ## Get a test token (regular-user / user123)
	@curl -s -X POST \
		http://localhost:30180/realms/demo-realm/protocol/openid-connect/token \
		-H "Content-Type: application/x-www-form-urlencoded" \
		-d "client_id=gateway-client" \
		-d "grant_type=password" \
		-d "username=regular-user" \
		-d "password=user123" \
		-d "scope=openid"

token-admin: ## Get a test token (admin-user / admin123)
	@curl -s -X POST \
		http://localhost:30180/realms/demo-realm/protocol/openid-connect/token \
		-H "Content-Type: application/x-www-form-urlencoded" \
		-d "client_id=gateway-client" \
		-d "grant_type=password" \
		-d "username=admin-user" \
		-d "password=admin123" \
		-d "scope=openid" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])"

# ── Test requests ─────────────────────────────────────────────────────────────

test-post: ## Test POST /booking/bookings (requires TOKEN env var)
	@curl -i -X POST http://localhost:30080/booking/bookings \
		-H "Authorization: Bearer $(TOKEN)" \
		-H "Content-Type: application/json" \
		-d '{"eventId":"event-123","userName":"Demo User"}'

test-get: ## Test GET /booking/bookings/123 (requires TOKEN env var)
	@curl -i -X GET http://localhost:30080/booking/bookings/123 \
		-H "Authorization: Bearer $(TOKEN)"

test-health: ## Test edge-service health
	@curl -s http://localhost:30080/actuator/health | python3 -m json.tool