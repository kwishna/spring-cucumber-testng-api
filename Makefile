# ==================================================
# Makefile for Spring Cucumber Test Framework
# ==================================================

.PHONY: help test clean grid-start grid-stop grid-status grid-test grid-standalone grid-video \
        docker-build docker-test full-setup reports logs ps down prune watch parallel smoke api ui

# Default target
.DEFAULT_GOAL := help

## help: Show this help message
help:
	@echo "Spring Cucumber Test Framework - Docker Commands"
	@echo "================================================="
	@echo ""
	@echo "Local Testing:"
	@echo "  make test          - Run tests locally"
	@echo "  make test-chrome   - Run tests in Chrome"
	@echo "  make test-firefox  - Run tests in Firefox"
	@echo "  make test-edge     - Run tests in Edge"
	@echo "  make test-headless - Run tests in headless mode"
	@echo ""
	@echo "Grid Commands:"
	@echo "  make grid-start        - Start Selenium Grid"
	@echo "  make grid-stop         - Stop Selenium Grid"
	@echo "  make grid-status       - Show Grid status"
	@echo "  make grid-test         - Run tests on Grid"
	@echo "  make grid-standalone   - Start standalone Grid (lighter)"
	@echo "  make grid-video        - Start Grid with video recording"
	@echo ""
	@echo "Docker Commands:"
	@echo "  make docker-build      - Build Docker image"
	@echo "  make docker-test       - Run tests in Docker"
	@echo "  make full-setup        - Complete setup (Grid + Tests)"
	@echo ""
	@echo "Utility Commands:"
	@echo "  make clean             - Clean build artifacts"
	@echo "  make reports           - Open test reports"
	@echo "  make logs              - View Grid logs"
	@echo "  make ps                - Show running Docker containers"
	@echo "  make down              - Stop and remove all containers"
	@echo "  make prune             - Clean up Docker resources"
	@echo ""
	@echo "Test Categories:"
	@echo "  make watch             - Watch tests (auto re-run)"
	@echo "  make parallel          - Run tests in parallel"
	@echo "  make smoke             - Run smoke tests only"
	@echo "  make api               - Run API tests only"
	@echo "  make ui                - Run UI tests only"

## test: Run tests locally
test:
	./gradlew clean test

## test-chrome: Run tests in Chrome
test-chrome:
	./gradlew test -Dbrowser=chrome

## test-firefox: Run tests in Firefox
test-firefox:
	./gradlew test -Dbrowser=firefox

## test-edge: Run tests in Edge
test-edge:
	./gradlew test -Dbrowser=edge

## test-headless: Run tests in headless mode
test-headless:
	./gradlew test -Dheadless=true

## grid-start: Start Selenium Grid
grid-start:
	@echo "🚀 Starting Selenium Grid..."
	@docker-compose up -d
	@echo "⏳ Waiting for Grid to be ready..."
	@sleep 10
	@echo "✅ Grid is ready!"
	@echo "📊 Grid Console: http://localhost:4444"
	@echo "🖥️ noVNC Chrome: http://localhost:7900"

## grid-stop: Stop Selenium Grid
grid-stop:
	@echo "🛑 Stopping Selenium Grid..."
	@docker-compose down
	@echo "✅ Grid stopped"

## grid-status: Show Grid status
grid-status:
	@./docker-scripts/view-grid-status.sh

## grid-test: Run tests on Grid
grid-test:
	@echo "🧪 Running tests on Selenium Grid..."
	./gradlew test -Dremote.execution=true -Dremote.url=http://localhost:4444/wd/hub

## grid-standalone: Start standalone Grid
grid-standalone:
	@echo "🚀 Starting standalone Selenium Grid..."
	@docker-compose -f docker-compose.standalone.yml up -d
	@sleep 5
	@echo "✅ Standalone Grid is ready!"
	@echo "📊 Grid Console: http://localhost:4444"

## grid-video: Start Grid with video recording
grid-video:
	@echo "🎥 Starting Grid with video recording..."
	@docker-compose -f docker-compose.video.yml up -d
	@sleep 10
	@echo "✅ Grid with video recording is ready!"
	@echo "📹 Videos will be saved to ./videos/"

## docker-build: Build Docker image
docker-build:
	@echo "🐳 Building Docker image..."
	@docker build -t spring-cucumber-tests:latest .
	@echo "✅ Docker image built successfully"

## docker-test: Run tests in Docker container
docker-test: docker-build
	@echo "🧪 Running tests in Docker..."
	@docker run --rm \
		--network selenium-grid-network \
		-e REMOTE_EXECUTION=true \
		-e REMOTE_URL=http://selenium-hub:4444/wd/hub \
		-v $(PWD)/build:/app/build \
		-v $(PWD)/ExtentReports:/app/ExtentReports \
		-v $(PWD)/logs:/app/logs \
		spring-cucumber-tests:latest

## full-setup: Complete setup - Grid + Tests
full-setup:
	@echo "🎯 Complete Docker Setup"
	@echo "========================"
	@$(MAKE) grid-start
	@sleep 5
	@$(MAKE) docker-test
	@echo ""
	@echo "✅ All done!"

## clean: Clean build artifacts
clean:
	@echo "🧹 Cleaning build artifacts..."
	./gradlew clean
	@rm -rf build/ .gradle/ ExtentReports/ videos/
	@echo "✅ Clean complete"

## reports: Open test reports
reports:
	@echo "📊 Opening test reports..."
	@open build/reports/tests/test/index.html 2>/dev/null || \
		xdg-open build/reports/tests/test/index.html 2>/dev/null || \
		echo "Report not found. Run tests first."

## logs: View Grid logs
logs:
	@docker-compose logs -f

## ps: Show running Docker containers
ps:
	@docker-compose ps

## down: Stop and remove all containers
down:
	@echo "🛑 Stopping all containers..."
	@docker-compose down
	@docker-compose -f docker-compose.standalone.yml down 2>/dev/null || true
	@docker-compose -f docker-compose.video.yml down 2>/dev/null || true
	@echo "✅ All containers stopped"

## prune: Clean up Docker resources
prune:
	@echo "🧹 Cleaning up Docker resources..."
	@docker system prune -f
	@docker volume prune -f
	@echo "✅ Docker cleanup complete"

## watch: Watch tests (re-run on file changes)
watch:
	@echo "👀 Watching for changes..."
	./gradlew test --continuous

## parallel: Run tests in parallel
parallel:
	./gradlew test -DdataProviderThreadCount=5

## smoke: Run smoke tests only
smoke:
	./gradlew test -Dcucumber.filter.tags="@smoke"

## api: Run API tests only
api:
	./gradlew test -Dcucumber.filter.tags="@api"

## ui: Run UI tests only
ui:
	./gradlew test -Dcucumber.filter.tags="@ui"
