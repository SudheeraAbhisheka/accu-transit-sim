//package org.example.multidisplaycore.service;
//
//import javafx.application.Application;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.geometry.Insets;
//import javafx.scene.Scene;
//import javafx.scene.control.*;
//import javafx.scene.layout.VBox;
//import javafx.stage.Stage;
//import org.example.multidisplaycore.config.AppConfig;
//import org.example.multidisplaycore.service.Bus;
//import org.example.multidisplaycore.service.ServiceCore;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.annotation.AnnotationConfigApplicationContext;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
//public class Ui extends Application {
//
//    private ServiceCore serviceCore;
//    private Map<String, Double> routeLengths;
//    private final ArrayList<Bus> buses = new ArrayList<>();
//
//    @Override
//    public void init() {
//        // Initialize your Spring Boot ApplicationContext.
//        ApplicationContext context = getApplicationContext();
//        this.serviceCore = context.getBean(ServiceCore.class);
//        this.routeLengths = serviceCore.getRouteLength();
//    }
//
//    @Override
//    public void start(Stage primaryStage) {
//        // ComboBox for selecting a routeId from routeLengths keys.
//        ComboBox<String> routeComboBox = new ComboBox<>();
//        List<String> sortedRoutes = new ArrayList<>(routeLengths.keySet());
//        Collections.sort(sortedRoutes);
//        routeComboBox.getItems().addAll(sortedRoutes);
//        routeComboBox.setPromptText("Select a route");
//
//        // Label to display route length when a route is selected.
//        Label routeLengthLabel = new Label("Route Length: ");
//
//        // Add a listener to update the label whenever the selected route changes.
//        routeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
//            if (newValue != null && routeLengths.containsKey(newValue)) {
//                routeLengthLabel.setText("Route Length: " + routeLengths.get(newValue));
//            } else {
//                routeLengthLabel.setText("Route Length: ");
//            }
//        });
//
//        // TextFields for bus parameters.
//        TextField lowestSpeedField = new TextField();
//        lowestSpeedField.setPromptText("Lowest Speed");
//
//        TextField highestSpeedField = new TextField();
//        highestSpeedField.setPromptText("Highest Speed");
//
//        TextField startAfterField = new TextField();
//        startAfterField.setPromptText("Start After (seconds)");
//
//        // Button to add a Bus based on user input.
//        Button addBusButton = new Button("Add Bus");
//
//        // ListView to display added buses.
//        ObservableList<String> busListItems = FXCollections.observableArrayList();
//        ListView<String> busListView = new ListView<>(busListItems);
//
//        // TextArea to display simulation results.
//        TextArea simulationResultsTextArea = new TextArea();
//        simulationResultsTextArea.setEditable(false);
//        simulationResultsTextArea.setWrapText(true);
//        simulationResultsTextArea.setPromptText("Simulation results...");
//
//        addBusButton.setOnAction(event -> {
//            String routeId = routeComboBox.getValue();
//            if (routeId == null || routeId.isEmpty()) {
//                showAlert("Please select a route.");
//                return;
//            }
//            try {
//                double lowestSpeed = Double.parseDouble(lowestSpeedField.getText());
//                double highestSpeed = Double.parseDouble(highestSpeedField.getText());
//
//                if (highestSpeed <= lowestSpeed) {
//                    showAlert("Highest speed must be greater than the lowest speed.");
//                    return;
//                }
//
//                long startAfter = Long.parseLong(startAfterField.getText());
//
//                // Create a new Bus instance and add it to the list.
//                Bus bus = new Bus(routeId, lowestSpeed, highestSpeed, startAfter);
//                buses.add(bus);
//                busListItems.add("Route: " + routeId +
//                        ", Speed: " + lowestSpeed + "-" + highestSpeed +
//                        ", Start After: " + startAfter);
//
//                // Clear the input fields after adding.
//                lowestSpeedField.clear();
//                highestSpeedField.clear();
//                startAfterField.clear();
//            } catch (NumberFormatException ex) {
//                showAlert("Please enter valid numeric values for speeds and start time.");
//            }
//        });
//
//        // Button to simulate all added buses.
//        Button simulateButton = new Button("Simulate Buses");
//        simulateButton.setOnAction(event -> {
//            if (buses.isEmpty()) {
//                simulationResultsTextArea.setText("No buses added for simulation.");
//                return;
//            }
//            // Iterate through the list of buses and invoke the simulator method.
//            for (Bus bus : buses) {
//                serviceCore.simulator(
//                        bus.getRouteId(),
//                        bus.getLowestSpeed(),
//                        bus.getHighestSpeed(),
//                        bus.getStartAfter());
//            }
//            simulationResultsTextArea.appendText("Simulation started for " + buses.size() + " bus(es).\n");
//            buses.clear();
//        });
//
//        // Layout setup using VBox.
//        VBox root = new VBox(10);
//        root.setPadding(new Insets(15));
//        root.getChildren().addAll(
//                new Label("Select Route:"),
//                routeComboBox,
//                routeLengthLabel, // Added label to display route length.
//                new Label("Lowest Speed:"),
//                lowestSpeedField,
//                new Label("Highest Speed:"),
//                highestSpeedField,
//                new Label("Start After (seconds):"),
//                startAfterField,
//                addBusButton,
//                new Label("Buses to Simulate:"),
//                busListView,
//                simulateButton,
//                new Label("Simulation Results:"),
//                simulationResultsTextArea
//        );
//
//        Scene scene = new Scene(root, 400, 600);
//        primaryStage.setScene(scene);
//        primaryStage.setTitle("Bus Simulator");
//        primaryStage.show();
//    }
//
//    /**
//     * Helper method to show an alert with a given message.
//     */
//    private void showAlert(String message) {
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("Information");
//        alert.setHeaderText(null);
//        alert.setContentText(message);
//        alert.showAndWait();
//    }
//
//    /**
//     * Dummy method to illustrate obtaining the Spring ApplicationContext.
//     * Replace with your actual Spring Boot initialization logic.
//     */
//    private ApplicationContext getApplicationContext() {
//        return new AnnotationConfigApplicationContext(AppConfig.class);
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}
