package org.example.transit_operations_center.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CentralHub {
    @Getter
    private final Map<String, Coordinate> coordinatesMap = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, ArrayList<Coordinate>> routes = new HashMap<>();
    @Getter
    private final Map<String, ArrayList<Coordinate>> bus_stops_map = new HashMap<>();
    @Getter
    private Map<String, String> routeOfBus = new HashMap<>();
    @Getter
    private Map<String, StopInfo> stopInfoMap = new HashMap<>();
    @Getter
    private final Map<String, String> stateOfTheBus = new HashMap<>();

    public CentralHub() {
        ObjectMapper objectMapper = new ObjectMapper();
        File folder = null;
        try {
            folder = new ClassPathResource("data").getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

            if (files != null) {
                for (File file : files) {
                    try {
                        JsonNode rootNode = objectMapper.readTree(file);
                        String routeId = rootNode.get("route_id").asText();
                        ArrayList<Coordinate> route = new ArrayList<>();
                        ArrayList<Coordinate> bus_stops_list = new ArrayList<>();


                        for (JsonNode node : rootNode.get("route")) {
                            double x = node.get("x").asDouble();
                            double y = node.get("y").asDouble();
                            route.add(new Coordinate(x, y));
                        }

                        routes.put(routeId, route);

                        for (JsonNode node : rootNode.get("bus_stops")) {
                            double x = node.get("x").asDouble();
                            double y = node.get("y").asDouble();
                            bus_stops_list.add(new Coordinate(x, y));
                        }

                        bus_stops_map.put(routeId, bus_stops_list);

                    } catch (IOException e) {
                        System.err.println("Error reading file: " + file.getName() + " -> " + e.getMessage());
                    }
                }
            }
        } else {
            System.err.println("Folder path is invalid!");
        }
    }


}
