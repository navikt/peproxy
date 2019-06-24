FROM navikt/java:11

COPY target/peproxy*.jar /app/app.jar
