#!/bin/bash

# System runner script
SYSTEM=$1

if [ -z "$SYSTEM" ]; then
    echo "Usage: $0 <system>"
    echo "Available systems: parkinglot, dropbox, payment, jobscheduler, digitalpayment, ticketbooking, instagram, ratelimiter"
    exit 1
fi

case $SYSTEM in
    "parkinglot")
        echo "🚗 Starting Parking Lot Management System on port 8080..."
        mvn spring-boot:run -Dspring-boot.run.profiles=parkinglot
        ;;
    "dropbox")
        echo "☁️ Starting Dropbox Clone on port 8081..."
        mvn spring-boot:run -Dspring-boot.run.profiles=dropbox
        ;;
    "payment")
        echo "💳 Starting Payment Service on port 8082..."
        mvn spring-boot:run -Dspring-boot.run.profiles=payment
        ;;
    "jobscheduler")
        echo "⏰ Starting Job Scheduler on port 8083..."
        mvn spring-boot:run -Dspring-boot.run.profiles=jobscheduler
        ;;
    "digitalpayment")
        echo "📱 Starting Digital Payment Platform on port 8084..."
        mvn spring-boot:run -Dspring-boot.run.profiles=digitalpayment
        ;;
    "ticketbooking")
        echo "🎫 Starting Ticket Booking Platform on port 8086..."
        mvn spring-boot:run -Dspring-boot.run.profiles=ticketbooking
        ;;
    "instagram")
        echo "📸 Starting Instagram Clone on port 8087..."
        mvn spring-boot:run -Dspring-boot.run.profiles=instagram
        ;;
    "ratelimiter")
        echo "🛡️ Starting API Rate Limiter on port 8088..."
        mvn spring-boot:run -Dspring-boot.run.profiles=ratelimiter
        ;;
    *)
        echo "❌ Unknown system: $SYSTEM"
        echo "Available systems: parkinglot, dropbox, payment, jobscheduler, digitalpayment, ticketbooking, instagram, ratelimiter"
        exit 1
        ;;
esac