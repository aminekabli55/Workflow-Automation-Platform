#!/bin/bash

# Color codes for pretty output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_message() {
    color=$1
    message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_message "$RED" "❌ Docker is not running. Please start Docker Desktop first."
        exit 1
    fi
    print_message "$GREEN" "✅ Docker is running"
}

# Function to create required directories
create_directories() {
    print_message "$BLUE" "📁 Creating required directories..."
    mkdir -p init-scripts
    mkdir -p logstash/config
    mkdir -p logstash/pipeline
    mkdir -p prometheus
    mkdir -p grafana/provisioning
    print_message "$GREEN" "✅ Directories created"
}

# Function to start all services
start_all() {
    print_message "$BLUE" "🚀 Starting all services..."
    docker-compose up -d
    
    print_message "$YELLOW" "⏳ Waiting for services to be healthy..."
    sleep 10
    
    print_message "$GREEN" "✅ Services started! Checking status..."
    docker-compose ps
    
    print_message "$BLUE" "\n📊 Access URLs:"
    echo "  - PgAdmin:       http://localhost:5050"
    echo "  - Kafka UI:      http://localhost:8090"
    echo "  - Eureka:        http://localhost:8761"
    echo "  - Kibana:        http://localhost:5601"
    echo "  - Prometheus:    http://localhost:9090"
    echo "  - Grafana:       http://localhost:3000"
}

# Function to start only core services (no monitoring)
start_core() {
    print_message "$BLUE" "🚀 Starting core services only..."
    docker-compose up -d postgres redis kafka zookeeper eureka-server
    
    print_message "$YELLOW" "⏳ Waiting for services to be healthy..."
    sleep 10
    
    print_message "$GREEN" "✅ Core services started!"
    docker-compose ps
}

# Function to stop all services
stop_all() {
    print_message "$YELLOW" "🛑 Stopping all services..."
    docker-compose down
    print_message "$GREEN" "✅ All services stopped"
}

# Function to restart all services
restart_all() {
    print_message "$YELLOW" "🔄 Restarting all services..."
    docker-compose restart
    print_message "$GREEN" "✅ Services restarted"
}

# Function to view logs
view_logs() {
    if [ -z "$1" ]; then
        print_message "$BLUE" "📋 Viewing logs for all services (Ctrl+C to exit)..."
        docker-compose logs -f
    else
        print_message "$BLUE" "📋 Viewing logs for $1 (Ctrl+C to exit)..."
        docker-compose logs -f "$1"
    fi
}

# Function to check service status
check_status() {
    print_message "$BLUE" "📊 Service Status:"
    docker-compose ps
    
    print_message "$BLUE" "\n💾 Volume Usage:"
    docker volume ls | grep workflow
    
    print_message "$BLUE" "\n🌐 Network:"
    docker network ls | grep workflow
}

# Function to cleanup (nuclear option)
cleanup() {
    print_message "$RED" "⚠️  WARNING: This will delete ALL data (containers, volumes, networks)"
    read -p "Are you sure? Type 'yes' to confirm: " confirmation
    
    if [ "$confirmation" = "yes" ]; then
        print_message "$YELLOW" "🗑️  Cleaning up everything..."
        docker-compose down -v
        docker volume prune -f
        print_message "$GREEN" "✅ Cleanup complete"
    else
        print_message "$BLUE" "Cleanup cancelled"
    fi
}

# Function to test services
test_services() {
    print_message "$BLUE" "🧪 Testing services...\n"
    
    # Test PostgreSQL
    print_message "$YELLOW" "Testing PostgreSQL..."
    if docker exec workflow-postgres pg_isready -U postgres > /dev/null 2>&1; then
        print_message "$GREEN" "✅ PostgreSQL is ready"
    else
        print_message "$RED" "❌ PostgreSQL is not responding"
    fi
    
    # Test Redis
    print_message "$YELLOW" "Testing Redis..."
    if docker exec workflow-redis redis-cli ping > /dev/null 2>&1; then
        print_message "$GREEN" "✅ Redis is ready"
    else
        print_message "$RED" "❌ Redis is not responding"
    fi
    
    # Test Kafka
    print_message "$YELLOW" "Testing Kafka..."
    if docker exec workflow-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        print_message "$GREEN" "✅ Kafka is ready"
    else
        print_message "$RED" "❌ Kafka is not responding"
    fi
    
    # Test Eureka
    print_message "$YELLOW" "Testing Eureka..."
    if curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; then
        print_message "$GREEN" "✅ Eureka is ready"
    else
        print_message "$RED" "❌ Eureka is not responding"
    fi
}

# Function to create Kafka topics
create_kafka_topics() {
    print_message "$BLUE" "📨 Creating Kafka topics..."
    
    docker exec workflow-kafka kafka-topics --create \
        --topic workflow-trigger \
        --bootstrap-server localhost:9092 \
        --partitions 3 \
        --replication-factor 1 \
        --if-not-exists
    
    docker exec workflow-kafka kafka-topics --create \
        --topic workflow-execution \
        --bootstrap-server localhost:9092 \
        --partitions 3 \
        --replication-factor 1 \
        --if-not-exists
    
    print_message "$GREEN" "✅ Kafka topics created"
    
    print_message "$BLUE" "\n📋 Available topics:"
    docker exec workflow-kafka kafka-topics --list --bootstrap-server localhost:9092
}

# Function to backup database
backup_db() {
    timestamp=$(date +"%Y%m%d_%H%M%S")
    backup_file="backup_${timestamp}.sql"
    
    print_message "$BLUE" "💾 Creating database backup..."
    docker exec workflow-postgres pg_dump -U postgres workflow_db > "$backup_file"
    
    if [ $? -eq 0 ]; then
        print_message "$GREEN" "✅ Backup created: $backup_file"
    else
        print_message "$RED" "❌ Backup failed"
    fi
}

# Function to show help
show_help() {
    print_message "$BLUE" "🔧 Workflow Platform Docker Manager\n"
    echo "Usage: ./manage.sh [command]"
    echo ""
    echo "Commands:"
    echo "  start         - Start all services"
    echo "  start-core    - Start only core services (no monitoring)"
    echo "  stop          - Stop all services"
    echo "  restart       - Restart all services"
    echo "  status        - Show service status"
    echo "  logs [svc]    - View logs (optionally for specific service)"
    echo "  test          - Test all service connections"
    echo "  topics        - Create Kafka topics"
    echo "  backup        - Backup PostgreSQL database"
    echo "  cleanup       - Remove all containers, volumes, networks (⚠️ DANGER)"
    echo "  help          - Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./manage.sh start"
    echo "  ./manage.sh logs kafka"
    echo "  ./manage.sh test"
}

# Main script logic
main() {
    check_docker
    
    case "$1" in
        start)
            create_directories
            start_all
            ;;
        start-core)
            create_directories
            start_core
            ;;
        stop)
            stop_all
            ;;
        restart)
            restart_all
            ;;
        status)
            check_status
            ;;
        logs)
            view_logs "$2"
            ;;
        test)
            test_services
            ;;
        topics)
            create_kafka_topics
            ;;
        backup)
            backup_db
            ;;
        cleanup)
            cleanup
            ;;
        help|"")
            show_help
            ;;
        *)
            print_message "$RED" "❌ Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
