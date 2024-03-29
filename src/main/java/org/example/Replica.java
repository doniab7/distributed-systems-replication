package org.example;

import com.rabbitmq.client.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Replica {
    private static final String EXCHANGE_NAME = "file_exchange";
    private static final String QUEUE_PREFIX = "file_addition_queue_replica";
    private static final int NUM_REPLICAS = 3;


    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Replica <replica_number>");
            System.exit(1);
        }

        int replicaNumber = Integer.parseInt(args[0]);

        if (replicaNumber < 1 || replicaNumber > NUM_REPLICAS) {
            System.out.println("Replica number must be between 1 and " + NUM_REPLICAS);
            System.exit(1);
        }

        String queueName = QUEUE_PREFIX + replicaNumber;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(queueName, false, false, false, null);
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            channel.queueBind(queueName, EXCHANGE_NAME, "");

            System.out.println(" [*] Replica " + replicaNumber + " waiting for messages.");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Received '" + message + "'");
                writeToTextFile(message, replicaNumber);
            };

            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
            });



            // Keep the main thread running to continue listening for messages
            while (true) {
                try {
                    Thread.sleep(1000); // Sleep for a short duration to avoid busy waiting
                } catch (InterruptedException e) {
                    System.err.println(" [!] Thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            }
        } catch (IOException | TimeoutException e) {
            System.err.println(" [!] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void writeToTextFile(String message, int replicaNumber) {
        String fileName = "file_replica_" + replicaNumber + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(message + "\n");
            System.out.println(" [x] Line added to file: " + fileName);
        } catch (IOException e) {
            System.err.println(" [!] Error adding line to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
