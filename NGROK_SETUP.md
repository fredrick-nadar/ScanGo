# Global File Sharing with ScanGo and ngrok

This guide explains how to make your local ScanGo application accessible globally without deploying to a server.

## What is ngrok?

ngrok is a tool that creates a secure tunnel from a public endpoint to a locally running web service. It allows you to expose your local web server to the internet without deploying it.

## Prerequisites

1. Install ngrok from https://ngrok.com/download
2. Sign up for a free ngrok account
3. Connect your ngrok client to your account with the authtoken (instructions provided after sign-up)

## Setup Instructions

### Simple Setup (Windows)

Run the enhanced setup script:
```
ngrok-setup-enhanced.bat
```

This script will:
1. Start ngrok automatically
2. Configure the application to use the ngrok URL
3. Start the Spring Boot application
4. Automatically detect and use the ngrok URL for all QR codes

### Manual Setup

#### For Windows Users:

1. Run the basic setup batch file:
   ```
   ngrok-setup.bat
   ```
2. Follow the instructions in the console
3. Copy the ngrok URL from the ngrok window (looks like `https://abcd1234.ngrok.io`)
4. Create a file named `ngrok-config.properties` with the following content:
   ```
   app.base-url=https://abcd1234.ngrok.io
   file.upload-dir=./uploads
   spring.profiles.active=ngrok
   ```
5. Restart your application with:
   ```
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.config.location=classpath:/application.properties,ngrok-config.properties"
   ```

#### For Linux/Mac Users:

1. Make the script executable:
   ```
   chmod +x ngrok-setup.sh
   ```
2. Run the script:
   ```
   ./ngrok-setup.sh
   ```
3. The script will automatically start your application and ngrok, and create the configuration file

## Advanced Configuration

For better performance and more control, you can use the provided `ngrok.yml` configuration file:

1. Edit the `ngrok.yml` file and add your authtoken
2. Choose your preferred region
3. Start ngrok with this configuration:
   ```
   ngrok start -config=ngrok.yml scango
   ```

## How It Works

1. The enhanced setup includes automatic URL detection
2. The application checks the ngrok URL every 10 minutes and updates if it changes
3. All QR codes will use the public ngrok URL instead of localhost
4. This allows people from anywhere in the world to access files shared through your application
5. The person scanning the QR code in London will be able to download the file from India without being on the same network

## Important Limitations

1. **Free ngrok sessions expire after 2 hours** - you'll need to restart ngrok
2. **The URL changes each time you restart ngrok** - but our application will detect this automatically
3. **Bandwidth and connection limits** - free tier has 1GB bandwidth per month and 40 connections per minute
4. **Security considerations** - your local machine is exposed to the internet, use with caution

## Going Beyond ngrok

If you need a more permanent solution:
1. Consider upgrading to a paid ngrok plan for static URLs and longer sessions
2. Use alternatives like Cloudflare Tunnel or PageKite
3. For a permanent solution, deploy to a cloud provider as described in the main README.md

## Troubleshooting

1. **Can't connect to ngrok**: Make sure you've authenticated with your authtoken
2. **Application not accessible**: Verify the ngrok tunnel is running and the URL is correct
3. **QR codes don't work**: Check if ngrok is running and the application has detected the URL
4. **Files too large**: Free ngrok plans have bandwidth limitations, consider upgrading 