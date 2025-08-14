#!/bin/bash

# Set the working directory
cd /opt/app

echo "Initial build..."
# Perform an initial build to ensure everything is ready
./gradlew :bootstrap-standalone:assemble --no-daemon

# Start the application in a loop
while true; do
  echo "Starting application..."
  # Run the application in the background and store its PID
  ./gradlew :bootstrap-standalone:run --no-daemon &
  PID=$!

  echo "Application started with PID $PID. Watching for file changes..."

  # Watch for changes in the source directories.
  inotifywait -q -r -e modify,create,delete,move \
    ./core/src \
    ./bootstrap/standalone/src

  echo "File change detected! Restarting application..."
  # Kill the previous application process before looping
  kill $PID
  # Wait for the process to be fully killed
  wait $PID 2>/dev/null
done
