# MCXboxBroadcast Manager
This is the manager for the MCXboxBroadcast project, it is used to manage the accounts and the server details that are broadcasted through a web page.

The manager is designed for bigger deployments and not recommended for smaller ones as it requires a MongoDB instance to store the data.

## Installation
1. Download the latest release file `MCXboxBroadcastManager.jar`
2. Set the `SPRING_DATA_MONGODB_URI` environment variable to the MongoDB connection string (or change the settings in the `application.yaml`)
3. Start the jar file using `java -jar MCXboxBroadcastManager.jar`
4. Open the web page, port 8082 by default
5. Login with the default credentials `admin:password`

## Docker
The image can be found at `ghcr.io/rtm516/mcxboxbroadcast-manager:latest`

There is also a prebuilt Docker compose file that can be used to run the manager and the mongodb together. This can be found at [`docker-compose.yml`](docker-compose.yml)

## Additional Configuration
Configuration can be made in the `application.yaml` mounted into `/opt/app/config` (supports a range of spring properties)

See [application.yaml](src/main/resources/application.yaml) for the default configuration
