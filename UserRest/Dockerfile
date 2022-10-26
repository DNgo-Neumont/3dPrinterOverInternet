FROM amazoncorretto:11.0.17

ENV EUREKA_HOST=eureka
ENV DB_HOST=userSQLDb
ENV DB_PORT=3306
ENV DB_USER=capstone-user
ENV DB_PASSWORD=Nu200139254
ENV auth_secret=bruh

COPY ./target/UserRest-0.0.1-SNAPSHOT.jar /usr/local/lib/UserAPI.jar

ENTRYPOINT ["java","-jar","/usr/local/lib/UserAPI.jar"]

EXPOSE 8080