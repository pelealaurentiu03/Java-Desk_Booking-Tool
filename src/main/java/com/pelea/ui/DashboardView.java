package com.pelea.ui;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DashboardView {

    private final String userEmail;
    private final String userName;
    private final String userPhotoUrl;
    
    private Pane mapPane;
    private ScrollPane scrollPane;
    private Group zoomGroup;
    private StackPane contentHolder;
    private int currentLevel = 1;
    private java.util.Map<String, com.pelea.ui.BookingInfo> currentBookingsMap;
    private LocalDate selectedDate = LocalDate.now();
    private String selectedTime = "08:00";
    private VBox bookingsList;
    private ScrollPane sidebarScroll;
    private TextField searchField;
    private ContextMenu suggestionsPopup;
    private DeskButton currentHighlightedBtn = null;
    private Label lblWeather;
    
    private static final double ZOOM_FACTOR = 1.1; 
    private double currentScale = 1.0;
    private double scrollAccumulator = 0;

    public DashboardView(String userEmail, String userName, String userPhotoUrl) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.userPhotoUrl = userPhotoUrl;
    }
    
    private javafx.scene.Node createHeaderAvatar(String photoUrl, String name) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            try {
                ImageView imageView = new ImageView(new Image(photoUrl, 40, 40, true, true, true));
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(20, 20, 20);
                imageView.setClip(clip);
                return imageView;
            } catch (Exception e) {
                System.out.println("The profile picture can't be loaded.");
            }
        }

        String initials = "";
        if (name != null && !name.isEmpty()) {
            initials = name.substring(0, Math.min(name.length(), 2)).toUpperCase();
        }

        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(20);
        circle.setFill(javafx.scene.paint.Color.web("#4285F4"));
        
        Label text = new Label(initials);
        text.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        return new StackPane(circle, text);
    }

    public void show(Stage stage) {
    	sendLogToServer("ACTION: User Login | USER: " + userName + " | EMAIL: " + userEmail);
    	stage.setOnCloseRequest(event -> {
            sendLogToServer("EXIT");
            Platform.exit();
            System.exit(0);
        });
    	
        stage.setTitle("Desk Booking - Dashboard");

        Label welcomeLabel = new Label("Hello, " + userName + "!");
        welcomeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        javafx.scene.Node userAvatar = createHeaderAvatar(userPhotoUrl, userName);
        
        HBox userInfo = new HBox(10, userAvatar, welcomeLabel);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        
        Label dateLabel = new Label("Select Date:");
        dateLabel.setStyle("-fx-font-weight: bold;");
        DatePicker mapDatePicker = new DatePicker(java.time.LocalDate.now());
        mapDatePicker.setPrefWidth(150);
        
        mapDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(java.time.LocalDate.now().minusDays(30)));
            }
        });

        mapDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedDate = newVal;
                sendLogToServer("ACTION: Date Changed | NEW DATE: " + newVal + " | USER: " + userName);
                loadMap(currentLevel);
                fetchWeatherForDate(newVal);
            }
        });
        
        javafx.scene.control.ComboBox<String> timeSelector = new javafx.scene.control.ComboBox<>();
        for (int i = 7; i <= 20; i++) {
            String timeString = String.format("%02d:00", i);
            timeSelector.getItems().add(timeString);
        }
        timeSelector.setValue(selectedTime);
        timeSelector.setStyle("-fx-font-weight: bold;");

        timeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedTime = newVal;
                loadMap(currentLevel);
            }
        });
        
        Button btnEtaj1 = new Button("LEVEL 1");
        Button btnEtaj2 = new Button("LEVEL 2");
        
        styleFloorButton(btnEtaj1);
        styleFloorButton(btnEtaj2);
        
        btnEtaj1.setOnAction(e -> { currentLevel = 1; loadMap(1); });
        btnEtaj2.setOnAction(e -> { currentLevel = 2; loadMap(2); });

        searchField = new TextField();
        searchField.setPromptText("Find colleague...");
        searchField.setPrefWidth(200);
        
        suggestionsPopup = new ContextMenu();

        Button btnSearch = new Button("Search");
        btnSearch.setOnAction(e -> performSearch(searchField.getText()));

        setupAutocomplete();
        
        this.lblWeather = new Label("Loading weather...");
        this.lblWeather.setStyle("-fx-font-weight: bold; -fx-text-fill: #5f6368; -fx-padding: 0 10 0 10;");
        fetchWeatherForDate(java.time.LocalDate.now());

        HBox controls = new HBox(15, userInfo, new Label("|"), dateLabel, mapDatePicker, timeSelector, new Label("|"), btnEtaj1, btnEtaj2, new Label("|"), searchField, btnSearch, new Label("|"), lblWeather);
        
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setStyle("-fx-padding: 10px; -fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        mapPane = new Pane();
        mapPane.setOnMouseClicked(e -> {
            if (currentHighlightedBtn != null) {
                currentHighlightedBtn.removeHighlight();
                currentHighlightedBtn = null;
            }
        });
        
        zoomGroup = new Group(mapPane);
        contentHolder = new StackPane(zoomGroup);
        
        contentHolder.setOnScroll(event -> {
            if (event.isControlDown() || true) { 
                event.consume(); 
                scrollAccumulator += event.getDeltaY();
                double threshold = 40.0;
                if (scrollAccumulator > threshold) {
                    applyZoom(ZOOM_FACTOR); 
                    scrollAccumulator = 0;
                } else if (scrollAccumulator < -threshold) {
                    applyZoom(1 / ZOOM_FACTOR); 
                    scrollAccumulator = 0;
                }
            }
        });

        scrollPane = new ScrollPane(contentHolder);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox sidePanel = new VBox(15);
        sidePanel.setPrefWidth(250);
        sidePanel.setStyle("-fx-background-color: #ffffff; -fx-padding: 20px; -fx-border-color: #ddd; -fx-border-width: 0 1 0 0;");

        Label sideTitle = new Label("My Bookings");
        sideTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        this.sidebarScroll = new ScrollPane();
        this.bookingsList = new VBox(10);
        this.bookingsList.setStyle("-fx-padding: 10;");
        sidebarScroll.setContent(bookingsList);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setStyle("-fx-background: #ffffff; -fx-background-color: transparent; -fx-border-color: transparent;");

        sidePanel.getChildren().addAll(sideTitle, new Separator(), sidebarScroll);

        BorderPane rootLayout = new BorderPane();
        rootLayout.setTop(controls);
        rootLayout.setLeft(sidePanel);
        rootLayout.setCenter(scrollPane);

        Scene scene = new Scene(rootLayout, 1280, 720);
        stage.setScene(scene);
        stage.centerOnScreen();
        
        refreshMyBookings();

        loadMap(1);
    }

    private void loadMap(int level) {
        mapPane.getChildren().clear();

        currentScale = 1.0;
        zoomGroup.setScaleX(1.0);
        zoomGroup.setScaleY(1.0);
        
        currentBookingsMap = com.pelea.database.DatabaseManager.getInstance()
                .getAllBookingsForDateAsMap(selectedDate.toString());
        
        java.util.Map<String, com.pelea.ui.BookingInfo> allBookingsToday = 
                com.pelea.database.DatabaseManager.getInstance()
                    .getAllBookingsForDateAsMap(selectedDate.toString());
        
        String imagePath = "/maps/level" + level + ".png";
        try {
            InputStream is = getClass().getResourceAsStream(imagePath);
            if (is == null) {
                System.out.println("Level map can't load: " + imagePath);
                return;
            }
            
            Image mapImage = new Image(is);
            ImageView imageView = new ImageView(mapImage);
            imageView.setPreserveRatio(true);
            
            mapPane.getChildren().add(imageView);
            mapPane.setPrefSize(mapImage.getWidth(), mapImage.getHeight());
            
            if (level == 1) {
                setupEtaj1Desks();
            } else {
                setupEtaj2Desks();
            }

            Platform.runLater(() -> {
                fitMapToWindow();
                scrollPane.setHvalue(0.5);
                scrollPane.setVvalue(0.5);
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //COORDONATE CLICK
        mapPane.setOnMouseClicked(e -> {
            System.out.println("COORDONATE: " + e.getX() + ", " + e.getY());
            
            if (currentHighlightedBtn != null) {
                currentHighlightedBtn.removeHighlight();
                currentHighlightedBtn = null;
            }
        });
    }
    
    private void applyZoom(double factor) {
        double oldH = scrollPane.getHvalue();
        double oldV = scrollPane.getVvalue();
        
        double newScale = currentScale * factor;
        if (newScale < 0.6 || newScale > 3.0) return;
        
        currentScale = newScale;
        zoomGroup.setScaleX(currentScale);
        zoomGroup.setScaleY(currentScale);
        
        contentHolder.setPrefSize(
                mapPane.getPrefWidth() * currentScale, 
                mapPane.getPrefHeight() * currentScale
            );
        
        scrollPane.layout(); 
        
        scrollPane.setHvalue(oldH);
        scrollPane.setVvalue(oldV);
    }
    
    private void fitMapToWindow() {
        if (mapPane.getChildren().isEmpty()) return;
        
        double scrollWidth = scrollPane.getWidth();
        double scrollHeight = scrollPane.getHeight();
        double mapWidth = mapPane.getPrefWidth();
        double mapHeight = mapPane.getPrefHeight();
        
        if (scrollWidth == 0 || mapWidth == 0) return;

        double scaleX = scrollWidth / mapWidth;
        double scaleY = scrollHeight / mapHeight;
        
        double scale = Math.min(scaleX, scaleY) * 0.95;
        
        currentScale = scale;
        zoomGroup.setScaleX(scale);
        zoomGroup.setScaleY(scale);
        
        contentHolder.setPrefSize(
                mapWidth * scale, 
                mapHeight * scale
            );
    }

    private void setupEtaj1Desks() {

        //STANDARD DESKS
        addDesk("IT-1", 677, 167, 30, 50);
        addDesk("IT-2", 677, 118, 30, 50);
        addDesk("IT-3", 677, 66, 30, 50);
        addDesk("IT-4", 708, 167, 30, 50);
        addDesk("IT-5", 708, 118, 30, 50);
        addDesk("IT-6", 708, 66, 30, 50);
        
        addDesk("IT-9", 815, 167, 30, 50);
        addDesk("IT-8", 815, 118, 30, 50);
        addDesk("IT-7", 815, 66, 30, 50);
        addDesk("IT-10", 848, 167, 30, 50);
        addDesk("IT-11", 848, 118, 30, 50);
        addDesk("IT-12", 848, 66, 30, 50);
        
        addDesk("IT-13", 954, 167, 30, 50);
        addDesk("IT-14", 954, 118, 30, 50);
        addDesk("IT-15", 954, 66, 30, 50);
        addDesk("IT-16", 986, 167, 30, 50);
        addDesk("IT-17", 986, 118, 30, 50);
        addDesk("IT-18", 986, 66, 30, 50);
        
        addDesk("IT-19", 1095, 95, 55, 30);
        addDesk("IT-20", 1173, 95, 55, 30);
        addDesk("IT-21", 1173, 123, 30, 50);
        
        addDesk("GM-1", 514, 59, 30, 50);
        addDesk("GM-2", 514, 166, 30, 50);
        
        addDesk("PM-1", 818, 433, 30, 50);
        addDesk("PM-2", 818, 484, 30, 50);
        addDesk("PM-3", 818, 534, 30, 50);
        addDesk("PM-4", 848, 534, 30, 50);
        addDesk("PM-5", 848, 484, 30, 50);
        addDesk("PM-6", 848, 433, 30, 50);
        addDesk("PM-7", 682, 423, 50, 50);
        addDesk("PM-8", 683, 520, 50, 50);
        addDesk("PM-9", 513, 543, 30, 65);
        
        addDesk("Sales-1", 994, 433, 30, 50);
        addDesk("Sales-2", 994, 484, 30, 50);
        addDesk("Sales-3", 994, 534, 30, 50);
        addDesk("Sales-4", 1025, 433, 30, 50);
        addDesk("Sales-5", 1025, 484, 30, 50);
        addDesk("Sales-6", 1025, 534, 30, 50);
        
        addDesk("Sales-7", 1144, 433, 30, 50);
        addDesk("Sales-8", 1144, 484, 30, 50);
        addDesk("Sales-9", 1144, 534, 30, 50);
        addDesk("Sales-10", 1175, 433, 30, 50);
        addDesk("Sales-11", 1175, 484, 30, 50);
        addDesk("Sales-12", 1175, 534, 30, 50);
        
        addDesk("Q-1", 1272, 74, 30, 50);
        addDesk("Q-2", 1272, 124, 30, 50);
        addDesk("Q-3", 1303, 124, 30, 50);
        addDesk("Q-4", 1303, 74, 30, 50);
        
        addDesk("Q-5", 1406, 103, 30, 50);
        addDesk("Q-6", 1406, 153, 30, 50);
        addDesk("Q-7", 1406, 205, 30, 50);
        addDesk("Q-8", 1437, 205, 30, 50);
        addDesk("Q-9", 1437, 153, 30, 50);
        addDesk("Q-10", 1437, 103, 30, 50);
        
        addDesk("Q-11", 1272, 536, 30, 50);
        addDesk("Q-12", 1272, 485, 30, 50);
        addDesk("Q-13", 1303, 485, 30, 50);
        addDesk("Q-14", 1303, 536, 30, 50);
        
        addDesk("Q-15", 1416, 546, 30, 50);
        addDesk("Q-16", 1416, 495, 30, 50);
        addDesk("Q-17", 1416, 445, 30, 50);
        addDesk("Q-18", 1446, 445, 30, 50);
        addDesk("Q-19", 1446, 495, 30, 50);
        addDesk("Q-20", 1446, 546, 30, 50);
        
        //BIG MEETING ROOM
        addMeetingRoom("Felix", 371, 90, 55, 150);
        
        //SMALL MEETINGROOM
        addMeetingRoom("Mt. Mic", 1148, 224, 45, 35);
        addMeetingRoom("Semenic", 1098, 262, 45, 35);
        addMeetingRoom("Herculane", 513, 423, 50, 50);
    }
    
    private void setupEtaj2Desks() {

    	//STANDARD DESKS
        addDesk("PUR", 337, 53, 30, 50);
        
        addDesk("PUR-1", 483, 169, 25, 50);
        addDesk("PUR-2", 483, 120, 25, 50);
        addDesk("PUR-3", 483, 74, 25, 50);
        addDesk("PUR-4", 512, 74, 25, 50);
        addDesk("PUR-5", 512, 120, 25, 50);
        addDesk("PUR-6", 512, 169, 25, 50);
        
        addDesk("PUR-7", 625, 74, 25, 50);
        addDesk("PUR-8", 625, 120, 25, 50);
        addDesk("PUR-9", 625, 169, 25, 50);
        addDesk("PUR-10", 653, 169, 25, 50);
        addDesk("PUR-11", 653, 120, 25, 50);
        addDesk("PUR-12", 653, 74, 25, 50);
        
        addDesk("PUR-13", 741, 74, 25, 50);
        addDesk("PUR-14", 741, 120, 25, 50);
        addDesk("PUR-15", 741, 169, 25, 50);
        addDesk("PUR-16", 770, 169, 25, 50);
        addDesk("PUR-17", 770, 120, 25, 50);
        addDesk("PUR-18", 770, 74, 25, 50);
        
        addDesk("PUR-19", 868, 62, 25, 50);
        addDesk("PUR-20", 868, 109, 25, 50);
        addDesk("PUR-21", 868, 159, 25, 50);
        addDesk("PUR-22", 896, 159, 25, 50);
        addDesk("PUR-23", 896, 109, 25, 50);
        addDesk("PUR-24", 896, 62, 25, 50);
        
        addDesk("HR", 1029, 60, 30, 50);
        
        addDesk("HR-1", 1157, 63, 25, 50);
        addDesk("HR-2", 1185, 63, 25, 50);
        addDesk("HR-3", 1275, 63, 25, 50);
        addDesk("HR-4", 1304, 63, 25, 50);
        
        addDesk("HR-5", 1194, 166, 50, 25);
        addDesk("HR-6", 1242, 166, 50, 25);
        addDesk("HR-7", 1242, 193, 50, 25);
        addDesk("HR-8", 1194, 193, 50, 25);
        
        addDesk("FI/CO-A", 451, 401, 30, 50);
        addDesk("FI/CO-B", 451, 523, 30, 50);
        
        addDesk("GBS-1", 604, 408, 25, 50);
        addDesk("GBS-2", 604, 457, 25, 50);
        addDesk("GBS-3", 604, 504, 25, 50);
        addDesk("GBS-4", 631, 408, 25, 50);
        addDesk("GBS-5", 631, 457, 25, 50);
        addDesk("GBS-6", 631, 504, 25, 50);

        addDesk("GBS-7", 729, 408, 25, 50);
        addDesk("GBS-8", 729, 457, 25, 50);
        addDesk("GBS-9", 729, 504, 25, 50);
        addDesk("GBS-10", 758, 408, 25, 50);
        addDesk("GBS-11", 758, 457, 25, 50);
        addDesk("GBS-12", 758, 504, 25, 50);
        
        addDesk("GBS-13", 855, 408, 25, 50);
        addDesk("GBS-14", 855, 457, 25, 50);
        addDesk("GBS-15", 855, 504, 25, 50);
        addDesk("GBS-16", 883, 408, 25, 50);
        addDesk("GBS-17", 883, 457, 25, 50);
        addDesk("GBS-18", 883, 504, 25, 50);
        
        addDesk("GBS-19", 1001, 408, 25, 50);
        addDesk("GBS-20", 1001, 457, 25, 50);
        addDesk("GBS-21", 1001, 504, 25, 50);
        addDesk("GBS-22", 1028, 408, 25, 50);
        addDesk("GBS-23", 1028, 457, 25, 50);
        addDesk("GBS-24", 1028, 504, 25, 50);
        
        addDesk("GBS-25", 1139, 408, 25, 50);
        addDesk("GBS-26", 1139, 457, 25, 50);
        addDesk("GBS-27", 1139, 504, 25, 50);
        addDesk("GBS-28", 1165, 408, 25, 50);
        addDesk("GBS-29", 1165, 457, 25, 50);
        addDesk("GBS-30", 1165, 504, 25, 50);
        
        addDesk("GBS-31", 1266, 408, 25, 50);
        addDesk("GBS-32", 1266, 457, 25, 50);
        addDesk("GBS-33", 1266, 504, 25, 50);
        addDesk("GBS-34", 1293, 408, 25, 50);
        addDesk("GBS-35", 1293, 457, 25, 50);
        addDesk("GBS-36", 1293, 504, 25, 50);
        
        //SMALL MEETINGROOM
        addMeetingRoom("Sinaia", 325, 187, 50, 50);
        addMeetingRoom("Predeal", 998, 185, 50, 50);
        addDesk("Straja", 843, 244, 77, 32);
    }

    private void addDesk(String id, double x, double y, double width, double height) {
        DeskButton btn = new DeskButton(id, x, y, width, height, userEmail, userName, userPhotoUrl, selectedDate, this::refreshMyBookings);
        btn.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; " + btn.getStyle());
        
        if (currentBookingsMap != null) {
            com.pelea.ui.BookingInfo info = currentBookingsMap.get(id);
            btn.setAvailabilityFromData(info, selectedTime);
        }
        
        mapPane.getChildren().add(btn);
    }
    
    private void addMeetingRoom(String id, double x, double y, double width, double height) {
        DeskButton btn = new DeskButton(id, x, y, width, height, userEmail, userName, userPhotoUrl, selectedDate, this::refreshMyBookings);
        btn.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; " + btn.getStyle());
        
        if (currentBookingsMap != null) {
            com.pelea.ui.BookingInfo info = currentBookingsMap.get(id);
            btn.setAvailabilityFromData(info, selectedTime);
        }
        
        mapPane.getChildren().add(btn);
    }
    
    private void styleFloorButton(Button b) {
        b.setStyle("-fx-background-color: #4285F4; -fx-text-fill: white; -fx-font-weight: bold;");
    }
    
    private void refreshMyBookings() {
        if (bookingsList == null) return;
        
        bookingsList.getChildren().clear();
        
        var userBookings = com.pelea.database.DatabaseManager.getInstance().getBookingsForUser(userEmail);
        
        if (userBookings.isEmpty()) {
            Label emptyLabel = new Label("No bookings found.");
            emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
            bookingsList.getChildren().add(emptyLabel);
            return;
        }

        for (var booking : userBookings) {
            VBox card = new VBox(8);
            card.setStyle(
                "-fx-background-color: white; " +
                "-fx-padding: 15; " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-width: 1; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);"
            );
            
            if (userBookings.size() > 4) {
                sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            } else {
                sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            }
            
            Label lblDesk = new Label("Desk " + booking.getDeskId());
            lblDesk.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");
            
            Label lblDate = new Label("Date: " + booking.getDate());
            lblDate.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368;");
            
            Label lblInterval = new Label("Hours: " + booking.getInterval());
            lblInterval.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368;");
           
            Separator sep = new Separator();
            
            Button btnDelete = new Button("Delete Booking");
            btnDelete.setMaxWidth(Double.MAX_VALUE);
            
            String normalStyle = "-fx-background-color: #fce8e6; -fx-text-fill: #d93025; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;";
            String hoverStyle = "-fx-background-color: #f9d2ce; -fx-text-fill: #a50e0e; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;";
            
            btnDelete.setStyle(normalStyle);
            
            btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(hoverStyle));
            btnDelete.setOnMouseExited(e -> btnDelete.setStyle(normalStyle));
            
            btnDelete.setOnAction(e -> {
                com.pelea.database.DatabaseManager.getInstance().deleteBooking(
                    booking.getDeskId(), 
                    booking.getDate(), 
                    userEmail
                );
                
                sendLogToServer("ACTION: Delete (List) | DESK: " + booking.getDeskId() + " | USER: " + userName + " | DATE: " + booking.getDate() + " | HOURS: " + booking.getInterval());
                
                bookingsList.getChildren().remove(card);
                
                if (bookingsList.getChildren().isEmpty()) {
                    Label emptyLabel = new Label("No bookings found.");
                    emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                    bookingsList.getChildren().add(emptyLabel);
                }
                
                if (booking.getDate().equals(selectedDate.toString())) {
                    if (currentBookingsMap != null) {
                        currentBookingsMap.remove(booking.getDeskId());
                    }
                    updateDeskStatusOnMap(booking.getDeskId());
                }
            });

            card.getChildren().addAll(lblDesk, lblDate, lblInterval, sep, btnDelete);
            bookingsList.getChildren().add(card);
        }
    }
    
    private void updateDeskStatusOnMap(String targetDeskId) {
        com.pelea.ui.BookingInfo info = null;
        if (currentBookingsMap != null) {
            info = currentBookingsMap.get(targetDeskId); 
        }
        
        for (javafx.scene.Node node : mapPane.getChildren()) {
            if (node instanceof DeskButton) {
                DeskButton btn = (DeskButton) node;
                if (btn.getDeskId().equals(targetDeskId)) {
                	btn.setAvailabilityFromData(info, selectedTime); 
                    break; 
                }
            }
        }
    }
    
    private void setupAutocomplete() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (suggestionsPopup.isShowing()) {
                suggestionsPopup.hide();
            }

            if (newValue == null || newValue.trim().isEmpty()) {
                return;
            }

            var allNames = com.pelea.database.DatabaseManager.getInstance()
                    .getAllBookedNamesForDate(selectedDate.toString());

            java.util.List<MenuItem> suggestions = new java.util.ArrayList<>();
            
            for (String name : allNames) {
                if (name.toLowerCase().contains(newValue.toLowerCase())) {
                    MenuItem item = new MenuItem(name);
                    
                    item.setOnAction(e -> {
                        searchField.setText(name);
                        performSearch(name);
                    });
                    suggestions.add(item);
                }
            }

            if (!suggestions.isEmpty()) {
                suggestionsPopup.getItems().setAll(suggestions);
                suggestionsPopup.show(searchField, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });
    }

    private void performSearch(String targetName) {
        if (targetName == null || targetName.isEmpty()) return;
        
        sendLogToServer("ACTION: Search | QUERY: " + targetName + " | USER: " + userName);
        
        suggestionsPopup.hide();
        
        if (currentHighlightedBtn != null) {
            currentHighlightedBtn.removeHighlight();
            currentHighlightedBtn = null;
        }
        
        boolean foundOnMap = false;

        for (javafx.scene.Node node : mapPane.getChildren()) {
            if (node instanceof DeskButton) {
                DeskButton btn = (DeskButton) node;
                
                if (btn.isBooked() && btn.getBookedByName() != null && 
                    btn.getBookedByName().equalsIgnoreCase(targetName)) {
                    
                    foundOnMap = true;
                    
                    currentHighlightedBtn = btn;
                    
                    btn.setStyle(
                            "-fx-background-color: transparent; " + 
                            "-fx-border-color: gold; " +         
                            "-fx-border-width: 4; " +            
                            "-fx-effect: dropshadow(gaussian, gold, 15, 0.6, 0, 0); " + 
                            "-fx-cursor: hand;"
                        );
                    
                    break;
                }
            }
        }

        if (!foundOnMap) {
            var allNamesToday = com.pelea.database.DatabaseManager.getInstance()
                    .getAllBookedNamesForDate(selectedDate.toString());
            
            boolean hasBookingToday = allNamesToday.stream()
                    .anyMatch(name -> name.equalsIgnoreCase(targetName));

            if (hasBookingToday) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Search Result");
                alert.setHeaderText("Not on this level");
                alert.setContentText(targetName + " doesn't have any booking for this level in the selected time period.");
                setAlertIcon(alert);
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Search Result");
                alert.setContentText("User '" + targetName + "' has no bookings for " + selectedDate + ".");
                alert.showAndWait();
            }
        }
    }
    
    private void fetchWeatherForDate(LocalDate date) {
        if (lblWeather != null) {
            Platform.runLater(() -> lblWeather.setText("Loading..."));
        }

        new Thread(() -> {
            try {
                String urlString;
                boolean isToday = date.equals(LocalDate.now());

                if (isToday) {
                    urlString = "https://api.open-meteo.com/v1/forecast?latitude=45.75&longitude=21.23&current_weather=true";
                } else {
                    urlString = "https://api.open-meteo.com/v1/forecast?latitude=45.75&longitude=21.23&daily=temperature_2m_max,temperature_2m_min&timezone=auto&start_date=" + date + "&end_date=" + date;
                }

                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() != 200) throw new RuntimeException("HTTP Error");

                java.util.Scanner scanner = new java.util.Scanner(url.openStream());
                StringBuilder inline = new StringBuilder();
                while (scanner.hasNext()) inline.append(scanner.nextLine());
                scanner.close();

                String json = inline.toString();
                String finalText = "";
                String tempForColor = "";

                if (isToday) {
                    String blockKey = "\"current_weather\"";
                    int blockIndex = json.indexOf(blockKey);
                    if (blockIndex != -1) {
                        String searchKey = "\"temperature\":";
                        int index = json.indexOf(searchKey, blockIndex);
                        if (index != -1) {
                            int start = index + searchKey.length();
                            int end = json.indexOf(",", start);
                            String temp = json.substring(start, end);
                            finalText = "Now: " + temp + "°C";
                            tempForColor = temp;
                        }
                    }
                } else {
                    String max = "?";
                    String min = "?";

                    String maxKey = "\"temperature_2m_max\":[";
                    int maxIndex = json.indexOf(maxKey);
                    if (maxIndex != -1) {
                        int start = maxIndex + maxKey.length();
                        int end = json.indexOf("]", start);
                        max = json.substring(start, end);
                        tempForColor = max;
                    }
                    
                    String minKey = "\"temperature_2m_min\":[";
                    int minIndex = json.indexOf(minKey);
                    if (minIndex != -1) {
                        int start = minIndex + minKey.length();
                        int end = json.indexOf("]", start);
                        min = json.substring(start, end);
                    }

                    if (!max.equals("?")) {
                        finalText = "Min: " + min + "° / Max: " + max + "°C";
                    }
                }

                if (!finalText.isEmpty()) {
                    String finalOutput = finalText;
                    String checkColor = tempForColor;
                    
                    Platform.runLater(() -> {
                        lblWeather.setText(finalOutput);
                        
                        try {
                            double t = Double.parseDouble(checkColor);
                            if (t < 10) lblWeather.setStyle("-fx-text-fill: #1a73e8; -fx-font-weight: bold; -fx-padding: 0 10 0 10;"); 
                            else if (t > 25) lblWeather.setStyle("-fx-text-fill: #d93025; -fx-font-weight: bold; -fx-padding: 0 10 0 10;"); 
                            else lblWeather.setStyle("-fx-text-fill: #188038; -fx-font-weight: bold; -fx-padding: 0 10 0 10;"); 
                        } catch (Exception e) {}
                    });
                } else {
                     Platform.runLater(() -> lblWeather.setText("No data"));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblWeather.setText("Weather N/A"));
            }
        }).start();
    }
    
    public static void sendLogToServer(String message) {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 5000);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                
                out.println(message);
                System.out.println("Log sent: " + message);
                
            } catch (Exception e) {
            }
        }).start();
    }
    
    private void setAlertIcon(Alert alert) {
        try {
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().addAll(
                new javafx.scene.image.Image(getClass().getResource("/logo_16.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_32.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_48.png").toExternalForm()), // Sper ca l-ai facut cu "l" mic! :)
                new javafx.scene.image.Image(getClass().getResource("/logo_64.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_128.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_256.png").toExternalForm())
            );
        } catch (Exception e) {
            System.out.println("Logo not found!");
        }
    }
}