FROM amazoncorretto:11.0.17

COPY ./target/CapstoneGateway-0.0.1-SNAPSHOT.jar /usr/local/lib/CapstoneGateway.jar
COPY ./src/main/resources/certificates/ /usr/local/lib/src/main/resources/certificates/


ENTRYPOINT ["java","-jar","/usr/local/lib/CapstoneGateway.jar"]

EXPOSE 80