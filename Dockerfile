FROM docker.io/clojure:temurin-21-tools-deps-jammy AS build-clojure

WORKDIR /build

COPY deps.edn /build/
RUN clj -Stree && clj -Stree -A:build && clj -X:deps prep

COPY . /build

RUN clj -T:build uber

# Final stage
FROM docker.io/eclipse-temurin:21

WORKDIR /app

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y sqlite3 && \
    apt-get clean

#TODO: @ieugen: implement build distribution
COPY --from=build-clojure /build/target/efactura-mea*.jar /app/efactura-mea.jar
COPY --from=build-clojure /build/public /app/public
COPY --from=build-clojure /build/templates /app/templates
COPY --from=build-clojure /build/conf /app/conf
COPY --from=build-clojure /build/dev-resources /app/dev-resources

CMD ["java", "-jar", "/app/efactura-mea.jar"]