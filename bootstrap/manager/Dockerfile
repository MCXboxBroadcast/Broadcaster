FROM eclipse-temurin:21-alpine

RUN adduser -h /opt/app -H -D app

RUN mkdir -p /opt/app/config && \
    chown -R app:app /opt/app

USER app:app

WORKDIR /opt/app/config

VOLUME /opt/app/config/logs

COPY bootstrap/manager/build/libs/MCXboxBroadcastManager.jar /opt/app/MCXboxBroadcastManager.jar

CMD ["java", "-jar", "/opt/app/MCXboxBroadcastManager.jar"]
