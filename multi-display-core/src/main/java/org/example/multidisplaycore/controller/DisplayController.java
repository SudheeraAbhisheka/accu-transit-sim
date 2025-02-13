package org.example.multidisplaycore.controller;

import org.example.multidisplaycore.service.ServiceCore;
import org.example.multidisplaycore.service.Coordinate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DisplayController {
    private final ServiceCore serviceCore;
    private final Map<String, Coordinate> coordinates;

    public DisplayController(ServiceCore serviceCore) {
        this.serviceCore = serviceCore;
        this.coordinates = serviceCore.getCoordinatesMap();
    }

    @PostMapping("start-core")
    public void startCore() {
    }

    @GetMapping("/get-coordinate")
    public Map<String, Coordinate> getDisplayOne() {
        return coordinates;
    }

    @GetMapping("/get-lowest-coordinate")
    public Coordinate getLowestCoordinate() {
        return new Coordinate(0.0, 0.0);
    }

    @GetMapping("/get-highest-coordinate")
    public Coordinate getHighestCoordinate() {
        return new Coordinate(10000, 1000);
    }
}
