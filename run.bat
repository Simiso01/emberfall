@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\emberfall\*.java
if errorlevel 1 exit /b 1
java -cp out emberfall.EmberfallApp
