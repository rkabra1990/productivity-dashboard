# Productivity Dashboard

A Spring Boot application for managing tasks and productivity using the Eisenhower Matrix.

## Features

- Task management with drag-and-drop interface
- Priority matrix for task organization
- Responsive design for all devices
- User authentication and authorization
- RESTful API endpoints
- Health monitoring with Actuator

## Prerequisites

- Java 17 or higher
- Maven 3.6.3 or higher
- MySQL/PostgreSQL database
- Node.js and npm (for frontend assets if needed)

## Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/productivity-dashboard.git
   cd productivity-dashboard
   ```

2. **Configure the database**
   - Create a new database in MySQL/PostgreSQL
   - Update `src/main/resources/application.yml` with your database credentials

3. **Run the application**
   ```bash
   # Using Maven
   mvn spring-boot:run
   
   # Or build and run the JAR
   mvn clean package
   java -jar target/productivity-dashboard-0.0.1-SNAPSHOT.jar
   ```

4. **Access the application**
   - Open http://localhost:8080 in your browser

## Production Deployment

### Environment Variables

Set the following environment variables in your production environment:

```bash
# Database
DATABASE_URL=jdbc:postgresql://your-db-host:5432/your-db-name
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password

# Security
ADMIN_USERNAME=admin
ADMIN_PASSWORD=changeit
JWT_SECRET=your-jwt-secret

# Server
PORT=8080
SPRING_PROFILES_ACTIVE=prod

# Optional: Email configuration
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_USERNAME=your-email@example.com
SPRING_MAIL_PASSWORD=your-email-password
```

### Building for Production

```bash
# Build with production profile
mvn clean package -Pprod -DskipTests

# Run the application
java -jar target/productivity-dashboard-0.0.1-SNAPSHOT.jar
```

### Docker Deployment

1. **Build the Docker image**
   ```bash
   docker build -t productivity-dashboard .
   ```

2. **Run the container**
   ```bash
   docker run -d \
     -p 8080:8080 \
     -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/your-db \
     -e DB_USERNAME=dbuser \
     -e DB_PASSWORD=dbpass \
     productivity-dashboard
   ```

## API Documentation

API documentation is available at `/swagger-ui.html` when running in development mode.

## Monitoring

- Health check: `/actuator/health`
- Info: `/actuator/info`
- Metrics: `/actuator/metrics`

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Spring Boot Team
- Thymeleaf
- Bootstrap
- Font Awesome
