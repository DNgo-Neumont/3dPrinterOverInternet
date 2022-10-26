FROM amazoncorretto:11.0.17

ENV azure_connection_params_string=DefaultEndpointsProtocol=https;AccountName=simplprintgcode;AccountKey=qpCNkGEFSRHA6qzpb9Smy19OmYqpCJnk3XRE7ucedjQiZ0Qsn5PZ/V8YUwD1zXY1ASJUkHILBjLP+AStF+8cIQ==;EndpointSuffix=core.windows.net;
ENV azure_connect_url=https://simplprintgcode.file.core.windows.net;azure_share_name=gcode-datashare
ENV auth_secret=bruh
ENV EUREKA_HOST=eureka

COPY ./target/FileRest-0.0.1-SNAPSHOT.jar /usr/local/lib/FileAPI.jar

ENTRYPOINT ["java","-jar","/usr/local/lib/FileAPI.jar"]

EXPOSE 8080