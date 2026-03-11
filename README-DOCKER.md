# 🚀 Workflow Automation Platform - Docker Infrastructure

Complete Docker Compose setup for the workflow automation platform infrastructure.

## 📋 What's Included

### Core Infrastructure
- **PostgreSQL** (Port 5432) - Main database with pgvector extension for RAG
- **Redis** (Port 6379) - Caching and session storage
- **Apache Kafka** (Port 9092) - Message broker for event-driven architecture
- **Zookeeper** (Port 2181) - Kafka coordinator

### Service Discovery
- **Eureka Server** (Port 8761) - Service registry and discovery

### Monitoring & Observability
- **Kafka UI** (Port 8090) - Monitor Kafka topics and messages
- **PgAdmin** (Port 5050) - PostgreSQL management UI
- **Elasticsearch** (Port 9200) - Log storage and search
- **Kibana** (Port 5601) - Log visualization
- **Logstash** (Port 5000) - Log processing pipeline
- **Prometheus** (Port 9090) - Metrics collection
- **Grafana** (Port 3000) - Metrics visualization dashboards

---

## 🚀 Quick Start

### Prerequisites
- Docker Desktop installed (Windows/Mac) or Docker Engine + Docker Compose (Linux)
- At least 8GB RAM available
- 20GB free disk space

### 1. Project Structure
Create the following directory structure:

```
workflow-platform/
├── docker-compose.yml
├── init-scripts/
│   └── 01-init-schema.sql
├── logstash/
│   ├── config/
│   │   └── logstash.yml
│   └── pipeline/
│       └── logstash.conf
├── prometheus/
│   └── prometheus.yml
└── grafana/
    └── provisioning/
```

### 2. Start All Services

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Check status
docker-compose ps
```

### 3. Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (⚠️ deletes all data)
docker-compose down -v
```

---

## 🔗 Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| PostgreSQL | `localhost:5432` | User: `postgres`, Password: `postgres` |
| PgAdmin | http://localhost:5050 | Email: `admin@workflow.com`, Password: `admin` |
| Redis | `localhost:6379` | No password |
| Kafka | `localhost:9092` | No authentication |
| Kafka UI | http://localhost:8090 | No authentication |
| Eureka | http://localhost:8761 | No authentication |
| Elasticsearch | http://localhost:9200 | No authentication |
| Kibana | http://localhost:5601 | No authentication |
| Prometheus | http://localhost:9090 | No authentication |
| Grafana | http://localhost:3000 | User: `admin`, Password: `admin` |

---

## 📊 Database Schema

The PostgreSQL database is automatically initialized with the following tables:

### Core Tables
- **workflows** - Store workflow definitions (nodes, edges, config)
- **execution_history** - Track all workflow executions
- **users** - User accounts and authentication
- **credentials** - Encrypted API keys and OAuth tokens
- **triggers** - Scheduled jobs and webhook configurations

### Supporting Tables
- **execution_logs** - Detailed logs for each execution
- **document_embeddings** - Vector embeddings for RAG (pgvector)
- **audit_trail** - Track all user actions

### Sample Data
A sample workflow is pre-loaded:
- **User**: `test@workflow.com`
- **Workflow**: "Daily Email Summary" (Gmail → Gemini → HTTP)

---

## 🔧 Testing the Setup

### 1. Test PostgreSQL Connection

```bash
# Connect to PostgreSQL
docker exec -it workflow-postgres psql -U postgres -d workflow_db

# List tables
\dt

# Query sample workflow
SELECT workflow_id, name, status FROM workflows;

# Exit
\q
```

### 2. Test Redis Connection

```bash
# Connect to Redis
docker exec -it workflow-redis redis-cli

# Test commands
PING
SET test "Hello World"
GET test

# Exit
exit
```

### 3. Test Kafka

```bash
# Create a test topic
docker exec -it workflow-kafka kafka-topics --create \
  --topic test-topic \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1

# List topics
docker exec -it workflow-kafka kafka-topics --list \
  --bootstrap-server localhost:9092

# Produce a message
echo "Hello Kafka" | docker exec -i workflow-kafka kafka-console-producer \
  --topic test-topic \
  --bootstrap-server localhost:9092

# Consume messages
docker exec -it workflow-kafka kafka-console-consumer \
  --topic test-topic \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

### 4. View Kafka UI
Open http://localhost:8090 to see:
- All topics including `workflow-trigger`, `workflow-execution`
- Messages in real-time
- Consumer groups and lag

### 5. Check Eureka Dashboard
Open http://localhost:8761 to see registered services (will be empty until you start your Spring Boot services)

---

## 📈 Monitoring Setup

### Kibana (Logs)
1. Open http://localhost:5601
2. Go to "Management" → "Stack Management" → "Index Patterns"
3. Create index pattern: `workflow-logs-*`
4. Go to "Discover" to view logs

### Grafana (Metrics)
1. Open http://localhost:3000
2. Login with `admin` / `admin`
3. Add Prometheus data source:
   - URL: `http://prometheus:9090`
4. Import dashboards for Spring Boot metrics

---

## 🔍 Troubleshooting

### Services Won't Start

```bash
# Check logs for errors
docker-compose logs [service-name]

# Common issues:
# 1. Port already in use
sudo lsof -i :5432  # Check what's using PostgreSQL port
sudo lsof -i :9092  # Check what's using Kafka port

# 2. Insufficient memory
docker system df  # Check Docker disk usage
docker system prune  # Clean up unused resources
```

### PostgreSQL Connection Issues

```bash
# Restart PostgreSQL
docker-compose restart postgres

# Check if database is ready
docker exec -it workflow-postgres pg_isready -U postgres
```

### Kafka Connection Issues

```bash
# Restart Kafka and Zookeeper
docker-compose restart zookeeper kafka

# Check Kafka logs
docker-compose logs kafka
```

### Reset Everything

```bash
# Stop and remove all containers, networks, and volumes
docker-compose down -v

# Remove all images
docker-compose down --rmi all

# Start fresh
docker-compose up -d
```

---

## 🔐 Security Notes (for Production)

### ⚠️ Current Setup is for DEVELOPMENT ONLY

For production deployment, you MUST:

1. **Change All Default Passwords**
   ```yaml
   # PostgreSQL
   POSTGRES_PASSWORD: <strong-password>
   
   # PgAdmin
   PGADMIN_DEFAULT_PASSWORD: <strong-password>
   
   # Grafana
   GF_SECURITY_ADMIN_PASSWORD: <strong-password>
   ```

2. **Enable Authentication**
   - Kafka: Enable SASL/SSL
   - Elasticsearch: Enable X-Pack Security
   - Redis: Set requirepass

3. **Use Secrets Management**
   - Docker Secrets
   - HashiCorp Vault
   - AWS Secrets Manager

4. **Network Security**
   - Don't expose all ports publicly
   - Use reverse proxy (Nginx/Traefik)
   - Enable TLS/SSL for all services

5. **Data Backup**
   - Regular PostgreSQL backups
   - Volume snapshots
   - Disaster recovery plan

---

## 📦 Volume Management

### View Volumes
```bash
docker volume ls | grep workflow
```

### Backup PostgreSQL Data
```bash
# Create backup
docker exec workflow-postgres pg_dump -U postgres workflow_db > backup.sql

# Restore backup
docker exec -i workflow-postgres psql -U postgres workflow_db < backup.sql
```

### Clean Up Unused Volumes
```bash
# Remove all unused volumes (⚠️ careful!)
docker volume prune
```

---

## 🚀 Next Steps

Now that infrastructure is running, you can:

1. **Build and Start Microservices**
   - Execution Service (Port 8082)
   - Workflow Service (Port 8081)
   - Trigger Service (Port 8083)
   - Gmail Service (Port 8091)
   - Gemini Service (Port 8092)
   - HTTP Service (Port 8093)

2. **Connect Services to Infrastructure**
   ```yaml
   # In your application.yml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/workflow_db
     kafka:
       bootstrap-servers: localhost:9092
   ```

3. **Build Frontend**
   - Angular application with drag-and-drop workflow designer
   - Connect to API Gateway

---

## 📚 Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Cloud Netflix Eureka](https://spring.io/projects/spring-cloud-netflix)

---

## 💡 Tips

### Performance Optimization
```bash
# Increase Docker memory (Docker Desktop)
# Settings → Resources → Memory: 8GB+

# Monitor resource usage
docker stats
```

### Development Workflow
```bash
# Start only core services (no monitoring)
docker-compose up -d postgres redis kafka zookeeper eureka-server

# Start monitoring separately when needed
docker-compose up -d prometheus grafana kibana
```

### Kafka Topic Management
```bash
# Create workflow topics
docker exec -it workflow-kafka kafka-topics --create \
  --topic workflow-trigger \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

docker exec -it workflow-kafka kafka-topics --create \
  --topic workflow-execution \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

---

## ✅ Health Checks

All services include health checks. View status:

```bash
docker-compose ps
```

Healthy services will show "healthy" in the status column.

---

**🎉 You're all set! Your infrastructure is ready for development.**
