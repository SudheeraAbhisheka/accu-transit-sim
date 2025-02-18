package org.example.multidisplaycore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import lombok.Getter;
import org.checkerframework.checker.units.qual.Speed;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ServiceCore {
    private final RestTemplate restTemplate;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(200);;

    private final Map<String, Deque<DistanceAndAngle>> routes = new HashMap<>();
    private final Map<String, Coordinate> coordinatesMap = new ConcurrentHashMap<>();
    private int count = 0;
    private final Map<String, Queue<Coordinate>> bus_stops_map = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, Double> routeLength = new HashMap<>(Map.of(
            "1", 122.2,
            "2", 222.3
    ));

    public ServiceCore(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
                        Deque<DistanceAndAngle> route = new ArrayDeque<>();

                        Iterator<JsonNode> iterator = rootNode.get("route").elements();
                        PeekingIterator<JsonNode> peekingIterator = Iterators.peekingIterator(iterator);

                        Queue<Coordinate> bus_stops_list = new LinkedList<>();

                        double totalLength = 0;

                        while (peekingIterator.hasNext()) {
                            JsonNode node = peekingIterator.next();
                            double x = node.get("x").asDouble();
                            double y = node.get("y").asDouble();
                            Coordinate coordinate = new Coordinate(x, y);

                            if (peekingIterator.hasNext()) {
                                JsonNode nextNode = peekingIterator.peek();
                                double nextX = nextNode.get("x").asDouble();
                                double nextY = nextNode.get("y").asDouble();

                                double lx = nextX - x;
                                double ly = nextY - y;

                                double distance = Math.sqrt(Math.pow(lx, 2) + Math.pow(ly, 2));
                                totalLength += distance;
                                double angleRadians = Math.atan2(ly, lx);

                                DistanceAndAngle distanceAndAngle = new DistanceAndAngle(coordinate, distance, angleRadians);
                                route.add(distanceAndAngle);

//                                System.out.println(routeId + ": " + distanceAndAngle);

                            }
                            else{
                                DistanceAndAngle distanceAndAngle = new DistanceAndAngle(coordinate);
                                route.add(distanceAndAngle);

//                                System.out.println(routeId + ": " + distanceAndAngle);

                            }
                        }
                        routes.put(routeId, route);
                        routeLength.put(routeId, totalLength);

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


        simulator("3", 45.5, 55.5, 2);

    }

    Coordinate busStop;
    public void simulator(String routeId, double lowestSpeed, double highestSpeed, long startAfter){
        String busNumber = "" + (count++);
        Deque<DistanceAndAngle> route = new ArrayDeque<>(routes.get(routeId));
        AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();

        Queue<Coordinate> bus_stops_list = bus_stops_map.get(routeId);
        if(bus_stops_list == null){
            System.err.println("Bus stops, not found!");
        }
        else{
            busStop = bus_stops_list.poll();
            if(busStop == null){
                System.err.println("Bus stops, not found!");
            }
        }

        future.set(
                scheduler.scheduleAtFixedRate(() -> {
                    double distanceInASecond = ThreadLocalRandom.current().nextDouble(lowestSpeed, highestSpeed);

                    DistanceAndAngle distanceAndAngle;
                    double totalDistance = 0;

                    do{
                        distanceAndAngle = route.poll();

                        if(distanceAndAngle == null){
                            Coordinate coordinate = new Coordinate(-1.0, -1.0);
                            coordinatesMap.put(busNumber, coordinate);

                            future.get().cancel(false);
                            return;
                        }

                        totalDistance += distanceAndAngle.getDistance();

                    }while(distanceInASecond > totalDistance);

                    double newDistance3 = totalDistance - distanceInASecond;

                    totalDistance -= distanceAndAngle.getDistance();
                    double distance_s = distanceInASecond - totalDistance;

                    double angleRadians = distanceAndAngle.getAngle();
                    double fromX = distanceAndAngle.getCoordinate().getX();
                    double fromY = distanceAndAngle.getCoordinate().getY();

                    double x = distance_s * Math.cos(angleRadians) + fromX;
                    double y = distance_s * Math.sin(angleRadians) + fromY;
                    Coordinate coordinate = new Coordinate(x, y);

                    coordinatesMap.put(busNumber, coordinate);

                    sendCoordinate(busNumber, coordinate);
//                    sendSpeed(busNumber, distanceInASecond);

                    DistanceAndAngle distanceAndAngle1 = new DistanceAndAngle(coordinate, newDistance3, angleRadians);
                    route.push(distanceAndAngle1);

                    double distance;

                    distance = distanceForNextStop(busStop, new ArrayDeque<>(route));

                    while(distance <= 0){
                        System.out.println("while loop");
                        busStop = bus_stops_list.poll();
                        if(busStop == null){
                            break;
                        }
                        else{
                            distance = distanceForNextStop(busStop, new ArrayDeque<>(route));
                        }
                    }
                    System.out.printf("current location: (%.1f, %.1f), bus stop: %s, distance: %.1f\n",
                            coordinate.getX(), coordinate.getY(), busStop, distance);

                }, startAfter, 1, TimeUnit.SECONDS)
        );

        scheduler.scheduleAtFixedRate(() -> {
//            System.out.println(coordinatesMap);
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public boolean sendCoordinate(String busNumber, Coordinate coordinate){
        String url = "http://localhost:8080/api/update-coordinate?busNumber=" + busNumber;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Coordinate> request = new HttpEntity<>(coordinate, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

/*
    public boolean sendSpeed(String busNumber, double speed){
        String url = "http://localhost:8080/api/update-speed?busNumber=" + busNumber;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Double> request = new HttpEntity<>(speed, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
*/

    private double distanceForNextStop(Coordinate nextStop, Deque<DistanceAndAngle> route){
        double distance = 0;
        DistanceAndAngle distanceAndAngle;
        Coordinate coordinate;

        distanceAndAngle = route.poll();
        if(distanceAndAngle == null){
            return distance;
        }
        coordinate = distanceAndAngle.getCoordinate();

        while(nextStop.getX() > coordinate.getX() && nextStop.getY() > coordinate.getY()){
            distance += distanceAndAngle.getDistance();
            distanceAndAngle = route.poll();
            if(distanceAndAngle == null){
                return distance;
            }
            coordinate = distanceAndAngle.getCoordinate();
        }

        return distance;
    }


}