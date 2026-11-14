@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo Java JDK was not found. Install JDK 21 or later and reopen this terminal.
  pause
  exit /b 1
)
if not exist build\classes mkdir build\classes
del build\sources.txt 2>nul
(for /r src\main\java %%f in (*.java) do (
  set "source=%%f"
  set "source=!source:\=/!"
  echo "!source!"
)) > build\sources.txt
javac --add-modules jdk.httpserver -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 (
  del build\sources.txt 2>nul
  echo Build failed. Review the compiler errors above.
  pause
  exit /b 1
)
del build\sources.txt 2>nul
if not exist build\classes\public mkdir build\classes\public
xcopy /E /I /Y src\main\resources\public build\classes\public >nul
jar --create --file build\online-examination-web.jar --main-class com.exam.web.ExamWebApplication -C build\classes .
echo.
echo Starting Online Examination System...
echo Open http://localhost:8080 in your browser.
echo Press Ctrl+C to stop the server.
java --add-modules jdk.httpserver -jar build\online-examination-web.jar
