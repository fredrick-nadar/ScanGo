@echo off
echo ScanGo - Enhanced Global Access Setup with ngrok
echo -----------------------------------------------

:: Set ngrok authtoken
echo Setting ngrok authtoken...
ngrok authtoken 2t1bmDHElJinFHZfK7E3pyaSG5z_7dUU3krqPnhuRfCKBdaGm

:: Check if ngrok is installed
where ngrok >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ngrok is not installed. Please install it first:
    echo Visit https://ngrok.com/download, sign up, and follow instructions
    exit /b 1
)

:: Create uploads directory if it doesn't exist
if not exist "uploads" mkdir uploads

:: Start ngrok in a new window
echo Starting ngrok tunnel...
start "ngrok" cmd /c "ngrok http 8080"

:: Wait for ngrok to start
echo Waiting for ngrok to initialize (10 seconds)...
timeout /t 10 /nobreak >nul

:: Prompt user to manually copy the ngrok URL
echo ======================================================================
echo IMPORTANT: Look at the ngrok window and copy the HTTPS URL (e.g. https://xxxx-xxxx.ngrok-free.app)
echo ======================================================================
echo.
set /p NGROK_URL="Enter the ngrok URL from the window (https://...): "

:: Create ngrok properties file with the URL
echo Creating configuration with the ngrok URL...
echo # Temporary configuration - do not commit > ngrok-config.properties
echo app.base-url=%NGROK_URL% >> ngrok-config.properties
echo file.upload-dir=uploads >> ngrok-config.properties
echo spring.profiles.active=ngrok >> ngrok-config.properties

echo.
echo Configuration created with URL: %NGROK_URL%
echo.
echo Press any key to start the Spring Boot application...
pause >nul

:: Start Spring Boot with ngrok profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.config.location=classpath:/application.properties,ngrok-config.properties" 