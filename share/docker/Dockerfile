FROM alpine:3.22
ARG VERSION
ADD rapidcontext-${VERSION}.zip /opt/
RUN apk --no-cache --quiet upgrade && \
    apk --no-cache --quiet add openjdk21-jre && \
    mkdir -p /opt/local/plugin && \
    cd /opt && \
    unzip -q rapidcontext-${VERSION}.zip && \
    rm rapidcontext-${VERSION}.zip && \
    ln -s rapidcontext-${VERSION} rapidcontext && \
    mv rapidcontext/lib/rapidcontext-${VERSION}.jar rapidcontext/lib/rapidcontext.jar
WORKDIR /opt/rapidcontext/
# EXPOSE 80/tcp
# VOLUME /opt/local
ENV JAVA_TOOL_OPTIONS="-Djava.util.logging.config.file=lib/logging.properties -Dorg.eclipse.jetty.server.Request.maxFormContentSize=1000000"
CMD ["java", "-jar", "lib/rapidcontext.jar", "--server", "--port", "80", "--local", "/opt/local"]
# HEALTHCHECK --timeout=5s CMD wget -q --spider http://localhost/rapidcontext/status
