# Deployment Guide

## Prerequisites
- Docker and Docker Compose
- Domain name (for HTTPS)
- Reverse proxy (Nginx, Caddy, or Cloudflare Tunnel)

## 1. Build Production Image

```bash
# Set production environment variables
cp .env.example .env
# Edit .env to fill in secrets:
# - DB_PASSWORD: strong PostgreSQL password
# - JWT_SECRET: 64+ char random string (openssl rand -hex 32)
# - AI_PROVIDER: noop / openai / anthropic
# - AI_ENABLED: true / false
```

## 2. Run with Docker Compose (Production)

```bash
docker compose -f docker-compose.prod.yml --env-file .env up --build -d
```

## 3. Run with Docker Compose (Development)

```bash
# Start DB and Redis
docker compose up -d

# Run backend
./mvnw spring-boot:run

# Run frontend
cd frontend
npm install
npm run dev
```

## 4. Health Check

Once the stack is up, verify health:
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

## 5. HTTPS Configuration (Production)

Use Nginx or a reverse proxy (e.g., Caddy, Nginx, or Cloudflare Tunnel) to:
- Terminate TLS (HTTPS)
- Reverse-proxy to backend at `http://localhost:8080`
- Set security headers (`Strict-Transport-Security`, `X-Content-Type-Options`, etc.)
- Enforce rate limiting on `/api/v1/auth/**` endpoints
- Block access to internal endpoints (`/actuator/`, `/swagger-ui.html`) from the public internet

Example Nginx config:
```
server {
    listen 443 ssl http2;
    server_name api.jobassistant.com;

    ssl_certificate /etc/nginx/cert.pem;
    ssl_certificate_key /etc/nginx/key.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 6. CORS Configuration

Configure `application-prod.yml`:
```yaml
spring:
  jackson:
    serialization:
      WRITE_DATES_AS_TIMESTAMPS: false
```

Add CORS configuration to allow your production frontend domain:
```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("https://your-frontend.com")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowCredentials(true);
            }
        };
    }
}
```

## 7. Database Backups

```bash
# Schedule a daily backup
BACKUP_FILE="backup_$(date +%F).sql"
docker exec job_assistant_db_prod pg_dump -U ${DB_USER} ${DB_NAME} > ${BACKUP_FILE}

# Upload to cloud storage (e.g., AWS S3, Google Cloud Storage, Backblaze B2)
```

## 8. Monitoring & Logging

### Actuator Endpoints
```
GET /actuator/health   → DB, Redis, disk space status
GET /actuator/info      → App info/version
GET /actuator/metrics   → System metrics (CPU, memory, HTTP requests)
```

### Structured Logging
Backend logs are printed to stdout (container-friendly). Format:
```
2026-10-01T20:45:00.000Z [thread-name] INFO  com.jobassistant.Application - message
```

For production, pipe logs to a log aggregator (ELK Stack, Splunk, Datadog, etc.).

## 9. Security Checklist

| Item | Status |
|------|--------|
| Passwords hashed with BCrypt | ✅ |
| JWT tokens signed and expire | ✅ |
| Secrets in env vars, not code | ✅ |
| HTTPS only | ⚠ Configure reverse proxy |
| Rate limiting on auth endpoints | ⚠ Configure in Nginx |
| Database user has minimal privileges | ✅ |
| `.env` and `target/` in `.gitignore` | ✅ |
| Dev `docker-compose.yml` uses `${DB_*}` vars (not hardcoded passwords) | ✅ |
| Stack traces disabled in prod | ✅ |
| CORS restricted to known origins | ⚠ Configure allowedOrigins |