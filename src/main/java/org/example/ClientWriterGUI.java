package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientWriterGUI {
    private JFrame frame;
    private JTextField messageField;
    private JButton sendButton;

    public ClientWriterGUI() {
        frame = new JFrame("Client Writer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 150);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        // Create message field
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Create send button
        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.setForeground(Color.WHITE);
        sendButton.setBackground(new Color(59, 89, 182));
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new Dimension(100, 30));
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Create panel to hold components
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        // Add components to the panel
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        // Add panel to the frame
        frame.add(panel, BorderLayout.CENTER);
    }

    // Method to display the GUI
    public void show() {
        frame.setVisible(true);
    }

    // Method to handle sending message
    private void sendMessage() {
        String message = messageField.getText();
        String[] args = { message };
        try {
            ClientWriter.main(args);
            JOptionPane.showMessageDialog(frame, "Message sent successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Failed to send message.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Main method to launch the GUI
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientWriterGUI gui = new ClientWriterGUI();
            gui.show();
        });
    }
}
