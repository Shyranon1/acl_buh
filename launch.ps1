Write-Host "Starting ACL Project..."

# Start the C++ Server
Write-Host "Launching Server..."
Start-Process -FilePath ".\server.exe"

# Short delay to ensure server is ready
Start-Sleep -Seconds 2

# Start the Java Client
Write-Host "Launching Client..."
Start-Process -FilePath "java" -ArgumentList "client.ApplicationLivraison"

Write-Host "Done! Server and Client should be running."
