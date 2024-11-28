package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final ConfigReader config = new ConfigReader("application.properties");
    private static final int SERVER_PORT = Integer.parseInt(config.getServerPort());
    private final List<ConnectionHandler> activeConnections = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Метод для запуска сервера
    public void launchServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        logger.info("Сервер успешно запущен на порту {}", SERVER_PORT);

        // Ожидание подключения клиентов
        while (true) {
            Socket clientSocket = serverSocket.accept();
            ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket);
            activeConnections.add(connectionHandler);
            new Thread(connectionHandler).start(); // Запуск нового потока для клиента
        }
    }

    // Класс для обработки соединений с клиентами
    private class ConnectionHandler implements Runnable {
        private String username;
        private final Socket clientSocket;
        private PrintWriter writer;

        public ConnectionHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);

                // Получение имени пользователя при первом подключении
                username = reader.readLine();
                logger.info("{} подключился к серверу.", username);
                broadcastMessage(username + " присоединился.");
                sendActiveUsersList();

                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("/users")) { // Запрос списка пользователей
                        sendActiveUsersList();
                    } else if (message.startsWith("/dm")) { // Личное сообщение
                        handleDirectMessage(message);
                    } else { // Широковещательное сообщение
                        broadcastMessage(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                logger.error("Ошибка при обработке клиента: {}", e.getMessage());
            } finally {
                closeConnection();
            }
        }

        // Обработка личного сообщения
        private void handleDirectMessage(String message) {
            String[] parts = message.split(" ", 3); // /dm <пользователь> <сообщение>
            if (parts.length == 3) {
                String recipient = parts[1];
                String messageContent = parts[2];
                for (ConnectionHandler client : activeConnections) {
                    if (client.username.equals(recipient)) {
                        client.sendMessage(username + "(лично): " + messageContent);
                        logger.info("Отправлено личное сообщение от {} для {}: {}", username, recipient, messageContent);
                        break;
                    }
                }
            } else {
                writer.println("Неверный формат сообщения! Используйте: /dm <пользователь> <сообщение>");
            }
        }

        // Отправка списка активных пользователей
        private void sendActiveUsersList() {
            StringBuilder sb = new StringBuilder("Подключенные пользователи:\n");
            for (ConnectionHandler client : activeConnections) {
                sb.append("- ").append(client.username).append("\n");
            }
            writer.println(sb);
        }

        // Отправка широковещательного сообщения
        private void broadcastMessage(String message) {
            for (ConnectionHandler client : activeConnections) {
                client.sendMessage(message);
            }
            logger.info("Широковещательное сообщение от {}: {}", username, message);
        }

        // Отправка сообщения одному пользователю
        private void sendMessage(String message) {
            writer.println(message);
        }

        // Закрытие соединения с клиентом
        private void closeConnection() {
            activeConnections.remove(this);
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.warn("Ошибка закрытия соединения с {}: {}", username, e.getMessage());
            }
            broadcastMessage(username + " покинул чат.");
            logger.info("{} покинул чат.", username);
        }
    }

    // Точка входа для запуска сервера
    public static void main(String[] args) throws IOException {
        new Main().launchServer();
    }
}
