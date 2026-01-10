FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y libstdc++6

RUN chmod 777 -R /tmp && chmod o+t -R /tmp

RUN useradd -d /opt/app -m app

RUN mkdir -p /opt/app/config && \
    chown -R app:app /opt/app

USER app:app

WORKDIR /opt/app/config

COPY bootstrap/standalone/build/libs/MCXboxBroadcastStandalone.jar /opt/app/MCXboxBroadcastStandalone.jar

CMD ["java", "-jar", "/opt/app/MCXboxBroadcastStandalone.jar"]
