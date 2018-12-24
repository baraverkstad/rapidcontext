ARG BASEREPO=amd64/openjdk

FROM ${BASEREPO}:8-jdk-alpine AS build
ARG VERSION
RUN apk update && \
    apk add --no-cache \
        apache-ant
ADD . /opt/rapidcontext/
RUN cd /opt/rapidcontext/ && \
    ant -Dbuild.version=${VERSION:-$(date +%Y%m%d)} compile doc

FROM ${BASEREPO}:8-jdk-alpine
COPY --from=build /opt/rapidcontext/doc.zip /opt/rapidcontext/
COPY --from=build /opt/rapidcontext/lib/ /opt/rapidcontext/lib/
COPY --from=build /opt/rapidcontext/plugin/ /opt/rapidcontext/plugin/
RUN mkdir /opt/plugin && \
    ln -s /opt/rapidcontext/lib/rapidcontext-*.jar /opt/rapidcontext/lib/rapidcontext.jar
WORKDIR /opt/rapidcontext/
EXPOSE 80/tcp
CMD ["java", "-Djava.util.logging.config.file=lib/logging.properties", \
     "-jar", "lib/rapidcontext.jar", "--server", "--port", "80", "--local", "/opt"]
