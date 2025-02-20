package org.example.multidisplaycore;

import javafx.application.Application;
import org.example.multidisplaycore.gui.CombinedApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MultiDisplayCoreApplication {

    public static void main(String[] args) {
//        SpringApplication.run(MultiDisplayCoreApplication.class, args);
        Application.launch(CombinedApplication.class, args);
    }

}
