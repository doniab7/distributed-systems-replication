package org.example;

import com.rabbitmq.client.*;

import java.io.*;
import java.sql.SQLOutput;
import java.util.concurrent.TimeoutException;

public class Replica {
    private static final String EXCHANGE_NAME = "file_exchange";
    private static final String QUEUE_PREFIX = "file_addition_queue_replica";
    private static final String READ_LAST_EXCHANGE = "read_last_exchange";
    private static final String READ_LAST_QUEUE_PREFIX = "read_last_queue_replica";
    private static final String RESPONSE_QUEUE = "response_queue";
    private static final int NUM_REPLICAS = 3;
    private static final String READ_ALL_EXCHANGE = "read_all_exchange";
    private static final String READ_ALL_RESPONSE_QUEUE = "read_all_response_queue";

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
        String readLastQueueName = READ_LAST_QUEUE_PREFIX + replicaNumber;
        String readAllQueueName = "read_all_queue_replica" + replicaNumber;


        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try {


            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();


            //1: RECEIVE MESSAGES FROM CLIENTWRITER
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


            //2: RECEIVE QUERIES FROM CLIENTREADER
            channel.exchangeDeclare(READ_LAST_EXCHANGE, BuiltinExchangeType.FANOUT);
            channel.queueDeclare(readLastQueueName, false, false, false, null);
            channel.queueBind(readLastQueueName, READ_LAST_EXCHANGE, "");

            channel.queueDeclare(RESPONSE_QUEUE, false, false, false, null);

            DeliverCallback readLastCallback = (consumerTag, delivery) -> {
                System.out.println(" [x] Received 'Read Last' request from ClientReader");

                // Process 'Read Last' request (example: read last line from file)
                String lastLine = readLastLine("file_replica_" + replicaNumber + ".txt");

                // Publish response to response queue
                channel.basicPublish("", RESPONSE_QUEUE, null, lastLine.getBytes("UTF-8"));
            };

            // Consume 'Read Last' requests
            channel.basicConsume(readLastQueueName, true, readLastCallback, consumerTag -> {
            });




            // 3: RECEIVE QUERIES FROM CLIENTREADER FOR READ ALL
            channel.exchangeDeclare(READ_ALL_EXCHANGE, BuiltinExchangeType.FANOUT);
            channel.queueDeclare(readAllQueueName, false, false, false, null);
            channel.queueBind(readAllQueueName, READ_ALL_EXCHANGE, "");

            // Start consuming 'Read All' requests
            DeliverCallback readAllCallback = (consumerTag, delivery) -> {
                System.out.println(" [x] Received 'Read All' request from ClientReader");

                // Read the entire file and send each line as a separate message to the response queue
                try (BufferedReader reader = new BufferedReader(new FileReader("file_replica_" + replicaNumber + ".txt"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        channel.basicPublish("", READ_ALL_RESPONSE_QUEUE, null, line.getBytes("UTF-8"));
                        System.out.println(" [x] Sent line to response queue: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Error reading file: " + e.getMessage());
                }
            };

            channel.basicConsume(readAllQueueName, true, readAllCallback, consumerTag -> {});



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

    public static String readLastLine(String fileName) {
        String lastLine = null;
        try (RandomAccessFile file = new RandomAccessFile(new File(fileName), "r")) {
            long fileLength = file.length();
            if (fileLength == 0) {
                return null; // File is empty
            }
            long pos = fileLength - 2; // Start at the end of the file
            String sb = "";
            boolean foundNewLine = false;
            // Read characters backward until a newline character is found or we reach the beginning of the file
            while (pos >= 0) {
                file.seek(pos);
                char c = (char) file.read();
                if (c == '\n') {
                    // If newline character is found, stop reading
                    foundNewLine = true;
                    break;
                }
                sb = c +sb;
                pos--;
            }
            lastLine = sb;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error reading file: " + e.getMessage());
        }
        return lastLine;
    }

}
