@echo off
echo Starting Productivity Dashboard with Docker Compose...
docker-compose up --build -d
echo.
echo Application is starting...
echo MySQL will be available on port 3306
echo Web interface will be available at http://localhost:8080
echo.
echo To view logs, run: docker-compose logs -f
echo To stop the application, run: stop.bat
