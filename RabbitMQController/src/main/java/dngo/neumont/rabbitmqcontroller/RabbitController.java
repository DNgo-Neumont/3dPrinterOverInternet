package dngo.neumont.rabbitmqcontroller;

import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping(path= "/rabbit")
public class RabbitController {

    private Channel rabbitChannel;
    private final Logger logger = LoggerFactory.getLogger(RabbitController.class);

    private final String exchange_name = "command-exchange";

    public RabbitController(){
        try {
            rabbitChannel = RabbitMqControllerApplication.getNewChannel();
            rabbitChannel.exchangeDeclare(exchange_name, BuiltinExchangeType.DIRECT, true);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            logger.error("Failed to connect to RabbitMQ server! Stacktrace provided");
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(path = "/downloadAndPrint/{username}/")
    public ResponseEntity<Map<String, Object>> queueCommand(@PathVariable String username, @RequestBody JsonNode requestBody){
        String printerSelected = requestBody.get("printer").asText();
        String filename = requestBody.get("filename").asText();

        try {
            rabbitChannel.queueDeclare(username, true, false, true, null);
            rabbitChannel.queueBind(username, exchange_name, username);

            Map<String, Object> messageBody = new HashMap<>();
            messageBody.put("printer", printerSelected);
            messageBody.put("file", filename);

            byte[] messageBytes = messageBody.toString().getBytes(StandardCharsets.UTF_8);

            rabbitChannel.basicPublish(exchange_name, username, null, messageBytes);


        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Failed to declare queue with username: " +  username + "! Stacktrace provided");
            throw new RuntimeException(e);
        }

        Map<String, Object> response = new HashMap<>();

        response.put("message", "printer " + printerSelected + " with file " + filename + " now printing");
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }




}
