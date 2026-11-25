@echo off
cd /d "%~dp0\.."
for %%f in (lib\mysql-connector-j-*.jar) do set JAR=%%f
if "%JAR%"=="" (echo Put mysql-connector-j-8.x.jar into the lib folder first. & exit /b 1)
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\*.java > sources.txt
javac -d out -cp "%JAR%" @sources.txt || exit /b 1
del sources.txt
java -cp "out;%JAR%" com.emall.Main
