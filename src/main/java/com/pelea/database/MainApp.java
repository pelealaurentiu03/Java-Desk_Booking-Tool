package com.pelea.database;

import com.pelea.ui.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("Starting the app...");
        
        try {
            primaryStage.getIcons().addAll(
                new javafx.scene.image.Image(getClass().getResource("/logo_16.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_32.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_48.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_64.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_128.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_256.png").toExternalForm())
            );
        } catch (Exception e) {
            System.out.println("Error at loading the logo!");
        }
        
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Closing app.");
            System.exit(0);
        });
        
        LoginView loginView = new LoginView();
        loginView.show(primaryStage);
    }

    public static void main(String[] args) {
        DatabaseManager.getInstance();
        
        launch(args);
    }
}