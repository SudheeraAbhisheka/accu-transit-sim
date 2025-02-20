package org.example.transit_operations_center.controller;

import org.example.transit_operations_center.service.CentralHub;
import org.example.transit_operations_center.service.Coordinate;
import org.example.transit_operations_center.service.StopInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@RestController
@RequestMapping("/api")
public class Controller {
    private final CentralHub centralHub;
    private Map<String, ArrayList<Coordinate>> routes;
    private Map<String, ArrayList<Coordinate>> busStops;

    public Controller(CentralHub centralHub) {
        this.centralHub = centralHub;
        routes = centralHub.getRoutes();
        busStops = centralHub.getBus_stops_map();
    }

    @GetMapping("/get-coordinates")
    public Map<String, Coordinate> getDisplayOne() {
        return centralHub.getCoordinatesMap();
    }

    @GetMapping("/get-routes")
    public Map<String, ArrayList<Coordinate>> getRoutes() {
        return routes;
    }

    @GetMapping("/get-bus-stops")
    public Map<String, ArrayList<Coordinate>> getBusStops() {
        return busStops;
    }

    @GetMapping("/get-stop-info")
    public Map<String, StopInfo> getStopInfo() {
        return centralHub.getStopInfoMap();
    }

    @GetMapping("/get-bus-state")
    public Map<String, String> getStateOfBus() {
        return centralHub.getStateOfTheBus();
    }

    @PostMapping("/update-coordinate")
    public ResponseEntity<String> updateCoordinate(@RequestParam String busNumber, @RequestBody Coordinate coordinate) {
        centralHub.getCoordinatesMap().put(busNumber, coordinate);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/set-stop-info")
    public ResponseEntity<String> setStopInfo(@RequestParam String busNumber, @RequestBody StopInfo stopInfo) {
        if(stopInfo.getCoordinate().equals(new Coordinate(-1, -1))){
            centralHub.getStopInfoMap().remove(busNumber);
        }
        else{
            centralHub.getStopInfoMap().put(busNumber, stopInfo);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
/*    @PostMapping("/update-speed")
    public ResponseEntity<String> updateSpeed(@RequestParam String busNumber, @RequestBody Double speed) {
        centralHub.getSpeedMap().put(busNumber, speed);

        return new ResponseEntity<>(HttpStatus.OK);
    }*/

    @PostMapping("/set-bus-route")
    public ResponseEntity<String> setRouteOfBus(@RequestParam String busNumber, @RequestBody String routeId) {
        centralHub.getRouteOfBus().put(busNumber, routeId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/set-bus-state")
    public ResponseEntity<String> setStateOfBus(@RequestParam String busNumber, @RequestBody String state) {
        centralHub.getStateOfTheBus().put(busNumber, state);

        return new ResponseEntity<>(HttpStatus.OK);
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
