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

    public BusSimulatorUI(ApplicationContext context) {
        // Retrieve the ServiceCore bean from Spring
        this.serviceCore = context.getBean(ServiceCore.class);
        // Get the route lengths from the service
        this.routeLengths = serviceCore.getRouteLength();
        // Build the UI
        root = new VBox(10);
        root.setPadding(new Insets(15));
        createUI();
    }

    private void createUI() {
        // 1. ComboBox for route selection
        ComboBox<String> routeComboBox = new ComboBox<>();
        List<String> sortedRoutes = new ArrayList<>(routeLengths.keySet());
        Collections.sort(sortedRoutes);
        routeComboBox.getItems().addAll(sortedRoutes);
        routeComboBox.setPromptText("Select a route");

        // 2. Label to display route length
        Label routeLengthLabel = new Label("Route Length: ");
        routeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && routeLengths.containsKey(newVal)) {
                routeLengthLabel.setText("Route Length: " + routeLengths.get(newVal));
            } else {
                routeLengthLabel.setText("Route Length: ");
            }
        });

        // 3. Input fields for bus parameters
        TextField lowestSpeedField = new TextField();
        lowestSpeedField.setPromptText("Lowest Speed");

        TextField highestSpeedField = new TextField();
        highestSpeedField.setPromptText("Highest Speed");

        TextField startAfterField = new TextField();
        startAfterField.setPromptText("Start After (seconds)");

        // 4. Button to add a bus
        Button addBusButton = new Button("Add Bus");
        ObservableList<String> busListItems = FXCollections.observableArrayList();
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
                    showAlert("Highest speed must be greater than or equal the lowest speed.");
                    return;
                }
                long startAfter = Long.parseLong(startAfterField.getText());

                // Create and store a new Bus instance
                Bus bus = new Bus(routeId, lowestSpeed, highestSpeed, startAfter);
                buses.add(bus);
                busListItems.add("Route: " + routeId +
                        ", Speed: " + lowestSpeed + "-" + highestSpeed +
                        ", Start After: " + startAfter);


            } catch (NumberFormatException ex) {
                showAlert("Please enter valid numeric values for speeds and start time.");
            }
        });

        // 5. Button to simulate added buses
        TextArea simulationResultsTextArea = new TextArea();
        simulationResultsTextArea.setEditable(false);
        simulationResultsTextArea.setWrapText(true);
        simulationResultsTextArea.setPromptText("Simulation results...");

        Button simulateButton = new Button("Simulate Buses");
        simulateButton.setOnAction(event -> {
            if (buses.isEmpty()) {
                simulationResultsTextArea.setText("No buses added for simulation.");
                return;
            }
            for (Bus bus : buses) {
                serviceCore.simulator(
                        bus.getRouteId(),
                        bus.getLowestSpeed(),
                        bus.getHighestSpeed(),
                        bus.getStartAfter());
            }
            simulationResultsTextArea.appendText("Simulation started for " + buses.size() + " bus(es).\n");
            buses.clear();
        });

        // 6. Assemble the layout
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
                new Label("Buses to Simulate:"),
                busListView,
                simulateButton,
                new Label("Simulation Results:"),
                simulationResultsTextArea
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
