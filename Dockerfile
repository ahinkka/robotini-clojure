FROM clojure:openjdk-11-lein-slim-buster

ENV NO_DISPLAY=true
RUN mkdir /app
WORKDIR /app
COPY src ./src
COPY project.clj .
RUN lein uberjar

ENTRYPOINT ["/usr/local/openjdk-11/bin/java", "-XX:+UseG1GC", "-XX:InitialHeapSize=256m", "-XX:MaxHeapSize=256m", "-XX:MaxGCPauseMillis=10", "-jar", "target/uberjar/robotini-clojure-0.1.0-SNAPSHOT-standalone.jar"]
