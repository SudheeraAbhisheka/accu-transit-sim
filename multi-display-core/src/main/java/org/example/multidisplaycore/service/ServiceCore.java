package org.example.multidisplaycore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ServiceCore {
    private final RestTemplate restTemplate;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(200);;

    private final Map<String, Deque<DistanceAndAngle>> routes = new HashMap<>();
    private int count = 0;
    private final Map<String, Queue<Coordinate>> bus_stops_map = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, Double[]> speeds = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, Double> routeLength = new HashMap<>(Map.of(
            "1", -122.2,
            "2", -222.3
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

                        Iterator<JsonNode> originalIterator = rootNode.get("route").elements();
                        List<JsonNode> nodes = new ArrayList<>();
                        originalIterator.forEachRemaining(nodes::add);

                        List<JsonNode> forwardNodes = new ArrayList<>(nodes);
                        Iterator<JsonNode> forwardIterator = forwardNodes.iterator();

                        List<JsonNode> reversedNodes = new ArrayList<>(nodes);
                        Collections.reverse(reversedNodes);
                        Iterator<JsonNode> reverseIterator = reversedNodes.iterator();

                        ArrayNode forwardBusStops = (ArrayNode) rootNode.get("bus_stops");
                        ObjectMapper mapper = new ObjectMapper();
                        ArrayNode reversedBusStops = mapper.createArrayNode();
                        for (int i = forwardBusStops.size() - 1; i >= 0; i--) {
                            reversedBusStops.add(forwardBusStops.get(i));
                        }

                        forwardAndReversed(routeId, forwardIterator, forwardBusStops);
                        forwardAndReversed(routeId+"R", reverseIterator, reversedBusStops);

                    } catch (IOException e) {
                        System.err.println("Error reading file: " + file.getName() + " -> " + e.getMessage());
                    }
                }

            }
        } else {
            System.err.println("Folder path is invalid!");
        }

//        simulator("1", 45.5, 55.5, 2);
//        simulator("1R", 45.5, 55.5, 2);

    }

    private void forwardAndReversed(String routeId, Iterator<JsonNode> iterator, ArrayNode busStops){
        Queue<Coordinate> bus_stops_list = new LinkedList<>();
        Deque<DistanceAndAngle> route = new ArrayDeque<>();
        PeekingIterator<JsonNode> peekingIterator = Iterators.peekingIterator(iterator);

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

        for (JsonNode node : busStops) {
            double x = node.get("x").asDouble();
            double y = node.get("y").asDouble();
            bus_stops_list.add(new Coordinate(x, y));
        }

        routes.put(routeId, route);
        bus_stops_map.put(routeId, bus_stops_list);
        routeLength.put(routeId, totalLength);
    }

    public void simulator(String routeId, double lowestSpeed, double highestSpeed, long startAfter){
        AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();
        String busNumber = "" + (++count);

        long currentTimeMillis = System.currentTimeMillis();
        long futureTimeMillis = currentTimeMillis + (startAfter * 1000);
        Date futureDate = new Date(futureTimeMillis);
        sendStateOfBus(busNumber, "scheduled_to_start: " + futureDate);

        Deque<DistanceAndAngle> route = new ArrayDeque<>(routes.get(routeId));

        Queue<Coordinate> bus_stops_list = new LinkedList<>(bus_stops_map.get(routeId));
        Map<Coordinate, Integer> maxSteps = new HashMap<>();
        AtomicReference<Coordinate> busStop = new AtomicReference<>();

        speeds.put(busNumber, new Double[]{lowestSpeed, highestSpeed});

        busStop.set(bus_stops_list.poll());
        if(busStop.get() == null){
            System.err.println("Bus stops, not found!");
        }

        future.set(
                scheduler.scheduleAtFixedRate(() -> {
                    double distanceInASecond;

                    sendStateOfBus(busNumber, "in_transit");

                    if(Objects.equals(speeds.get(busNumber)[0], speeds.get(busNumber)[1])){
                        distanceInASecond = speeds.get(busNumber)[0];
                    }
                    else{
                        distanceInASecond = ThreadLocalRandom.current().nextDouble(speeds.get(busNumber)[0], speeds.get(busNumber)[1]);
                    }

                    DistanceAndAngle distanceAndAngle;
                    double totalDistance = 0;

                    do{
                        distanceAndAngle = route.poll();

                        if(distanceAndAngle == null){
                            long currentTimeMillis2 = System.currentTimeMillis();
                            Date futureDate2 = new Date(currentTimeMillis2);
                            long differenceInMillis = currentTimeMillis2 - futureTimeMillis;
                            double differenceInMinutes = differenceInMillis / 1000.0;

                            System.out.println(currentTimeMillis2);
                            System.out.println(futureTimeMillis);
                            System.out.println(differenceInMinutes);

                            String s = String.format("destination_reached: %s, time_taken: %s, departure_at: %s",
                                    futureDate2, differenceInMinutes, futureDate);
                            sendStateOfBus(busNumber, s);

                            future.get().cancel(false);
                            return;
                        }

                        totalDistance += distanceAndAngle.getDistance();

                    }while(distanceInASecond > totalDistance);

                    double dToNextCoordinate = totalDistance - distanceInASecond;
                    double dFromLastCoordinate = distanceAndAngle.getDistance() - dToNextCoordinate;

                    double angleRadians = distanceAndAngle.getAngle();
                    double fromX = distanceAndAngle.getCoordinate().getX();
                    double fromY = distanceAndAngle.getCoordinate().getY();

                    double x = dFromLastCoordinate * Math.cos(angleRadians) + fromX;
                    double y = dFromLastCoordinate * Math.sin(angleRadians) + fromY;
                    Coordinate coordinate = new Coordinate(x, y);

                    sendCoordinate(busNumber, coordinate);
//                    sendSpeed(busNumber, distanceInASecond);

                    DistanceAndAngle distanceAndAngle1 = new DistanceAndAngle(coordinate, dToNextCoordinate, angleRadians);
                    route.push(distanceAndAngle1);

                    double distance = 0;

                    if(busStop.get() != null){
                        distance = distanceForNextStop(busStop.get(), route, maxSteps);
                    }

                    while(distance <= 0){
                        busStop.set(bus_stops_list.poll());
                        if(busStop.get() == null){
                            break;
                        }
                        else{
                            distance = distanceForNextStop(busStop.get(), route, maxSteps);
                        }
                    }

                    if(busStop.get() == null){
                        sendDistanceNextStop(busNumber, new Coordinate(-1, -1), 0);
                    }else{
                        sendDistanceNextStop(busNumber, busStop.get(), distance);
                    }

//                    System.out.printf("current location: (%.1f, %.1f), bus stop: %s, distance: %.1f\n",
//                            coordinate.getX(), coordinate.getY(), busStop, distance);

                }, startAfter, 1, TimeUnit.SECONDS)
        );

        scheduler.scheduleAtFixedRate(() -> {
//            System.out.println(coordinatesMap);
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private boolean sendCoordinate(String busNumber, Coordinate coordinate){
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

    private boolean sendDistanceNextStop(String busNumber, Coordinate coordinate, double distanceNextStop){
        String url = "http://localhost:8080/api/set-stop-info?busNumber=" + busNumber;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        StopInfo stopInfo = new StopInfo(coordinate, distanceNextStop);
        HttpEntity<StopInfo> request = new HttpEntity<>(stopInfo, headers);

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

    private double distanceForNextStop(Coordinate nextStop, Deque<DistanceAndAngle> route, Map<Coordinate, Integer> stepsCount){
        double distance = 0;
        int count = 0;
        int maxSteps = stepsCount.getOrDefault(nextStop, Integer.MAX_VALUE);

        for(DistanceAndAngle distanceAndAngle : route){
            Coordinate coordinate = distanceAndAngle.getCoordinate();

            if(coordinate.equals(nextStop)){
                if(!stepsCount.containsKey(coordinate)){
                   stepsCount.put(coordinate, count);
                }

                return distance;
            }
            else{
                if(maxSteps < count){
                    return 0.0;
                }
                distance += distanceAndAngle.getDistance();
                count++;
            }
        }

        return 0.0;
    }

    private boolean sendStateOfBus(String busNumber, String state){
        String url = "http://localhost:8080/api/set-bus-state?busNumber=" + busNumber;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(state, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}