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
    private TextField keyField;
    private Button lookupButton;
    private TextField lowestSpeedField;
    private Button lowestSpeedUpButton;
    private Button lowestSpeedDownButton;
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
        keyField = new TextField();
        keyField.setPromptText("bus number");

        lookupButton = new Button("Show Speeds");

        Label lowestSpeedLabel = new Label("Lowest speed:");
        Label highestSpeedLabel = new Label("Highest speed:");

        lowestSpeedField = new TextField();
        lowestSpeedField.setDisable(true);

        highestSpeedField = new TextField();
        highestSpeedField.setDisable(true);

        lowestSpeedUpButton = new Button("▲");
        lowestSpeedDownButton = new Button("▼");

        highestSpeedUpButton = new Button("▲");
        highestSpeedDownButton = new Button("▼");

        HBox lowestSpeedBox = new HBox(5, lowestSpeedUpButton, lowestSpeedDownButton, lowestSpeedField);
        HBox highestSpeedBox = new HBox(5, highestSpeedUpButton, highestSpeedDownButton, highestSpeedField);

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
                lowestSpeedField.setText(String.valueOf(speeds[0]));
                highestSpeedField.setText(String.valueOf(speeds[1]));

                lowestSpeedUpButton.setOnAction(e -> {
                    double newLowestSpeed = speeds[0] + 0.5;
                    if (newLowestSpeed > speeds[1]) {
                        showAlert("Lowest speed cannot exceed the highest speed.");
                    } else {
                        speeds[0] = newLowestSpeed;
                        if (serviceCore.getSpeeds().containsKey(key)) {
                            serviceCore.getSpeeds().put(key, speeds);
                        } else {
                            showAlert("No speeds found for key: " + key);
                        }
                        lowestSpeedField.setText(String.valueOf(speeds[0]));
                    }
                });

                lowestSpeedDownButton.setOnAction(e -> {
                    speeds[0] -= 0.5;
                    if (serviceCore.getSpeeds().containsKey(key)) {
                        serviceCore.getSpeeds().put(key, speeds);
                    } else {
                        showAlert("No speeds found for key: " + key);
                    }
                    lowestSpeedField.setText(String.valueOf(speeds[0]));
                });

                highestSpeedUpButton.setOnAction(e -> {
                    speeds[1] += 0.5;
                    if (serviceCore.getSpeeds().containsKey(key)) {
                        serviceCore.getSpeeds().put(key, speeds);
                    } else {
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
                        if (serviceCore.getSpeeds().containsKey(key)) {
                            serviceCore.getSpeeds().put(key, speeds);
                        } else {
                            showAlert("No speeds found for key: " + key);
                        }
                        highestSpeedField.setText(String.valueOf(speeds[1]));
                    }
                });
            }
        });

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
