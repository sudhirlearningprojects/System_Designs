#!/bin/bash

SYSTEM=$1

if [ -z "$SYSTEM" ]; then
    echo "Usage: ./run-systems.sh <system-name>"
    echo "Available systems: parkinglot, dropbox, payment, jobscheduler, digitalpayment, ticketbooking, instagram, ratelimiter, notification, uber, googledocs"
    exit 1
fi

case $SYSTEM in
    parkinglot)
        PORT=8080
        ;;
    dropbox)
        PORT=8081
        ;;
    payment)
        PORT=8082
        ;;
    jobscheduler)
        PORT=8083
        ;;
    digitalpayment)
        PORT=8084
        ;;
    ticketbooking)
        PORT=8086
        ;;
    instagram)
        PORT=8087
        ;;
    ratelimiter)
        PORT=8088
        ;;
    notification)
        PORT=8089
        ;;
    uber)
        PORT=8090
        ;;
    googledocs)
        PORT=8091
        ;;
    *)
        echo "Unknown system: $SYSTEM"
        exit 1
        ;;
esac

echo "Starting $SYSTEM on port $PORT..."
mvn spring-boot:run -Dspring-boot.run.profiles=$SYSTEM -Dserver.port=$PORT
