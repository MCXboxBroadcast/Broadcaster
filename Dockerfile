FROM eclipse-temurin:21-alpine

RUN adduser -h /opt/app -H -D app

RUN mkdir -p /opt/app/config && \
    chown -R app:app /opt/app

USER app:app

WORKDIR /opt/app/config

COPY bootstrap/standalone/build/libs/MCXboxBroadcastStandalone.jar /opt/app/MCXboxBroadcastStandalone.jar

CMD ["java", "-jar", "/opt/app/MCXboxBroadcastStandalone.jar"]
