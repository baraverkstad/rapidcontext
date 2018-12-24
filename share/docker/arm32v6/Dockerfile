FROM openjdk:8-jdk-alpine AS build
ARG VERSION
RUN apk update && \
    apk add --no-cache \
        apache-ant
ADD . /opt/rapidcontext/
RUN cd /opt/rapidcontext/ && \
    ant -Dbuild.version=${VERSION:-$(date +%Y%m%d)} compile doc && \
    cd /opt/rapidcontext/lib/ && \
    ln -s rapidcontext-*.jar rapidcontext.jar && \
    mkdir /opt/plugin


FROM arm32v6/openjdk:8-jdk-alpine
COPY --from=build /opt/rapidcontext/doc.zip /opt/rapidcontext/
COPY --from=build /opt/rapidcontext/lib/ /opt/rapidcontext/lib/
COPY --from=build /opt/rapidcontext/plugin/ /opt/rapidcontext/plugin/
COPY --from=build /opt/plugin/ /opt/plugin/
WORKDIR /opt/rapidcontext/
EXPOSE 80/tcp
ENV JAVA_TOOL_OPTIONS="-Djava.util.logging.config.file=lib/logging.properties -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
CMD ["java", "-jar", "lib/rapidcontext.jar", "--server", "--port", "80", "--local", "/opt"]
