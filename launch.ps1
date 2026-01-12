Write-Host "Starting ACL Project..."

# Create bin directory if it doesn't exist
if (!(Test-Path "bin")) { mkdir bin }

# Clean old class files in client/ (cleanup step)
if (Test-Path "client/*.class") {
    Remove-Item "client/*.class"
}

# Compile Java Client (Target Java 8 for compatibility)
Write-Host "Compiling Client..."
# -d bin : Output compiled files to bin folder
javac -d bin --release 8 -Xlint:-options -encoding UTF-8 client/*.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    Pause
    exit
}

# Start the C++ Server
Write-Host "Launching Server..."
Start-Process -FilePath ".\server\server.exe"

# Short delay to ensure server is ready
Start-Sleep -Seconds 2

# Start the Java Client
Write-Host "Launching Client..."
# Use -cp bin to include the bin folder in classpath
Start-Process -FilePath "java" -ArgumentList "-cp bin client.ApplicationLivraison"

Write-Host "Done! Server and Client should be running."
