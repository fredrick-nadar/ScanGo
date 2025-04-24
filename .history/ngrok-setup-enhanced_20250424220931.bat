@echo off
echo ScanGo - Enhanced Global Access Setup with ngrok
echo -----------------------------------------------

:: Check if ngrok is installed
where ngrok >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ngrok is not installed. Please install it first:
    echo Visit https://ngrok.com/download, sign up, and follow instructions
    exit /b 1
)

:: Check if ngrok is authenticated
ngrok config check >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ngrok is not authenticated. Please enter your authtoken:
    set /p NGROK_AUTHTOKEN="Enter your ngrok authtoken: "
    ngrok authtoken %NGROK_AUTHTOKEN%
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to authenticate ngrok. Please check your authtoken.
        exit /b 1
    )
)

:: Create uploads directory if it doesn't exist
if not exist "uploads" mkdir uploads

:: Start ngrok in a new window
echo Starting ngrok tunnel...
start "ngrok" cmd /c "ngrok http 8080"

:: Wait for ngrok to start and get the URL
echo Waiting for ngrok to initialize...
set NGROK_URL=
for /L %%i in (1,1,30) do (
    timeout /t 2 /nobreak >nul
    for /f "tokens=2 delims=:" %%a in ('curl -s http://localhost:4040/api/tunnels ^| findstr "public_url"') do (
        set NGROK_URL=%%a
        set NGROK_URL=!NGROK_URL:"=!
        set NGROK_URL=!NGROK_URL:,=!
        goto :url_found
    )
)
:url_found

if "%NGROK_URL%"=="" (
    echo Failed to get ngrok URL. Please check if ngrok is running properly.
    exit /b 1
)

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