# MCXboxBroadcast Standalone Development Guide

## Prerequisites

- **Docker** and **Docker Compose** (for development environment)
- **Microsoft/Xbox Live account** (for authentication)

**Note:** Java 21 is no longer required locally as the development environment runs in Docker.

## Quick Start

**Note: Development setup currently supports the standalone application only.**

1. **Clone and setup**
   ```bash
   git clone <repository-url>
   cd Broadcaster
   ```

2. **Start development with hot-reload**
   ```bash
   make watch
   ```

   This starts the development container in detached mode. To view logs:
   ```bash
   make logs-follow
   ```

   To shut down the container
   ```bash
   make down
   ```

3. **Authenticate** with Microsoft when prompted (follow the browser link and enter the code)

## Development Commands

| Command | Description |
|---------|-------------|
| `make watch` | Start development container with hot reload |
| `make build` | Build the application Docker image |
| `make build-no-cache` | Build the application Docker image without cache |
| `make down` | Stop and remove the development container |
| `make logs` | Show logs from the service (one-time) |
| `make logs-follow` | Follow logs from the service (continuous) |
| `make clean` | Clean up containers, networks, and volumes |

### Production Commands

| Command | Description |
|---------|-------------|
| `make docker-build` | Build production Docker image |
| `make docker-build-no-cache` | Build production image without cache |
| `make docker-run` | Run production container (one-time) |

## Project Structure

```
Broadcaster/
├── core/                           # Core MCXboxBroadcast library
│   └── src/main/java/             # Core Java source code
├── bootstrap/
│   ├── geyser/                    # Geyser plugin integration
│   └── standalone/                # Standalone application
│       ├── src/main/java/         # Standalone Java source code
│       ├── config.yml             # Configuration file (created during development)
│       ├── cache/                 # Runtime cache directory (created during development)
│       └── logs/                  # Log files (created during development)
├── build-logic/                   # Gradle build configuration
└── Makefile                       # Development commands
```

## Configuration

The application creates and reads `config.yml` in its working directory. This file contains session settings, server information, and feature toggles.

**Important:** During development, edit the config file at `bootstrap/standalone/config.yml` (this file is created automatically when you run `make watch`).

## Production Docker Build
For production deployments:

```bash
# Build production Docker image
make docker-build

# Run the Docker container
make docker-run
```

The production image packages the standalone application with all dependencies for deployment.

## Architecture

- **Core Library**: Xbox Live integration and session management
- **Bootstrap Modules**: Different deployment methods (standalone app, Geyser plugin)
- **Configuration**: YAML-based settings in `config/config.yml`
- **Storage**: File-based token caching in `config/cache/`
