# Distributed Job Scheduler

A highly available, production-grade, distributed job scheduler built with Spring Boot 3 and Java 17.

## Features

- **Distributed Architecture**: Uses database-level locking with SKIP LOCKED for multi-instance deployment
- **CRON Scheduling**: Supports 6-part CRON expressions (second minute hour day month dayOfWeek)
- **Execution Types**: ATLEAST_ONCE and ATMOST_ONCE execution guarantees
- **Failure Recovery**: Automatic retry with exponential backoff for failed jobs
- **Stale Detection**: Automatically marks stale running jobs as failed
- **Async Execution**: Non-blocking job execution with configurable thread pools
- **REST API**: Complete REST API for job management and monitoring
- **Production Ready**: Docker support, health checks, and comprehensive logging

## Architecture

### Core Components

1. **JobSchedulingService**: Scans for jobs ready to execute using distributed locking
2. **JobExecutionService**: Executes jobs asynchronously with HTTP calls
3. **FailureRecoveryService**: Handles stale job detection and retry logic
4. **CronService**: Parses CRON expressions and calculates next execution times

### Database Schema

- **jobs**: Stores job definitions with scheduling information
- **job_executions**: Tracks individual execution attempts with detailed status

## API Endpoints

### Create Job
```http
POST /api/v1/jobs
Content-Type: application/json

{
  "schedule": "0 */5 * * * *",
  "apiUrl": "https://api.example.com/webhook",
  "type": "ATLEAST_ONCE"
}
```

### Get Job Executions
```http
GET /api/v1/jobs/{jobId}/executions
```

## Configuration

The application uses `application.properties` for configuration:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/job_scheduler?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=rootpassword

# Worker Pool
app.executor.core-pool-size=50
app.executor.max-pool-size=200
app.executor.queue-capacity=10000

# Job Execution
app.job.http-client.timeout-seconds=95
app.job.recovery.stale-timeout-seconds=100
app.job.retry.max-attempts=5
app.job.retry.initial-delay-ms=1000
app.job.retry.multiplier=2.0
```

## Running the Application

### Using Docker Compose (Recommended)

```bash
docker-compose up -d
```

This will start:
- MySQL 8.0 database on port 3306
- Job Scheduler application on port 8080

### Using Maven

1. Start MySQL database
2. Update `application.properties` with your database credentials
3. Run the application:

```bash
mvn spring-boot:run
```

### Building and Running with Docker

```bash
# Build the image
docker build -t job-scheduler .

# Run with external MySQL
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/job_scheduler \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=rootpassword \
  job-scheduler
```

## CRON Expression Format

The scheduler supports 6-part CRON expressions:
```
second minute hour day month dayOfWeek
```

Examples:
- `0 */5 * * * *` - Every 5 minutes
- `0 0 9 * * 1-5` - 9 AM on weekdays
- `0 30 14 * * *` - 2:30 PM daily

## Execution Types

- **ATLEAST_ONCE**: Jobs are retried on failure until successful or max retries reached
- **ATMOST_ONCE**: Jobs are not retried on failure

## Monitoring and Health Checks

- Health endpoint: `GET /actuator/health`
- Metrics endpoint: `GET /actuator/metrics`

## Production Considerations

1. **Database**: Use a production-grade MySQL instance with proper backup strategy
2. **Scaling**: Multiple application instances can run simultaneously due to distributed locking
3. **Monitoring**: Configure proper logging and monitoring for production use
4. **Security**: Implement proper authentication and authorization for production deployment
5. **Network**: Ensure proper network connectivity for HTTP job executions

## Development

### Project Structure
```
src/main/java/com/scheduler/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data transfer objects
├── entity/         # JPA entities
├── exception/      # Custom exceptions
├── repository/     # Data repositories
└── service/        # Business logic services
```

### Building
```bash
mvn clean package
```

### Testing
```bash
mvn test
```
