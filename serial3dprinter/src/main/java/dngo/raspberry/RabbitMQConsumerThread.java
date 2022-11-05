package dngo.raspberry;

// import java.io.IOException;
// import java.net.URISyntaxException;
// import java.security.KeyManagementException;
// import java.security.NoSuchAlgorithmException;
// import java.util.concurrent.TimeoutException;

// import com.rabbitmq.client.Connection;
// import com.rabbitmq.client.ConnectionFactory;
// import com.rabbitmq.client.Consumer;
// import com.rabbitmq.client.DefaultConsumer;


//To be thrown out - RabbitMQ is unfrasible with current architecture
public class RabbitMQConsumerThread implements Runnable{

    boolean basicAck = false;

    // ConnectionFactory factory = new ConnectionFactory();

    // Connection connection = null;


    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

    public RabbitMQConsumerThread(){
        // factory.setHost("rabbitmqcapstone.azurewebsites.net");
        // factory.setHost("https:%2f%2fsimplprint3d.com%2frabbitserver%2f");
        // factory.setPort(80);
        // factory.setUsername(username);
        // factory.setPassword(password);
        // factory.setVirtualHost("/");
        // factory.setConnectionTimeout(20000);

        
        // try {
        //     // factory.setUri(String.format("amqp://%s:%s@%s:%s/", username, password, "https:%2f%2fsimplprint3d.com%2frabbitserver%2f", 80));
        //     // connection = factory.newConnection();
        // } catch (IOException | TimeoutException e) {//| KeyManagementException | NoSuchAlgorithmException | URISyntaxException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        //     System.out.println("Issues connecting - please check params");


        // }

    }

    
}
