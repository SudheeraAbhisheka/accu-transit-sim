package org.example.multidisplaycore.gui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.multidisplaycore.service.ServiceCore;
import org.springframework.context.ApplicationContext;

public class SpeedLookupUI {

    private final ServiceCore serviceCore;
    private final VBox root;

    // Keep references to controls we need to update
    private TextField keyField;
    private Button lookupButton;

    // UI elements for the lowest speed
    private TextField lowestSpeedField;
    private Button lowestSpeedUpButton;
    private Button lowestSpeedDownButton;

    // UI elements for the highest speed
    private TextField highestSpeedField;
    private Button highestSpeedUpButton;
    private Button highestSpeedDownButton;

    public SpeedLookupUI(ApplicationContext context) {
        this.serviceCore = context.getBean(ServiceCore.class);
        root = new VBox(10);
        root.setPadding(new Insets(15));
        createUI();
    }

    private void createUI() {
        // Field for the key
        keyField = new TextField();
        keyField.setPromptText("bus number");

        // Button to look up speeds
        lookupButton = new Button("Show Speeds");

        // Create labels for the two speeds
        Label lowestSpeedLabel = new Label("Lowest speed:");
        Label highestSpeedLabel = new Label("Highest speed:");

        // Create text fields for displaying the speeds
        lowestSpeedField = new TextField();
        lowestSpeedField.setDisable(true);  // user should not type directly

        highestSpeedField = new TextField();
        highestSpeedField.setDisable(true); // user should not type directly

        // Create arrow buttons for the lowest speed
        lowestSpeedUpButton = new Button("▲");
        lowestSpeedDownButton = new Button("▼");

        // Create arrow buttons for the highest speed
        highestSpeedUpButton = new Button("▲");
        highestSpeedDownButton = new Button("▼");

        // Group the arrow buttons + text field for each speed
        HBox lowestSpeedBox = new HBox(5, lowestSpeedUpButton, lowestSpeedDownButton, lowestSpeedField);
        HBox highestSpeedBox = new HBox(5, highestSpeedUpButton, highestSpeedDownButton, highestSpeedField);

        // When the user presses "Show Speeds", fetch speeds and show them
        lookupButton.setOnAction(event -> {
            String key = keyField.getText();
            if (key == null || key.isEmpty()) {
                showAlert("Please enter a key.");
                return;
            }

            Double[] speeds = serviceCore.getSpeeds().get(key);
            if (speeds == null) {
                showAlert("No speeds found for key: " + key);
            } else {
                // Display the current speeds in the text fields
                lowestSpeedField.setText(String.valueOf(speeds[0]));
                highestSpeedField.setText(String.valueOf(speeds[1]));

                // Set up the arrow buttons to adjust the speeds by ±0.5
                lowestSpeedUpButton.setOnAction(e -> {
                    double newLowestSpeed = speeds[0] + 0.5;
                    if (newLowestSpeed > speeds[1]) {
                        showAlert("Lowest speed cannot exceed the highest speed.");
                    } else {
                        speeds[0] = newLowestSpeed;
                        if(serviceCore.getSpeeds().containsKey(key)){
                            serviceCore.getSpeeds().put(key, speeds);
                        }
                        else{
                            showAlert("No speeds found for key: " + key);
                        }
                        lowestSpeedField.setText(String.valueOf(speeds[0]));
                    }
                });

                lowestSpeedDownButton.setOnAction(e -> {
                    speeds[0] -= 0.5;
                    if(serviceCore.getSpeeds().containsKey(key)){
                        serviceCore.getSpeeds().put(key, speeds);
                    }
                    else{
                        showAlert("No speeds found for key: " + key);
                    }
                    lowestSpeedField.setText(String.valueOf(speeds[0]));
                });

                highestSpeedUpButton.setOnAction(e -> {
                    speeds[1] += 0.5;
                    if(serviceCore.getSpeeds().containsKey(key)){
                        serviceCore.getSpeeds().put(key, speeds);
                    }
                    else{
                        showAlert("No speeds found for key: " + key);
                    }
                    highestSpeedField.setText(String.valueOf(speeds[1]));
                });

                highestSpeedDownButton.setOnAction(e -> {
                    double newHighestSpeed = speeds[1] - 0.5;
                    if (newHighestSpeed < speeds[0]) {
                        showAlert("Highest speed cannot be lower than the lowest speed.");
                    } else {
                        speeds[1] = newHighestSpeed;
                        if(serviceCore.getSpeeds().containsKey(key)){
                            serviceCore.getSpeeds().put(key, speeds);
                        }
                        else{
                            showAlert("No speeds found for key: " + key);
                        }
                        highestSpeedField.setText(String.valueOf(speeds[1]));
                    }
                });
            }
        });

        // Add everything to the root layout
        root.getChildren().addAll(
                new Label("Enter Bus Number:"),
                keyField,
                lookupButton,
                lowestSpeedLabel,
                lowestSpeedBox,
                highestSpeedLabel,
                highestSpeedBox
        );
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Parent getRoot() {
        return root;
    }
}
