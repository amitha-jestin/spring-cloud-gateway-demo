# ─────────────────────────────────────────────────────────────────────────────
# Spring Cloud Gateway demo — Makefile
#
# make up              Start infra (Keycloak, Redis, Prometheus, Grafana, Loki, Tempo)
# make build           Build all Java modules
# make run-edge        Run edge-service
# make run-booking     Run Booking Service
# make test            Run tests
# make clean           Clean builds and stop infra
# make k8s-deploy      Deploy to minikube
# make token           Get a test JWT from Keycloak
# make help            Show all commands
# ─────────────────────────────────────────────────────────────────────────────

.PHONY: help up down build run-gateway run-booking test clean \
        logs logs-keycloak status \
        k8s-build k8s-deploy k8s-delete k8s-status \
        open-grafana open-keycloak open-prometheus token token-admin

ifeq ($(OS),Windows_NT)
  OPEN := start
  MVN  := mvnw.cmd
else
  UNAME := $(shell uname -s)
  ifeq ($(UNAME),Darwin)
    OPEN := open
  else
    OPEN := xdg-open
  endif
  MVN := ./mvnw
endif

GREEN  := \033[0;32m
YELLOW := \033[0;33m
BLUE   := \033[0;34m
RESET  := \033[0m

help: ## Show this help message
	@echo ""
	@echo "$(BLUE)Spring Cloud Gateway Demo — Commands$(RESET)"
	@echo "──────────────────────────────────────────────────"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-18s$(RESET) %s\n", $$1, $$2}'
	@echo ""

# ── Infrastructure ────────────────────────────────────────────────────────────

up: ## Start all infrastructure (Keycloak, Redis, Prometheus, Grafana, Loki, Tempo)
	@echo "$(BLUE)Starting infrastructure...$(RESET)"
	docker compose up -d
	@echo "$(GREEN)Started.$(RESET)"
	@echo "  Keycloak Admin : http://localhost:8180  (admin / admin)"
	@echo "  Grafana        : http://localhost:3001  (admin / admin)"
	@echo "  Prometheus     : http://localhost:9090"
	@echo "$(YELLOW)Wait ~60s for Keycloak to import the realm before starting services.$(RESET)"

down: ## Stop all infrastructure
	docker compose down

down-volumes: ## Stop infra and delete all data volumes
	docker compose down -v

status: ## Show running containers
	docker compose ps

logs: ## Tail all infra logs
	docker compose logs -f

logs-keycloak: ## Tail Keycloak logs
	docker compose logs -f keycloak

# ── Java Build ────────────────────────────────────────────────────────────────

build: ## Build all Java modules
	$(MVN) clean package -DskipTests

test: ## Run all tests
	$(MVN) test

clean-java: ## Clean Java build artifacts
	$(MVN) clean

# ── Run Services ─────────────────────────────────────────────────────────────

run-edge: ## Run Edge Service (port 8080)
	@echo "$(BLUE)Starting Edge Service on port 8080...$(RESET)"
	java -jar edge-service/target/edge-service-1.0.0.jar

run-booking: ## Run Booking Service (port 9001)
	@echo "$(BLUE)Starting Booking Service on port 9001...$(RESET)"
	java -jar booking-service/target/booking-service-1.0.0.jar

# ── Kubernetes ────────────────────────────────────────────────────────────────

k8s-build: build ## Build Docker images and load into minikube
	docker build -f edge-service/Dockerfile -t amithapoulose/edge-service:latest .
	docker build -f booking-service/Dockerfile -t amithapoulose/booking-service:latest .
	minikube image load amithapoulose/edge-service:latest
	minikube image load amithapoulose/booking-service:latest
	@echo "$(GREEN)Images loaded into minikube.$(RESET)"

k8s-deploy: ## Deploy all services to minikube
	kubectl apply -f k8s/namespace.yml
	kubectl apply -f k8s/edge-service/
	kubectl apply -f k8s/booking-service/
	kubectl get pods -n gateway-demo

k8s-status: ## Show Kubernetes pod/service/HPA status
	kubectl get pods,services,hpa -n gateway-demo

k8s-delete: ## Remove all resources from Kubernetes
	kubectl delete namespace gateway-demo --ignore-not-found=true

k8s-logs-gateway: ## Tail gateway pod logs
	kubectl logs -f -l app=edge-service -n gateway-demo

k8s-logs-booking: ## Tail booking-service pod logs
	kubectl logs -f -l app=booking-service -n gateway-demo

k8s-ingress: ## Enable minikube ingress addon
	minikube addons enable ingress
	@echo "$(YELLOW)Add to /etc/hosts: $$(minikube ip) gateway.local$(RESET)"

# ── Browser shortcuts ─────────────────────────────────────────────────────────

open-grafana: ## Open Grafana
	$(OPEN) http://localhost:3001

open-keycloak: ## Open Keycloak admin console
	$(OPEN) http://localhost:8180/admin

open-prometheus: ## Open Prometheus
	$(OPEN) http://localhost:9090

# ── Utilities ─────────────────────────────────────────────────────────────────

token: ## Get a test token (regular-user)
	@curl -s -X POST \
		http://localhost:8180/realms/demo-realm/protocol/openid-connect/token \
		-H "Content-Type: application/x-www-form-urlencoded" \
		-d "client_id=gateway-client" \
		-d "grant_type=password" \
		-d "username=regular-user" \
		-d "password=user123" \
		-d "scope=openid"

token-admin: ## Get a test token (admin-user)
	@curl -s -X POST \
		http://localhost:8180/realms/demo-realm/protocol/openid-connect/token \
		-H "Content-Type: application/x-www-form-urlencoded" \
		-d "client_id=gateway-client" \
		-d "grant_type=password" \
		-d "username=admin-user" \
		-d "password=admin123" \
		-d "scope=openid" | python3 -m json.tool

clean: down clean-java ## Stop infra and clean all build artifacts
	@echo "$(GREEN)Clean complete.$(RESET)"
