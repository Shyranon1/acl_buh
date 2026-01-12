Write-Host "Starting ACL Project..."

# Compile Java Client (Target Java 8 for compatibility)
Write-Host "Compiling Client..."
# We use --release 8 because the system has JDK 21 but JRE 1.8 active in PATH
javac --release 8 -Xlint:-options -encoding UTF-8 client/*.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    Pause
    exit
}

# Start the C++ Server
Write-Host "Launching Server..."
Start-Process -FilePath ".\server.exe"

# Short delay to ensure server is ready
Start-Sleep -Seconds 2

# Start the Java Client
Write-Host "Launching Client..."
# Use -cp . explicitly to include current directory in classpath
Start-Process -FilePath "java" -ArgumentList "-cp . client.ApplicationLivraison"

Write-Host "Done! Server and Client should be running."
