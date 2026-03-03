package com.pelea.ui;

import java.time.LocalDate;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.stage.Stage;

public class DeskButton extends Button {
    
    private static final double AVATAR_SIZE = 36.0;
    private final String deskId;
    private final String currentUserEmail;
    private final String currentUserName;
    private final String currentUserPhotoUrl;
    private final LocalDate viewingDate;
    private Runnable onDataChanged;
    
    private boolean isBooked = false;
    private String bookedByEmail = null;
    private String bookedByName = null;
    private String bookedByPhotoUrl = null;
    private String bookingInterval = null;
    private String bookingDate = null;

    public DeskButton(String deskId, double x, double y, double w, double h, String currentUserEmail, String currentUserName, String currentUserPhotoUrl, LocalDate viewingDate, Runnable onDataChanged) {
        this.deskId = deskId;
        this.currentUserEmail = currentUserEmail;
        this.currentUserName = currentUserName;
        this.currentUserPhotoUrl = currentUserPhotoUrl;
        this.viewingDate = viewingDate;
        this.onDataChanged = onDataChanged;
        
        this.setLayoutX(x);
        this.setLayoutY(y);
        this.setPrefSize(w, h);
        this.setMinSize(w, h);
        this.setMaxSize(w, h);

        this.setOnAction(e -> {
            if (isBooked) {
                if (bookedByEmail != null && bookedByEmail.equals(currentUserEmail)) {
                    showCancelDialog();
                }
                return;
            }

            BookingDialog dialog = new BookingDialog(
                deskId, currentUserEmail, currentUserName, currentUserPhotoUrl, viewingDate,
                (date, interval) -> {
                	this.markAsBooked(currentUserEmail, currentUserName, currentUserPhotoUrl, date, interval);
                	DashboardView.sendLogToServer("ACTION: Add Booking | DESK: " + deskId + " | USER: " + currentUserName + " | DATE: " + date + " | HOURS: " + interval);
                    if (onDataChanged != null) onDataChanged.run();
                }
            );
            dialog.display();
        });
    }

    public void setAvailabilityFromData(BookingInfo info, String currentTimeFilter) {
        if (info != null && info.interval != null) {
            try {
                String[] parts = info.interval.split(" - ");
                if (parts.length == 2) {
                    java.time.LocalTime start = java.time.LocalTime.parse(parts[0]);
                    java.time.LocalTime end = java.time.LocalTime.parse(parts[1]);
                    java.time.LocalTime filterTime = java.time.LocalTime.parse(currentTimeFilter);

                    if (!filterTime.isBefore(start) && filterTime.isBefore(end)) {
                        this.markAsBooked(info.email, info.name, info.photo, info.date, info.interval);
                        return;
                    }
                }
            } catch (Exception e) {
                System.out.println("Eroare la procesarea orelor: " + e.getMessage());
            }
        }
        
        resetToFree();
    }
    
    public void markAsBooked(String email, String name, String photoUrl, String date, String interval) {
    	this.isBooked = true;
        this.bookedByEmail = email;
        this.bookedByName = name;
        this.bookedByPhotoUrl = photoUrl;
        this.bookingDate = date;
        this.bookingInterval = interval;
        
        this.setText(""); 
        this.setPadding(Insets.EMPTY);
        
        double w = this.getPrefWidth();
        double h = this.getPrefHeight();
        
        double minSide = Math.min(w, h);
        
        double dynamicSize = minSide * 0.75; 
        
        Node avatarGraphic = createCircularAvatar(photoUrl, name, dynamicSize);
        this.setGraphic(avatarGraphic);
        
        this.setStyle(
            "-fx-background-color: transparent; " + 
            "-fx-border-color: #d93025; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 5; " +
            "-fx-cursor: hand;"
        );
        
        Tooltip t = new Tooltip(
            "ID: " + deskId + "\n" +
            "Reserved by: " + name + "\n" +
            "Date: " + date + "\n" +
            "Hours: " + interval
        );
        t.setStyle("-fx-font-size: 12px; -fx-background-color: white; -fx-text-fill: black; -fx-border-color: red;");
        this.setTooltip(t);
        
        t.setStyle("-fx-font-size: 12px; -fx-background-color: white; -fx-text-fill: black; -fx-border-color: #dc3545;");
        t.setShowDelay(Duration.millis(50)); 
        this.setTooltip(t);
    }
    
    private void resetToFree() {
        this.isBooked = false;
        this.bookedByEmail = null;
        this.bookedByName = null;
        this.bookedByPhotoUrl = null;
        
        String textStyle = "-fx-text-fill: #28a745; -fx-font-weight: bold; -fx-font-size: 11px;";

        if (this.getPrefHeight() > this.getPrefWidth()) {
            this.setText("");
            
            Label verticalLabel = new Label(deskId);
            verticalLabel.setStyle(textStyle);
            verticalLabel.setRotate(-90);
            
            this.setGraphic(verticalLabel);
            this.setPadding(new Insets(0));
            
        } else {
            this.setGraphic(null);
            this.setText(deskId);
            this.setPadding(new Insets(5));
        }


        this.setStyle(
            "-fx-background-color: rgba(40, 167, 69, 0.15); " + 
            "-fx-border-color: #28a745; " + 
            "-fx-border-width: 1px; " + 
            "-fx-border-radius: 5; -fx-background-radius: 5;" +
            textStyle + 
            " -fx-cursor: hand;"
        );
        
        Tooltip t = new Tooltip(deskId + " (Free)");
        t.setStyle(
            "-fx-font-size: 12px; " + 
            "-fx-text-fill: #28a745; " +
            "-fx-background-color: white; " +
            "-fx-border-color: #28a745; " +
            "-fx-border-width: 1px;"
        );
        t.setShowDelay(Duration.millis(50));
        this.setTooltip(t);
    }

    private void showCancelDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Booking");
        try {
        	Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().addAll(
                new Image(getClass().getResourceAsStream("/logo_16.png")),
                new Image(getClass().getResourceAsStream("/logo_32.png")),
                new Image(getClass().getResourceAsStream("/logo_48.png")),
                new Image(getClass().getResourceAsStream("/logo_64.png")),
                new Image(getClass().getResourceAsStream("/logo_128.png")),
                new Image(getClass().getResourceAsStream("/logo_256.png"))
            );
        } catch (Exception e) { }
        alert.setHeaderText("Reserved for: " + bookedByName);
        
        alert.setContentText(
            "Desk ID: " + deskId + "\n" +
            "Scheduled: " + bookingDate + "\n" +
            "Interval: " + bookingInterval + "\n\n" +
            "Do you want to cancel this booking?"
        );

        alert.setGraphic(createCircularAvatar(bookedByPhotoUrl, bookedByName, 50));

        alert.getButtonTypes().clear();

        ButtonType btnDelete = new ButtonType("Delete");
        ButtonType btnNo = new ButtonType("No");

        alert.getButtonTypes().addAll(btnDelete, btnNo);

        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == btnDelete) {
            com.pelea.database.DatabaseManager.getInstance().deleteBooking(
                deskId, 
                bookingDate, 
                currentUserEmail
            );
            
            DashboardView.sendLogToServer("ACTION: Cancel (Map) | DESK: " + deskId + " | USER: " + currentUserName + " | DATE: " + bookingDate + " | HOURS: " + bookingInterval);
            
            resetToFree();
            
            if (onDataChanged != null) onDataChanged.run();
        }
    }
    
    public boolean isBooked() {
        return isBooked;
    }

    public String getBookedByName() {
        return bookedByName;
    }
    
    private Node createCircularAvatar(String photoUrl, String fallbackName) {
        return createCircularAvatar(photoUrl, fallbackName, AVATAR_SIZE);
    }

    private Node createCircularAvatar(String photoUrl, String fallbackName, double targetSize) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            try {
            	double loadingSize = targetSize * 4;
            	Image img = new Image(photoUrl, loadingSize, loadingSize, true, true, true);
                ImageView imageView = new ImageView(img);
                
                imageView.setFitWidth(targetSize);
                imageView.setFitHeight(targetSize);
                
                imageView.setSmooth(true);
                imageView.setPreserveRatio(true);
                
                Circle clip = new Circle(targetSize / 2, targetSize / 2, targetSize / 2);
                imageView.setClip(clip);
                
                return imageView;
            } catch (Exception e) {
                System.out.println("Could not load avatar image: " + photoUrl);
            }
        }

        return createInitialsFallback(fallbackName, targetSize);
    }

    private Node createInitialsFallback(String name, double size) {
        String initials = "";
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].charAt(0);
            if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].charAt(0);
            initials = initials.toUpperCase();
        }

        Circle circle = new Circle(size / 2);
        circle.setFill(Color.web("#d93025"));
        
        Label text = new Label(initials);
        text.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (size * 0.4) + "px;");
        
        return new StackPane(circle, text);
    }
    
    public void removeHighlight() {
        this.setStyle(
            "-fx-background-color: transparent; " + 
            "-fx-border-color: #d93025; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 5; " +
            "-fx-cursor: hand;"
        );
    }
    
    public String getDeskId() {
        return this.deskId;
    }
}