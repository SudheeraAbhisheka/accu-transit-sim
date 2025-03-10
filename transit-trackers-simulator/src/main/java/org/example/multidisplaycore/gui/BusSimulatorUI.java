package org.example.multidisplaycore.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.multidisplaycore.service.Bus;
import org.example.multidisplaycore.service.ServiceCore;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BusSimulatorUI {

    private final ServiceCore serviceCore;
    private final Map<String, Double> routeLengths;
    private final ArrayList<Bus> buses = new ArrayList<>();
    private final VBox root;
    private final ObservableList<String> busListItems = FXCollections.observableArrayList();

    public BusSimulatorUI(ApplicationContext context) {
        this.serviceCore = context.getBean(ServiceCore.class);
        this.routeLengths = serviceCore.getRouteLength();
        root = new VBox(10);
        root.setPadding(new Insets(15));
        createUI();
    }

    private void createUI() {
        ComboBox<String> routeComboBox = new ComboBox<>();
        List<String> sortedRoutes = new ArrayList<>(routeLengths.keySet());
        Collections.sort(sortedRoutes);
        routeComboBox.getItems().addAll(sortedRoutes);
        routeComboBox.setPromptText("Select a route");

        Label routeLengthLabel = new Label("Route Length: ");
        routeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && routeLengths.containsKey(newVal)) {
                routeLengthLabel.setText("Route Length: " + routeLengths.get(newVal));
            } else {
                routeLengthLabel.setText("Route Length: ");
            }
        });

        TextField lowestSpeedField = new TextField();
        lowestSpeedField.setPromptText("units per second");

        TextField highestSpeedField = new TextField();
        highestSpeedField.setPromptText("units per second");

        TextField startAfterField = new TextField();
        startAfterField.setPromptText("seconds");

        Button addBusButton = new Button("Add Bus");
        ListView<String> busListView = new ListView<>(busListItems);

        addBusButton.setOnAction(event -> {
            String routeId = routeComboBox.getValue();
            if (routeId == null || routeId.isEmpty()) {
                showAlert("Please select a route.");
                return;
            }
            try {
                double lowestSpeed = Double.parseDouble(lowestSpeedField.getText());
                double highestSpeed = Double.parseDouble(highestSpeedField.getText());
                if (highestSpeed < lowestSpeed) {
                    showAlert("Highest speed must be greater than or equal to the lowest speed.");
                    return;
                }
                long startAfter = Long.parseLong(startAfterField.getText());

                Bus bus = new Bus(routeId, lowestSpeed, highestSpeed, startAfter);
                buses.add(bus);
                busListItems.add("Route: " + routeId +
                        ", Speed: " + lowestSpeed + "-" + highestSpeed +
                        ", Start After: " + startAfter);

            } catch (NumberFormatException ex) {
                showAlert("Please enter valid numeric values for speeds and start time.");
            }
        });


        Button simulateButton = new Button("Simulate Buses");
        simulateButton.setOnAction(event -> {
            if (buses.isEmpty()) {
                String noBusMsg = "No buses added for simulation.\n";
                busListItems.add(noBusMsg);
                return;
            }
            for (Bus bus : buses) {
                serviceCore.simulator(
                        bus.getRouteId(),
                        bus.getLowestSpeed(),
                        bus.getHighestSpeed(),
                        bus.getStartAfter());
            }
            String simulationMessage = "Simulation started for " + buses.size() + " bus(es).\n";
            busListItems.add(simulationMessage);
            buses.clear();
        });

        root.getChildren().addAll(
                new Label("Select Route:"),
                routeComboBox,
                routeLengthLabel,
                new Label("Lowest Speed:"),
                lowestSpeedField,
                new Label("Highest Speed:"),
                highestSpeedField,
                new Label("Start After (seconds):"),
                startAfterField,
                addBusButton,
                // Changed heading from "Buses to Simulate:" to "Simulated Buses:"
                new Label("Simulated Buses:"),
                busListView,
                simulateButton
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
