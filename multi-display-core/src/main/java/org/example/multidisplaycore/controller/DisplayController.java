package org.example.multidisplaycore.controller;

import org.example.multidisplaycore.service.Bus;
import org.example.multidisplaycore.service.ServiceCore;
import org.example.multidisplaycore.service.Coordinate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DisplayController {
    private final ServiceCore serviceCore;

    public DisplayController(ServiceCore serviceCore) {
        this.serviceCore = serviceCore;
    }

    @GetMapping("/get-available-routes")
    public Map<String, Double> getAvailableRoutes() {
        return serviceCore.getRouteLength();
    }

    @PostMapping("start-core")
    public ResponseEntity<String> startCore(@RequestBody List<Bus> buses) {
        /*ArrayList<Bus> buses_ = new ArrayList<>(List.of(
                new Bus("1", 45.5, 55.5, 2),
                new Bus("2", 45.5, 65.5, 6),
                new Bus("2", 45.5, 75.5, 18)
        ));*/

        for(Bus bus : buses) {
            serviceCore.simulator(bus.getRouteId(), bus.getLowestSpeed(), bus.getHighestSpeed(), bus.getStartAfter());
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
