package com.pelea.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.function.BiConsumer;

public class BookingDialog {

    private final String deskId;
    private final BiConsumer<String, String> onBookingSuccess;
    private String userEmail;
    private String userName;
    private String userPhotoUrl;
    private LocalDate initialDate;

    public BookingDialog(String deskId, String userEmail, String userName, String userPhotoUrl, 
            LocalDate initialDate, BiConsumer<String, String> onBookingSuccess) {
        this.deskId = deskId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.userPhotoUrl = userPhotoUrl;
        this.onBookingSuccess = onBookingSuccess;
        this.initialDate = initialDate;
    }

    public void display() {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Desk booking: " + deskId);
        window.setMinWidth(400);  
        window.setMinHeight(600);
        
        try {
            window.getIcons().addAll(
                new javafx.scene.image.Image(getClass().getResource("/logo_16.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_32.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_48.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_64.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_128.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_256.png").toExternalForm())
            );
        } catch (Exception e) {
            System.out.println("Logo not found!!");
        }

        // UI ELEMENTS
        Label titleLabel = new Label("Book the desk no. " + deskId);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        ToggleGroup group = new ToggleGroup();
        RadioButton rbSingle = new RadioButton("Single day");
        RadioButton rbMultiple = new RadioButton("More days");
        rbSingle.setToggleGroup(group);
        rbMultiple.setToggleGroup(group);
        rbSingle.setSelected(true);

        HBox radioBox = new HBox(20, rbSingle, rbMultiple);
        radioBox.setAlignment(Pos.CENTER);
        radioBox.setPadding(new Insets(10));

        // DATE SELECTOR
        VBox dateContainer = new VBox(10);
        dateContainer.setAlignment(Pos.CENTER);
        
        Label lblDateStart = new Label("Date:");
        DatePicker datePickerStart = new DatePicker(initialDate);
        styleDatePicker(datePickerStart);

        Label lblDateEnd = new Label("Until:");
        DatePicker datePickerEnd = new DatePicker(initialDate.plusDays(1));
        styleDatePicker(datePickerEnd);
        
        datePickerStart.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                datePickerEnd.setValue(newDate.plusDays(1));
            }
        });
        
        VBox endContainer = new VBox(5, lblDateEnd, datePickerEnd);
        endContainer.setAlignment(Pos.CENTER);
        endContainer.setVisible(false);
        endContainer.setManaged(false);

        dateContainer.getChildren().addAll(lblDateStart, datePickerStart, endContainer);

        rbSingle.setOnAction(e -> {
            lblDateStart.setText("Date:");
            endContainer.setVisible(false);
            endContainer.setManaged(false);
        });

        rbMultiple.setOnAction(e -> {
            lblDateStart.setText("From:");
            endContainer.setVisible(true);
            endContainer.setManaged(true);
        });

        // HOUR SELECTOR
        Label lblTime = new Label("Work hours: ");
        ComboBox<String> startHour = new ComboBox<>();
        ComboBox<String> endHour = new ComboBox<>();
        for (int i = 7; i <= 20; i++) {
            String hour = String.format("%02d:00", i);
            startHour.getItems().add(hour);
            endHour.getItems().add(hour);
        }
        startHour.setValue("08:00");
        endHour.setValue("16:00");

        HBox timeBox = new HBox(10, startHour, new Label("->"), endHour);
        timeBox.setAlignment(Pos.CENTER);

        // BUTTONS
        Button btnSave = new Button("Confirm the booking");
        btnSave.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnSave.setPrefWidth(200);
        
        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> window.close());

        // SAVE LOGIC (COMBINED & CLEANED)
        btnSave.setOnAction(e -> {
            LocalDate startDate = datePickerStart.getValue();
            String sHour = startHour.getValue();
            String eHour = endHour.getValue();

            // VALIDATIONS
            if (startDate == null) { showAlert("Error", "Please select the start date!"); return; }
            if (sHour.compareTo(eHour) >= 0) { showAlert("Error", "The start hour can't be after the end hour!"); return; }

            String intervalText = sHour + " - " + eHour;
            boolean allSaved = true;

            if (rbMultiple.isSelected()) {
                LocalDate endDate = datePickerEnd.getValue();
                if (endDate == null || endDate.isBefore(startDate)) { 
                    showAlert("Error", "The start date can't be after the end date!"); 
                    return; 
                }

                // SAVING MULTIPLE DAYS
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    boolean success = com.pelea.database.DatabaseManager.getInstance().saveBooking(
                        this.deskId, this.userEmail, this.userName, this.userPhotoUrl, 
                        date.toString(),
                        intervalText
                    );
                    if (!success) allSaved = false;
                }
            } else {
                // SAVING A SINGLE DAY
                allSaved = com.pelea.database.DatabaseManager.getInstance().saveBooking(
                    this.deskId, this.userEmail, this.userName, this.userPhotoUrl, 
                    startDate.toString(), 
                    intervalText
                );
            }

            if (allSaved) {
                if (onBookingSuccess != null) {
                    onBookingSuccess.accept(startDate.toString(), intervalText);
                }
                window.close();
            } else {
                showAlert("Error", "Some bookings could not be saved to MongoDB.");
            }
        });

        VBox layout = new VBox(15);
        layout.getChildren().addAll(titleLabel, new Separator(), radioBox, new Separator(), dateContainer, lblTime, timeBox, new Separator(), btnSave, btnCancel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f8f9fa;");

        window.setScene(new Scene(layout));
        window.showAndWait();
    }

    private void styleDatePicker(DatePicker dp) {
        dp.setEditable(false);
        dp.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now().minusDays(30))) { 
                    setDisable(true); 
                    setStyle("-fx-background-color: #eeeeee;"); 
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        setAlertIcon(alert);
        alert.showAndWait();
    }
    
    private void setAlertIcon(Alert alert) {
        try {
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().addAll(
                new javafx.scene.image.Image(getClass().getResource("/logo_16.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_32.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_48.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_64.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_128.png").toExternalForm()),
                new javafx.scene.image.Image(getClass().getResource("/logo_256.png").toExternalForm())
            );
        } catch (Exception e) {
            System.out.println("Logo not found!");
        }
    }
}