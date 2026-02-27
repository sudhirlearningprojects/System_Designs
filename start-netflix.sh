#!/bin/bash

echo "Starting Netflix System..."

# Build the project
echo "Building project..."
mvn clean compile -DskipTests

if [ $? -eq 0 ]; then
    echo "Build successful!"
    
    # Run the application
    echo "Starting application..."
    mvn spring-boot:run
else
    echo "Build failed!"
    exit 1
fi