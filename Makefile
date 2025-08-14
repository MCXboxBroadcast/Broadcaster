# MCXboxBroadcast Makefile (Multi-stage Docker Build)
# Variables
IMAGE_NAME = mcxboxbroadcast-standalone

# Default target
.PHONY: help
help:
	@echo "Development:"
	@echo "  watch                    - Start development server with hot reload"
	@echo "  build                    - Build the application"
	@echo "  run                      - Run the application (after building)"
	@echo "  down                     - Stop and remove the service"
	@echo "  logs                     - Show logs from the service"
	@echo "  logs-follow              - Follow logs from the service"
	@echo "  shell                    - Open a shell in the container"
	@echo "  clean                    - Clean build artifacts"
	@echo ""
	@echo "Production:"
	@echo "  docker-build             - Build production Docker image"
	@echo "  docker-build-no-cache    - Build production image without cache"
	@echo "  docker-run               - Run production container (one-time)"

# =============================================================================
# Development Targets
# =============================================================================

# Build the service
.PHONY: build
build:
	@echo "Building MCXboxBroadcast service..."
	docker compose build

.PHONY: build-no-cache
build-no-cache:
	@echo "Building MCXboxBroadcast service without cache..."
	docker compose build --no-cache

# Start development container with hot reload
.PHONY: watch
watch:
	@echo "Starting development container with hot reload..."
	docker compose up -d
	@echo "Run make logs-follow to see logs"

# Stop and remove the service
.PHONY: down
down:
	@echo "Stopping and removing MCXboxBroadcast service..."
	docker compose down

# Show logs
.PHONY: logs
logs:
	docker compose logs

# Follow logs
.PHONY: logs-follow
logs-follow:
	docker compose logs -f

# Clean up
.PHONY: clean
clean:
	@echo "Cleaning up containers, networks, and volumes..."
	docker compose down -v --remove-orphans

# =============================================================================
# Production Docker Targets
# =============================================================================

.PHONY: docker-build
docker-build:
	docker build -t $(IMAGE_NAME):latest -f ./bootstrap/standalone/Dockerfile .

.PHONY: docker-build-no-cache
docker-build-no-cache:
	docker build --no-cache -t $(IMAGE_NAME):latest -f ./bootstrap/standalone/Dockerfile .

.PHONY: docker-run
docker-run:
	docker run --rm -it -v ./config:/opt/app/config $(IMAGE_NAME):latest
