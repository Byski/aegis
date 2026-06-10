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

# --- Filled in later phases -------------------------------------------------
# up      : create kind cluster and deploy the stack        (Phase 2)
# demo    : one-command end-to-end demo                      (Phase 4)
# load    : run k6 load test                                 (Phase 5)
# chaos   : run chaos experiment and measure recovery        (Phase 5)
# down    : tear down the kind cluster                       (Phase 2)
