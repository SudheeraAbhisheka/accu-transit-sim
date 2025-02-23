package org.example.multidisplaycore.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.example.multidisplaycore.config.AppConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class CombinedApplication extends Application {

    private ApplicationContext context;

    @Override
    public void init() {
        // Initialize the Spring ApplicationContext using your AppConfig.
        context = new AnnotationConfigApplicationContext(AppConfig.class);
    }

    @Override
    public void start(Stage primaryStage) {
        // Create a TabPane for multiple tabs.
        TabPane tabPane = new TabPane();

        // Tab 1: Bus Simulator UI
        Tab busSimulatorTab = new Tab("Bus Simulator");
        BusSimulatorUI busSimulatorUI = new BusSimulatorUI(context);
        busSimulatorTab.setContent(busSimulatorUI.getRoot());
        busSimulatorTab.setClosable(false);
        tabPane.getTabs().add(busSimulatorTab);

        // Tab 2: Speed Lookup UI (using the new SpeedLookupUI class)
        Tab speedLookupTab = new Tab("Speed Lookup");
        SpeedLookupUI speedLookupUI = new SpeedLookupUI(context);
        speedLookupTab.setContent(speedLookupUI.getRoot());
        speedLookupTab.setClosable(false);
        tabPane.getTabs().add(speedLookupTab);

        // Set up the scene and stage
        Scene scene = new Scene(tabPane, 600, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Combined Bus Simulator UI");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
