package org.example.multidisplaycore;

import javafx.application.Application;
import org.example.multidisplaycore.service.Ui;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MultiDisplayCoreApplication {

    public static void main(String[] args) {
//        SpringApplication.run(MultiDisplayCoreApplication.class, args);
        Application.launch(Ui.class, args);
    }

}
