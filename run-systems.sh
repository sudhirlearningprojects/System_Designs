#!/bin/bash

# System Designs Runner Script
# Usage: ./run-systems.sh <system-name>

SYSTEM=$1

if [ -z "$SYSTEM" ]; then
    echo "Usage: $0 <system-name>"
    echo "Available systems:"
    echo "  parkinglot      - Port 8080"
    echo "  dropbox         - Port 8081"
    echo "  payment         - Port 8082"
    echo "  jobscheduler    - Port 8083"
    echo "  digitalpayment  - Port 8084"
    echo "  ticketbooking   - Port 8086"
    echo "  instagram       - Port 8087"
    echo "  ratelimiter     - Port 8088"
    echo "  notification    - Port 8089"
    echo "  uber            - Port 8090"
    echo "  googledocs      - Port 8091"
    echo "  urlshortener    - Port 8092"
    echo "  whatsapp        - Port 8093"
    echo "  cloudflare      - Port 8094"
    exit 1
fi

case $SYSTEM in
    "parkinglot")
        echo "Starting Parking Lot Management System on port 8080..."
        mvn spring-boot:run -Dspring-boot.run.profiles=parkinglot
        ;;
    "dropbox")
        echo "Starting Dropbox Clone on port 8081..."
        mvn spring-boot:run -Dspring-boot.run.profiles=dropbox
        ;;
    "payment")
        echo "Starting Payment Service on port 8082..."
        mvn spring-boot:run -Dspring-boot.run.profiles=payment
        ;;
    "jobscheduler")
        echo "Starting Job Scheduler on port 8083..."
        mvn spring-boot:run -Dspring-boot.run.profiles=jobscheduler
        ;;
    "digitalpayment")
        echo "Starting Digital Payment Platform on port 8084..."
        mvn spring-boot:run -Dspring-boot.run.profiles=digitalpayment
        ;;
    "ticketbooking")
        echo "Starting Ticket Booking Platform on port 8086..."
        mvn spring-boot:run -Dspring-boot.run.profiles=ticketbooking
        ;;
    "instagram")
        echo "Starting Instagram Clone on port 8087..."
        mvn spring-boot:run -Dspring-boot.run.profiles=instagram
        ;;
    "ratelimiter")
        echo "Starting API Rate Limiter on port 8088..."
        mvn spring-boot:run -Dspring-boot.run.profiles=ratelimiter
        ;;
    "notification")
        echo "Starting Notification System on port 8089..."
        mvn spring-boot:run -Dspring-boot.run.profiles=notification
        ;;
    "uber")
        echo "Starting Uber Clone on port 8090..."
        mvn spring-boot:run -Dspring-boot.run.profiles=uber
        ;;
    "googledocs")
        echo "Starting Google Docs Clone on port 8091..."
        mvn spring-boot:run -Dspring-boot.run.profiles=googledocs
        ;;
    "urlshortener")
        echo "Starting URL Shortener on port 8092..."
        mvn spring-boot:run -Dspring-boot.run.profiles=urlshortener
        ;;
    "whatsapp")
        echo "Starting WhatsApp Messenger on port 8093..."
        mvn spring-boot:run -Dspring-boot.run.profiles=whatsapp
        ;;
    "cloudflare")
        echo "Starting Cloudflare Clone on port 8094..."
        mvn spring-boot:run -Dspring-boot.run.profiles=cloudflare
        ;;
    *)
        echo "Unknown system: $SYSTEM"
        echo "Run '$0' without arguments to see available systems"
        exit 1
        ;;
esac