# Job Scheduler

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


## Running the Application

### Using Docker Compose

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
