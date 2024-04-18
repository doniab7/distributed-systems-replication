package org.example;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ClientReader {
    private static final String READ_LAST_EXCHANGE = "read_last_exchange";
    private static final String RESPONSE_QUEUE = "response_queue";

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // Declare read last exchange and response queue
            channel.exchangeDeclare(READ_LAST_EXCHANGE, BuiltinExchangeType.FANOUT);
            channel.queueDeclare(RESPONSE_QUEUE, false, false, false, null);

            // Bind response queue to exchange
            // channel.queueBind(RESPONSE_QUEUE, READ_LAST_EXCHANGE, "");

            // Send 'Read Last' query to read last exchange
            String message = "Read Last";
            channel.basicPublish(READ_LAST_EXCHANGE, "", null, message.getBytes());
            System.out.println(" [x] Sent 'Read Last' request to replicas");

            // Listen for response from any replica
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String response = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Received response from replica: " + response);
                // Perform further processing if needed
            };
            channel.basicConsume(RESPONSE_QUEUE, true, deliverCallback, consumerTag -> {});
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
