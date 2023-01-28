FROM alpine:3.17
ARG VERSION
ADD rapidcontext-${VERSION}.zip /opt/
RUN apk add --no-cache openjdk17-jre && \
    mkdir -p /opt/local/plugin && \
    cd /opt && \
    unzip rapidcontext-${VERSION}.zip && \
    rm rapidcontext-${VERSION}.zip && \
    ln -s rapidcontext-${VERSION} rapidcontext && \
    mv rapidcontext/lib/rapidcontext-${VERSION}.jar rapidcontext/lib/rapidcontext.jar
WORKDIR /opt/rapidcontext/
# EXPOSE 80/tcp
# VOLUME /opt/local
ENV JAVA_TOOL_OPTIONS="-Djava.util.logging.config.file=lib/logging.properties"
ENTRYPOINT ["java", "-jar", "lib/rapidcontext.jar"]
CMD ["--server", "--port", "80", "--local", "/opt/local"]
# HEALTHCHECK --timeout=5s CMD wget -q --spider http://localhost/rapidcontext/status
