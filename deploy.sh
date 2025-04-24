#!/bin/bash

# Build the application
echo "Building application..."
mvn clean package -DskipTests

# Configure the deployment
echo "Please enter your public domain or IP address (e.g., example.com or 123.456.789.10):"
read domain

# Update the docker-compose file with the domain
sed -i "s|http://your-public-domain-or-ip:8080|http://$domain:8080|g" docker-compose.yml

echo "Building and starting Docker containers..."
docker-compose up -d --build

echo "Deployment complete! Your application is now running at http://$domain:8080"
echo "Make sure your firewall allows traffic on port 8080 and your domain is pointed to this server."
echo ""
echo "To check logs, run: docker-compose logs -f"
echo "To stop the application, run: docker-compose down" 