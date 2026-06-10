# Aegis platform automation.
# Targets are filled in progressively across build phases. Run `make help`.

CLUSTER_NAME ?= aegis
SHELL := /bin/bash

.DEFAULT_GOAL := help

.PHONY: help
help: ## Show available targets
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

.PHONY: bootstrap
bootstrap: ## Install local toolchain and wire git hooks (run once)
	./scripts/bootstrap.sh

.PHONY: hygiene
hygiene: ## Run the repository hygiene gate over all tracked files
	./scripts/check-hygiene.sh --all

.PHONY: images
images: ## Build both service images locally
	docker build -t shortener-api:local services/shortener-api
	docker build -t analytics-svc:local services/analytics-svc

.PHONY: cluster
cluster: ## Create the kind cluster if it does not exist
	@kind get clusters | grep -qx $(CLUSTER_NAME) || kind create cluster --config deploy/kind/cluster.yaml

.PHONY: up
up: cluster images ## Create cluster, build and load images, deploy the chart
	kind load docker-image shortener-api:local analytics-svc:local --name $(CLUSTER_NAME)
	helm upgrade --install aegis deploy/helm/aegis \
		--namespace aegis --create-namespace --wait --timeout 5m
	@echo "shortener: http://localhost:30080   analytics: http://localhost:30081"

.PHONY: down
down: ## Delete the kind cluster
	kind delete cluster --name $(CLUSTER_NAME)

# --- Filled in later phases -------------------------------------------------
# demo    : one-command end-to-end demo                      (Phase 4)
# load    : run k6 load test                                 (Phase 5)
# chaos   : run chaos experiment and measure recovery        (Phase 5)
