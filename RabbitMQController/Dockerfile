FROM amazoncorretto:11.0.17

ENV EUREKA_HOST=eureka
ENV auth_secret=bruh
ENV RABBITMQ_HOST=rabbitMQcapstone
ENV RABBITMQ_PORT=5672
ENV RABBITMQ_USER=exchangeuser
ENV RABBITMQ_PASS=exchangepass23a@
ENV RABBITMQ_VHOST=/

COPY ./target/RabbitMQController-0.0.1-SNAPSHOT.jar /usr/local/lib/RabbitAPI.jar

ENTRYPOINT ["java","-jar","/usr/local/lib/RabbitAPI.jar"]

EXPOSE 8080