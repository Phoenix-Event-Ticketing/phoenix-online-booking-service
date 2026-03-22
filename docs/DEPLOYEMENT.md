# Booking Service Deployment Guide

This document explains how to prepare and deploy the Phoenix Online Booking Service.

## 1. Container Registry

The Docker image can be published to GitHub Container Registry:

- ghcr.io/<your-github-username>/phoenix-online-booking-service:latest

The repository should be public for assignment submission.

## 2. Required Environment Variables

Set these in your target deployment platform:

- APP_PORT=8083
- MONGODB_URI=<your-managed-or-cloud-mongodb-uri>
- MONGODB_DATABASE=booking_db
- JWT_SECRET=<strong-secret>
- JWT_ISSUER=phoenix-online-auth
- INTERNAL_SERVICE_PERMISSIONS_JSON={"payment-service":["PAYMENT_CALLBACK"],"inventory-service":["EXPIRE_BOOKING"]} (callers send `X-Internal-Service-Id`; no secrets)
- BOOKING_SERVICE_CALLBACK_BASE_URL=<public-base-url-of-booking-service>
- EVENT_SERVICE_BASE_URL=<public-url-of-event-service>
- INVENTORY_SERVICE_BASE_URL=<public-url-of-inventory-service>
- PAYMENT_SERVICE_BASE_URL=<public-url-of-payment-service>
- APP_ENV=prod
- LOG_LEVEL=INFO

## 3. Health Endpoint

Use this endpoint for cloud/container health checks:

- GET /actuator/health

Optional service information endpoint:

- GET /actuator/info

## 4. Suggested Cloud Deployment Targets

The assignment allows managed container platforms such as:

- Azure Container Apps
- AWS ECS / Fargate
- Managed Kubernetes services

A good simple option is:

- build image with GitHub Actions
- push image to GHCR
- deploy that image to your chosen managed container platform

## 5. Demo Checklist

Before the final demo, verify:

- the service is reachable publicly
- MongoDB connection works
- Event / Inventory / Payment URLs point to the real teammate services
- Swagger UI opens
- health endpoint returns UP
- at least one real inter-service flow works
- logs show request_id / trace_id
- CI pipeline passes
- SonarCloud scan is visible if configured
