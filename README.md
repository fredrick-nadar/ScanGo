# ScanGo - File Sharing with QR Codes

ScanGo is a responsive web application built with Spring Boot that allows users to upload files and share them via QR codes. Recipients can download the shared files by scanning the QR code with their mobile devices.

## Features

- Responsive web interface for both desktop and mobile devices
- Easy file upload with drag-and-drop support
- Automatic QR code generation for shared files
- Simple file management and download system
- H2 database for storing file metadata
- Support for file sizes up to 10GB

## Prerequisites

- Java 17 or higher
- Maven 3.6.x or higher

## Getting Started

1. Clone the repository
2. Navigate to the project directory

```
cd scango
```

3. Run the application using Maven

```
mvn spring-boot:run
```

4. Access the application at http://localhost:8080

## Global Deployment

To make your application accessible globally (from anywhere in the world):

### Option 1: Docker Deployment (Easiest)

This project includes Docker configuration for easy deployment:

1. Install Docker and Docker Compose on your server
2. Clone this repository
3. Run the deployment script:

```bash
chmod +x deploy.sh
./deploy.sh
```

4. Follow the prompts to configure your domain
5. Your application will be accessible at your domain/IP on port 8080
6. Share files with QR codes that will work globally from any location

### Option 2: Cloud Deployment (recommended)

1. Create an account on a cloud platform like AWS, Google Cloud, or Azure
2. Deploy your Spring Boot application to the platform
3. Update the `app.base-url` in `application.properties` with your domain name
4. Configure your server for large file uploads

Example for AWS Elastic Beanstalk:
```
# Install the AWS CLI and EB CLI
pip install awscli eb

# Initialize EB application
eb init

# Create an environment and deploy
eb create

# Update application.properties with your EB URL
app.base-url=http://your-app.elasticbeanstalk.com
```

### Option 3: Use a VPS (Virtual Private Server)

1. Rent a VPS from providers like DigitalOcean, Linode, or Vultr
2. Install Java and required dependencies
3. Upload and run your application
4. Configure your domain name and set it in `app.base-url`

### Option 4: Use a File Hosting Service API

If you don't want to host large files yourself:
1. Modify the application to use cloud storage services like AWS S3, Google Cloud Storage, or Azure Blob Storage
2. Generate temporary URLs for file downloads
3. Host only the application itself on a smaller server

## Usage

1. Open the application in your web browser
2. Choose a file to upload by clicking "Browse Files" or by dragging and dropping it onto the drop zone
3. Click "Upload File" to upload the file
4. Once uploaded, you will see a QR code and a direct download link
5. Share the QR code with anyone you want to share the file with
6. Recipients can scan the QR code with their mobile devices to download the file

## Configuration

Configuration options can be modified in the `application.properties` file:

- `server.port`: Change the port the application runs on (default: 8080)
- `app.base-url`: The base URL for generating download links (set to your public domain)
- `spring.servlet.multipart.max-file-size`: Maximum file size (default: 10GB)
- `spring.servlet.multipart.max-request-size`: Maximum request size (default: 10GB)
- `file.upload-dir`: Directory where uploaded files will be stored (default: ./uploads)
- `server.tomcat.connection-timeout`: Connection timeout in milliseconds (default: 600000)

## Technologies Used

- Spring Boot
- Spring Data JPA
- Thymeleaf
- H2 Database
- ZXing (for QR code generation)
- Bootstrap 5

## License

This project is licensed under the MIT License - see the LICENSE file for details. 