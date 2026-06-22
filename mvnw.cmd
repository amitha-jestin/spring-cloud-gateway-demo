@echo off
SET MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists
SET MAVEN_VERSION=3.9.6
SET MAVEN_DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip
SET MAVEN_INSTALL_DIR=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%

IF NOT EXIST "%MAVEN_INSTALL_DIR%" (
    echo Downloading Apache Maven %MAVEN_VERSION%...
    mkdir "%MAVEN_HOME%"
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_DIST_URL%' -OutFile '%MAVEN_HOME%\maven.zip'"
    powershell -Command "Expand-Archive -Path '%MAVEN_HOME%\maven.zip' -DestinationPath '%MAVEN_HOME%'"
    del "%MAVEN_HOME%\maven.zip"
    echo Maven %MAVEN_VERSION% installed.
)
"%MAVEN_INSTALL_DIR%\bin\mvn.cmd" %*
