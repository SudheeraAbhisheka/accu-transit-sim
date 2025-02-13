package org.example.transit_operations_center.controller;

import org.example.transit_operations_center.service.Coordinate;
import org.example.transit_operations_center.service.MapCreation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class Controller {
    private final MapCreation mapCreation;
    private Map<String, ArrayList<Coordinate>> routes;
    private Map<String, ArrayList<Coordinate>> busStops;

    public Controller(MapCreation mapCreation) {
        this.mapCreation = mapCreation;
        routes = mapCreation.getRoutes();
        busStops = mapCreation.getBus_stops_map();
    }

    @GetMapping("/get-routes")
    public Map<String, ArrayList<Coordinate>> getRoutes() {
        return routes;
    }

    @GetMapping("/get-bus-stops")
    public Map<String, ArrayList<Coordinate>> getBusStops() {
        return busStops;
    }
}
