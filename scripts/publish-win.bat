@echo off
setlocal


REM Get the script's directory and switch to the project root
pushd "%~dp0.."

REM -- Configuration --
SET "JAR_PATH=target\code-assistant-backend-0.0.1-SNAPSHOT.jar"
SET "REMOTE_USER=root"
SET "REMOTE_HOST=8.148.146.195"
SET "REMOTE_DIR=/lfs/project"
SET "REMOTE_JAR_NAME=code-assistant-backend-0.0.1-SNAPSHOT.jar"

REM --- Checks ---

REM Check if the password environment variable exists
IF NOT DEFINED S_PASSWORD (
    echo [ERROR] S_PASSWORD environment variable not found.
    echo Please set the password first: set S_PASSWORD=your_password
    goto:eof
)

REM Check if the JAR file exists
IF NOT EXIST "%JAR_PATH%" (
    echo [ERROR] JAR file not found: %JAR_PATH%
    echo Please build the project first using "mvn clean package".
    goto:eof
)


REM --- Deployment ---

echo Uploading JAR file to %REMOTE_HOST%...
pscp  -batch -pw %S_PASSWORD% "%JAR_PATH%" "%REMOTE_USER%@%REMOTE_HOST%:%REMOTE_DIR%/%REMOTE_JAR_NAME%"
IF ERRORLEVEL 1 (
    echo [ERROR] JAR file upload failed.
    goto:eof
)
echo Upload complete.

echo ----------------------------------------------------

echo Executing deployment script on %REMOTE_HOST%...
plink  -batch -pw %S_PASSWORD% "%REMOTE_USER%@%REMOTE_HOST%" "sh %REMOTE_DIR%/deploy.sh %REMOTE_DIR%/%REMOTE_JAR_NAME%"
IF ERRORLEVEL 1 (
    echo [ERROR] Failed to execute remote deployment script.
    goto:eof
)

echo ----------------------------------------------------
echo Deployment script finished.

popd
endlocal