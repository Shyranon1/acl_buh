#!/bin/bash

echo "Starting ACL Project..."

# Create bin directory if it doesn't exist
if [ ! -d "bin" ]; then
    mkdir bin
fi

# Clean old class files in client/ (cleanup step)
if ls client/*.class 1> /dev/null 2>&1; then
    rm client/*.class
fi

# Compile Java Client (Target Java 8 for compatibility)
echo "Compiling Client..."
# -d bin : Output compiled files to bin folder
javac -d bin --release 8 -Xlint:-options -encoding UTF-8 client/*.java
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Compile C++ Server
echo "Compiling Server..."
# No -lws2_32 needed for Linux
g++ server/server_tsp.cpp -o server/server
if [ $? -ne 0 ]; then
    echo "Server compilation failed!"
    exit 1
fi

# Start the C++ Server in background
echo "Launching Server..."
./server/server &
SERVER_PID=$!

# Ensure server acts as a background process but we kill it when script exits
trap "kill $SERVER_PID 2> /dev/null" EXIT

# Short delay to ensure server is ready
sleep 2

# Start the Java Client
echo "Launching Client..."
# Use -cp bin to include the bin folder in classpath
java -cp bin client.ApplicationLivraison

# When Java client exits, the script ends and the trap kills the server
echo "Done."
