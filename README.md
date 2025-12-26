# Payment Service
## Overview
- The Payment Service is responsible for managing payment processing, transaction handling, and payment method management using Stripe as the payment gateway.

- It exposes a REST API, handles Stripe webhooks, processes Stripe Payment Intents, and coordinates payment state with other services in the Mazadak platform.

- The Payment Service is the owner of payment transaction state and payment gateway integration within the platform.

## API Endpoints
- See [Payment Service Wiki Page](https://github.com/Mazaadak/.github/wiki/Payment-Service) for a detailed breakdown of the service's API endpoints
- Swagger UI available at `http://localhost:18081/swagger-ui/index.html` when running locally

## How to Run
You can run it via [Docker Compose](https://github.com/Mazaadak/mazadak-infrastructure) or [Kubernetes](https://github.com/Mazaadak/mazadak-k8s/)

## Tech Stack
- **Spring Boot 3.5.6** (Java 21) 
- **PostgreSQL**
- **Apache Kafka**
- **Stripe API** - Payment Processing (Payment Intents, Connect, Webhooks)
- **Netflix Eureka** - Service Discovery
- **Docker & Kubernetes** - Deployment & Containerization
- **Micrometer, OpenTelemetry, Alloy, Loki, Prometheus, Tempo, Grafana** - Observability
- **OpenAPI/Swagger** - API Documentation

## For Further Information
Refer to [Payment Service Wiki Page](https://github.com/Mazaadak/.github/wiki/Payment-Service).
