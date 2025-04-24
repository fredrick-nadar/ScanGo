@echo off
echo ScanGo - Global Access Setup with ngrok
echo ----------------------------------------

:: Check if ngrok is installed
where ngrok >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ngrok is not installed. Please install it first:
    echo Visit https://ngrok.com/download, sign up, and follow instructions
    exit /b 1
)

:: Start Spring Boot application in new window
echo Starting Spring Boot application...
start "Spring Boot" cmd /c "mvn spring-boot:run"

:: Wait for Spring Boot to start
echo Waiting for Spring Boot to initialize (15 seconds)...
timeout /t 15 /nobreak >nul

:: Start ngrok in a new window
echo Starting ngrok tunnel...
start "ngrok" cmd /c "ngrok http 8080"

:: Wait for ngrok to get URL
echo Getting ngrok public URL (5 seconds)...
timeout /t 5 /nobreak >nul

echo ============================================================================
echo Your application should now be globally accessible through the ngrok URL
echo Please check the ngrok window to see your public URL (https://xxxx.ngrok.io)
echo ============================================================================
echo.
echo IMPORTANT STEPS:
echo 1. Look at the ngrok window and copy the https://xxxx.ngrok.io URL
echo 2. Create a file named "ngrok-config.properties" with these contents:
echo.
echo # Temporary configuration - do not commit
echo app.base-url=https://xxxx.ngrok.io
echo file.upload-dir=./uploads
echo spring.profiles.active=ngrok
echo.
echo 3. Restart your application with:
echo    mvn spring-boot:run -Dspring-boot.run.arguments="--spring.config.location=classpath:/application.properties,ngrok-config.properties"
echo.
echo 4. Now your QR codes will work from anywhere in the world!
echo.
echo NOTE: This URL is temporary and will change if you restart ngrok
echo For permanent access, you'll need to deploy to a public server.
echo.
echo Press any key to continue...
pause >nul 