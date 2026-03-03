package com.pelea.database;

import com.pelea.ui.LogServer;

public class AppLauncher {
    public static void main(String[] args) {
    	//OPEN LOG SERVER IN BACKGROUND
        Thread serverThread = new Thread(() -> {
            try {
                LogServer.main(null);
            } catch (Exception e) {
                System.out.println("Info: LogServer could not start (Port might be busy).");
            }
        });
        
        serverThread.setDaemon(true); 
        serverThread.start();

        try { 
            Thread.sleep(500); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //OPEN MAIN APP
        MainApp.main(args);
    }
}