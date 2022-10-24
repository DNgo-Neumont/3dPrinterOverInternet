package dngo.neumont.rabbitmqcontroller;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


@SpringBootApplication
public class RabbitMqControllerApplication {

    private final static String rabbitHost = System.getenv("RABBITMQ_HOST");
    private final static int rabbitPort = Integer.parseInt(System.getenv("RABBITMQ_PORT"));

    private final static String rabbitUser = System.getenv("RABBITMQ_USER");

    private final static String rabbitPass = System.getenv("RABBITMQ_PASS");

    private final static String rabbitVhost = System.getenv("RABBITMQ_VHOST");

    private static ConnectionFactory connectionFactory = null;


    public static void main(String[] args) {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setConnectionTimeout(10000);
        connectionFactory.setAutomaticRecoveryEnabled(true);

        connectionFactory.setHost(rabbitHost);
        connectionFactory.setPort(rabbitPort);
        connectionFactory.setUsername(rabbitUser);
        connectionFactory.setPassword(rabbitPass);
        connectionFactory.setVirtualHost(rabbitVhost);

        SpringApplication.run(RabbitMqControllerApplication.class, args);
    }

    public static Channel getNewChannel() throws IOException, TimeoutException {
        try{
            Connection connection = connectionFactory.newConnection();
            return connection.createChannel();
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

}
