package com.pelea.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LogServer {
    
    private static final String LOG_FOLDER = "server_logs";
    private static final int RETENTION_DAYS = 30;

    public static void main(String[] args) {
        int port = 5000;
        
        cleanOldLogs();
        
        System.out.println("LOG SERVER STARTED ON PORT " + port);
        System.out.println("Logs are being saved to folder: " + new File(LOG_FOLDER).getAbsolutePath());
        System.out.println("Waiting for incoming messages...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String message = in.readLine();
                    
                    if (message != null) {
                        if (message.equals("EXIT")) {
                            System.out.println("Server shutdown command received. Closing...");
                            clientSocket.close();
                            break;
                        }

                        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        String logEntry = "[" + time + "] " + message;
                        
                        System.out.println("Action: " + logEntry);
                        saveToFile(logEntry);
                    }
                    
                    clientSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("SERVER STOPPED");
    }

    private static void saveToFile(String logEntry) {
        try {
            File directory = new File(LOG_FOLDER);
            if (!directory.exists()) directory.mkdir();

            String filename = "log_" + LocalDate.now().toString() + ".txt";
            File logFile = new File(directory, filename);

            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(logEntry);
            }
        } catch (Exception e) {
            System.err.println("Could not write to log file: " + e.getMessage());
        }
    }

    private static void cleanOldLogs() {
        File directory = new File(LOG_FOLDER);
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        long currentTime = System.currentTimeMillis();
        long retentionTime = (long) RETENTION_DAYS * 24 * 60 * 60 * 1000;

        for (File file : files) {
            if (currentTime - file.lastModified() > retentionTime) {
                file.delete();
            }
        }
    }
}