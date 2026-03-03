package com.pelea.ui;

import com.pelea.auth.TriConsumer;
import javafx.scene.image.Image;
import com.pelea.auth.GoogleAuthService;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;

public class LoginView {

    private final GoogleAuthService authService = new GoogleAuthService();

    public void show(Stage stage) {
        stage.setTitle("Desk Booking - Login");

        Label titleLabel = new Label("Welcome to Desk Booking");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Button loginButton = new Button("Sign in with Google");
        loginButton.setStyle("-fx-background-color: #4285F4; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 20px;");
        
        loginButton.setOnAction(e -> {
            System.out.println("Login...");
            loginButton.setDisable(true);
            loginButton.setText("Connecting...");
            
            new Thread(() -> {
                authService.performLogin((email, name, photoUrl) -> {
                    
                    System.out.println("Login successful for: " + name);

                    Platform.runLater(() -> {
                        new DashboardView(email, name, photoUrl).show(stage);
                    });
                    
                });
            }).start();
        });

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(titleLabel, loginButton);

        Scene scene = new Scene(layout, 400, 300);
        stage.setScene(scene);
        stage.show();
    }
}