#!/bin/bash

echo "Starting WebClient Demo on port 8092..."
export ACTIVE_PROFILE=webclient
mvn spring-boot:run -Dspring-boot.run.profiles=webclient