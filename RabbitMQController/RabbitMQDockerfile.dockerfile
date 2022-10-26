FROM rabbitmq:latest

# Define environment variables.
ENV RABBITMQ_USER exchangeuser
ENV RABBITMQ_PASSWORD exchangepass23a@
ENV RABBITMQ_PID_FILE /var/lib/rabbitmq/mnesia/rabbitmq

ADD init.sh /init.sh
RUN chmod +x /init.sh
EXPOSE 15672

# Define default command
CMD ["/init.sh"]