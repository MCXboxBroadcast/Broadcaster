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

MCXboxBroadcast acts as a **connection broker** that makes Minecraft servers discoverable through Xbox Live and facilitates friend-to-friend joining. It does **not** relay ongoing gameplay traffic.

### High-Level Components

- **Core Library**: Xbox Live integration and session management
- **Bootstrap Modules**: Different deployment methods (standalone app, Geyser plugin)
- **Configuration**: YAML-based settings in `config/config.yml`
- **Storage**: File-based token caching in `config/cache/`

### Xbox Live Integration Architecture

#### RTA (Real-Time Activity) WebSocket
**Purpose**: Xbox Live's real-time notification system
- **Connects to**: `wss://rta.xboxlive.com/connect`
- **Role**: Session discovery and social events
- **Critical for**: Making your Minecraft session visible to Xbox friends

**What RTA does**:
- Subscribes to session directory events (`https://sessiondirectory.xboxlive.com/connections/`)
- Subscribes to social/friend events (`https://social.xboxlive.com/users/xuid(...)/friends`)
- Receives regular "shoulderTaps" (~30 second intervals) that keep session alive and visible
- Handles friend request notifications and auto-acceptance
- **Without RTA**: Session goes stale in Xbox Live directory → friends cannot see session

#### RTC (Real-Time Communication) WebSocket
**Purpose**: WebRTC signaling for peer-to-peer connections
- **Connects to**: `wss://signal.franchise.minecraft-services.net/ws/v1.0/signaling/`
- **Role**: Connection establishment between players
- **Critical for**: Actual joining process when friends click "Join"

**What RTC does**:
- Handles WebRTC signaling (SDP offer/answer exchange) - *exchanges connection*
- Manages ICE candidate negotiation for NAT traversal - *finds best network path through firewalls and routers*
- Establishes DTLS/SCTP encrypted data channels - *sets up secure, private communication tunnel*
- Sends heartbeats every 40 seconds to maintain connection
- **Without RTC**: Friends can see session but joining fails

### Connection Flow

1. **Discovery**: RTA keeps your session visible in friend's Xbox app via regular shoulderTaps
2. **Join Attempt**: Friend clicks "Join" → RTC receives `CONNECTREQUEST` message
3. **Connection Setup**: RTC handles WebRTC P2P connection establishment
4. **Authentication**: Brief Minecraft protocol handshake for validation
5. **Transfer**: MCXboxBroadcast sends `TransferPacket` to redirect friend to real server
6. **Cleanup**: MCXboxBroadcast closes its connection

**Result**: Friend is now connected directly to your game server. MCXboxBroadcast is no longer involved.

#### Authentication Flow

1. Microsoft/Xbox Live device code authentication
2. Token caching in `./cache/` directory
3. Automatic token refresh when needed
4. **RTA websocket recreation required** when tokens refresh (current limitation)


### Troubleshooting

**Friends can't see session**: Check RTA websocket health and shoulderTap flow
**Friends can see "Join" button but can't join**: Check RTC websocket and network connectivity

### Failure Scenarios

#### RTA Failure (Session Discovery Broken)
- ❌ No shoulderTaps received → session goes stale in Xbox Live directory
- ❌ Friend requests cannot be auto-accepted (social notifications broken)
- ❌ Friends cannot see your session in Xbox app/console
- ❌ No "Join" button appears for friends
- ✅ Existing players unaffected (already connected to real server)


#### RTC Failure (Join Attempts Fail)
- ✅ Friends can see your session in Xbox app/console (RTA working)
- ✅ "Join" button appears for friends
- ❌ Clicking "Join" fails with connection errors
- ✅ Existing players unaffected (already connected to real server)

#### MCXboxBroadcast Downtime
- **Existing Players**: ✅ Unaffected (connected directly to real server)
- **New Players**: ❌ Cannot join (no discovery or connection broker)
