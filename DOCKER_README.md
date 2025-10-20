# 🐳 Docker Quick Start Guide

The fastest way to run tests with Docker and Selenium Grid.

## 📦 Prerequisites

- Docker Desktop (or Docker + Docker Compose)
- Make (optional, for using Makefile commands)

## ⚡ Quick Commands

### 1. Start Grid and Run Tests (One Command)

```bash
make full-setup
```

That's it! This will:
1. Start Selenium Grid with Chrome, Firefox, and Edge
2. Build the test Docker image
3. Run all tests
4. Generate reports

### 2. Use Makefile (Recommended)

```bash
# See all available commands
make help

# Start Grid
make grid-start

# Run tests on Grid
make grid-test

# Stop Grid
make grid-stop
```

### 3. Manual Docker Compose

```bash
# Start Grid
docker-compose up -d

# Run tests from host
./gradlew test -Dremote.execution=true -Dremote.url=http://localhost:4444/wd/hub

# Stop Grid
docker-compose down
```

## 🎯 Common Use Cases

### Test Specific Browser

```bash
# Chrome
make grid-start
./gradlew test -Dremote.execution=true -Dbrowser=chrome

# Firefox
./gradlew test -Dremote.execution=true -Dbrowser=firefox

# Edge
./gradlew test -Dremote.execution=true -Dbrowser=edge
```

### Lightweight Development Setup

```bash
# Start standalone Chrome Grid (lighter)
make grid-standalone

# Run tests
make grid-test

# Stop
make grid-stop
```

### Record Test Videos

```bash
# Start Grid with video recording
make grid-video

# Run tests
make grid-test

# Videos saved in ./videos/
```

### Parallel Testing

```bash
# Start Grid (supports up to 10 concurrent sessions)
make grid-start

# Run 5 tests in parallel
make parallel
```

## 📊 Monitor Tests

### View Grid Console
http://localhost:4444

### Watch Tests Live (noVNC)
- **Chrome**: http://localhost:7900
- **Firefox**: http://localhost:7901  
- **Edge**: http://localhost:7902

Password: `secret`

### Check Grid Status

```bash
make grid-status
```

## 🗂️ File Reference

| File | Purpose |
|------|---------|
| `Dockerfile` | Test application container |
| `docker-compose.yml` | Full Grid (Hub + 3 browsers) |
| `docker-compose.standalone.yml` | Lightweight standalone Grid |
| `docker-compose.video.yml` | Grid with video recording |
| `Makefile` | Easy command shortcuts |
| `.dockerignore` | Docker build exclusions |

## 📁 Helper Scripts

Located in `docker-scripts/`:

```bash
./docker-scripts/start-grid.sh          # Start Grid
./docker-scripts/stop-grid.sh           # Stop Grid  
./docker-scripts/run-tests-docker.sh    # Run tests in Docker
./docker-scripts/full-setup.sh          # Complete setup
./docker-scripts/view-grid-status.sh    # Grid status
```

Make executable:
```bash
chmod +x docker-scripts/*.sh
```

## 🔧 Configuration

### Environment Variables

```bash
# Set before running tests
export BROWSER=firefox
export HEADLESS=true
export REMOTE_EXECUTION=true

./gradlew test
```

### Profile Selection

```bash
# Use Grid profile
./gradlew test -Dspring.profiles.active=grid

# Or set environment variable
export SPRING_PROFILES_ACTIVE=grid
./gradlew test
```

## 🛠️ Troubleshooting

### Grid Won't Start

```bash
# Check Docker is running
docker ps

# Check logs
docker-compose logs

# Check port 4444
lsof -i :4444
```

### Tests Can't Connect

```bash
# Verify Grid is ready
curl http://localhost:4444/wd/hub/status

# Check Grid status
make grid-status
```

### Out of Memory

Edit `docker-compose.yml`:
```yaml
shm_size: '4gb'  # Increase from 2gb
```

## 📚 Full Documentation

For detailed information, see:
- **[DOCKER_GUIDE.md](DOCKER_GUIDE.md)** - Complete Docker documentation
- **[SELENIUM_GRID_GUIDE.md](SELENIUM_GRID_GUIDE.md)** - Grid integration guide

## 🎓 Examples

### Example 1: Quick Test Run
```bash
make grid-start && make grid-test && make grid-stop
```

### Example 2: Multi-Browser Testing
```bash
make grid-start
./gradlew test -Dremote.execution=true -Dbrowser=chrome
./gradlew test -Dremote.execution=true -Dbrowser=firefox
./gradlew test -Dremote.execution=true -Dbrowser=edge
make grid-stop
```

### Example 3: Debug with Video
```bash
make grid-video
make grid-test
# Check videos in ./videos/
```

### Example 4: CI/CD Pipeline
```bash
#!/bin/bash
make grid-start
sleep 10
make grid-test || EXIT_CODE=$?
make grid-stop
exit ${EXIT_CODE:-0}
```

## 💡 Pro Tips

1. **Use Standalone for Development**
   ```bash
   make grid-standalone  # Faster startup, less resources
   ```

2. **Watch Tests Live**
   - Access noVNC at http://localhost:7900 while tests run

3. **Parallel Execution**
   ```bash
   make parallel  # Runs multiple tests concurrently
   ```

4. **Clean Resources**
   ```bash
   make prune  # Clean up Docker resources
   ```

5. **Monitor Resources**
   ```bash
   docker stats  # See CPU/Memory usage
   ```

## 🚀 Ready to Start?

```bash
# One command to rule them all
make full-setup
```

Happy Testing! 🎉

