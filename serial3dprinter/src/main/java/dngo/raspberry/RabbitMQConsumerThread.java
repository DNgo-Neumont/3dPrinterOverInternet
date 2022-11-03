package dngo.raspberry;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;


public class RabbitMQConsumerThread implements Runnable{

    boolean basicAck = false;

    ConnectionFactory factory = new ConnectionFactory();

    Connection connection = null;

    private String username = "exchangeuser";
    private String password = "exchangepass23a@";

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

    public RabbitMQConsumerThread(){
        factory.setHost("https://simplprint3d.com/rabbitserver/");
        factory.setPort(80);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost("/");

        try {
            connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Issues connecting - please check params");


        }

    }

    
}
