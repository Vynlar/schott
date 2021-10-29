FROM openjdk:8-alpine

COPY target/uberjar/schott.jar /schott/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/schott/app.jar"]
