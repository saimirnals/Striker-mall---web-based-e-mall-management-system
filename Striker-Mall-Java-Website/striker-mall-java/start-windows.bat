@echo off
cd /d "%~dp0"
echo ==============================
echo      STRIKER MALL - JAVA
 echo ==============================
where javac >nul 2>nul
if errorlevel 1 (
 echo Java JDK 17 or newer is required. Download from https://adoptium.net/
 pause
 exit /b 1
)
if not exist build\classes mkdir build\classes
javac -encoding UTF-8 -cp "lib\hsqldb.jar" -d build\classes src\main\java\com\strikermall\Json.java src\main\java\com\strikermall\Db.java src\main\java\com\strikermall\Server.java
if errorlevel 1 (
 echo Compilation failed. Check that JDK 17 or newer is installed.
 pause
 exit /b 1
)
echo Website link: http://localhost:8081
start "" "http://localhost:8081"
java -cp "build\classes;lib\hsqldb.jar" com.strikermall.Server
pause
