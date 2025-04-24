#!/bin/bash

# Check if ngrok is installed
if ! command -v ngrok &> /dev/null; then
    echo "ngrok is not installed. Please install it first:"
    echo "Visit https://ngrok.com/download, sign up, and follow instructions"
    exit 1
fi

# Start Spring Boot application in the background
echo "Starting Spring Boot application..."
mvn spring-boot:run &
SPRING_PID=$!

# Wait for Spring Boot to start
echo "Waiting for Spring Boot to initialize (15 seconds)..."
sleep 15

# Start ngrok in the background
echo "Starting ngrok tunnel..."
ngrok http 8080 &
NGROK_PID=$!

# Wait for ngrok to get URL
echo "Getting ngrok public URL (5 seconds)..."
sleep 5

# Get the ngrok URL
NGROK_URL=$(curl -s http://localhost:4040/api/tunnels | grep -o "https://[^\"]*")

if [ -z "$NGROK_URL" ]; then
    echo "Failed to get ngrok URL. Please check if ngrok is running properly."
    kill $SPRING_PID
    kill $NGROK_PID
    exit 1
fi

echo "=================================================="
echo "✅ Your application is now globally accessible at:"
echo "   $NGROK_URL"
echo "=================================================="
echo "QR codes generated will now work from anywhere in the world"
echo ""
echo "To stop, press Ctrl+C"
echo ""
echo "IMPORTANT: This URL is temporary and will change if you restart ngrok"
echo "For permanent access, you'll need to deploy to a public server"

# Create a temporary properties file with the ngrok URL
echo "# Temporary configuration - do not commit" > ngrok-config.properties
echo "app.base-url=$NGROK_URL" >> ngrok-config.properties
echo "file.upload-dir=./uploads" >> ngrok-config.properties
echo "spring.profiles.active=ngrok" >> ngrok-config.properties

echo ""
echo "Config file created: ngrok-config.properties"
echo "For your next application start, use this command:"
echo "mvn spring-boot:run -Dspring-boot.run.arguments=\"--spring.config.location=classpath:/application.properties,ngrok-config.properties\""
echo ""

# Wait for user to press Ctrl+C
trap "kill $SPRING_PID; kill $NGROK_PID; echo 'Shutting down...'" INT
wait 