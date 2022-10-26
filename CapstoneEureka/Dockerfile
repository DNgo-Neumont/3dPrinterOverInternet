FROM amazoncorretto:11.0.17

COPY ./target/CapstoneEureka-0.0.1-SNAPSHOT.jar /usr/local/lib/EurekaServer.jar

ENTRYPOINT ["java","-jar","/usr/local/lib/EurekaServer.jar"]

EXPOSE 8761