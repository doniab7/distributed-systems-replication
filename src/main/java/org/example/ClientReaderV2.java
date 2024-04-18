package org.example;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class ClientReaderV2 {
    private static final String READ_ALL_EXCHANGE = "read_all_exchange";
    private static final String READ_ALL_RESPONSE_QUEUE = "read_all_response_queue";
    private static final int NUM_REPLICAS = 3;

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // Declare read all exchange and response queue
            channel.exchangeDeclare(READ_ALL_EXCHANGE, BuiltinExchangeType.FANOUT);
            channel.queueDeclare(READ_ALL_RESPONSE_QUEUE, false, false, false, null);
            //channel.queueBind(RESPONSE_QUEUE, READ_ALL_EXCHANGE, "");

            // Send 'Read All' query to read all exchange
            String message = "Read All";
            channel.basicPublish(READ_ALL_EXCHANGE, "", null, message.getBytes());
            System.out.println(" [x] Sent 'Read All' request to replicas");

            // Listen for responses from all replicas
            Map<String, Integer> lineCountMap = new HashMap<>();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String response = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Received response from replica: " + response);
                // Update line count map
                updateLineCountMap(response, lineCountMap);
            };
            channel.basicConsume(READ_ALL_RESPONSE_QUEUE, true, deliverCallback, consumerTag -> {});

            Thread.sleep(2000);

            // Find lines that appear in the majority of replicas
            List<String> majorityLines = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : lineCountMap.entrySet()) {
                if (entry.getValue() > NUM_REPLICAS / 2) {
                    majorityLines.add(entry.getKey());
                }
            }

            // Display majority lines
            System.out.println("Lines appearing in the majority of replicas:");
            for (String line : majorityLines) {
                System.out.println(line);
            }


        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void updateLineCountMap(String line, Map<String, Integer> lineCountMap) {
        lineCountMap.put(line, lineCountMap.getOrDefault(line, 0) + 1);
    }

}
