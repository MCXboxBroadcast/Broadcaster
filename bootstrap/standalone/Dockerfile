FROM eclipse-temurin:21-jre

ENV HOME="/opt/app"
RUN groupmod -n app ubuntu && usermod -l app -d "${HOME}" -m ubuntu
ENTRYPOINT ["/bin/bash", "-c", "chown -R app:app . && exec setpriv --reuid=app --regid=app --init-groups \"$@\"", "--"]

WORKDIR /opt/app/config

COPY bootstrap/standalone/build/libs/MCXboxBroadcastStandalone.jar /opt/app/MCXboxBroadcastStandalone.jar

CMD ["java", "-jar", "/opt/app/MCXboxBroadcastStandalone.jar"]
