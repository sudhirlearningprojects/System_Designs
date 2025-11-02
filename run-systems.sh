#!/bin/bash

# System Designs Runner Script

echo "Available System Designs:"
echo "1. Parking Lot Management System (port 8080)"
echo "2. Dropbox Clone System (port 8081)" 
echo "3. Payment Service System (port 8082)"
echo "4. Job Scheduler System (port 8083)"
echo "5. Digital Payment Platform (port 8084)"
echo ""

case "$1" in
  "parkinglot")
    echo "Starting Parking Lot Management System..."
    mvn spring-boot:run -Dspring-boot.run.profiles=parkinglot
    ;;
  "dropbox")
    echo "Starting Dropbox Clone System..."
    mvn spring-boot:run -Dspring-boot.run.profiles=dropbox
    ;;
  "payment")
    echo "Starting Payment Service System..."
    mvn spring-boot:run -Dspring-boot.run.profiles=payment
    ;;
  "jobscheduler")
    echo "Starting Job Scheduler System..."
    mvn spring-boot:run -Dspring-boot.run.profiles=jobscheduler
    ;;
  *)
    echo "Usage: $0 {parkinglot|dropbox|payment|jobscheduler|digitalpayment}"
    echo ""
    echo "Examples:"
    echo "  ./run-systems.sh parkinglot"
    echo "  ./run-systems.sh dropbox"
    echo "  ./run-systems.sh payment"
    echo "  ./run-systems.sh jobscheduler"
    echo "  ./run-systems.sh digitalpayment"
    exit 1
    ;;
esac