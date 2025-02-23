package org.example.multidisplaycore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import lombok.Getter;
import lombok.Setter;
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
    private int count = 0;
    private final Map<String, Queue<Coordinate>> bus_stops_map = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, Double[]> speeds = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, Double> routeLength = new HashMap<>(Map.of(
            "1", -122.2,
            "2", -222.3
    ));
    @Setter
    private CountDownLatch latch;

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
/*
    public void simulator(String routeId, double lowestSpeed, double highestSpeed, long startAfter) {
        simulator(routeId, lowestSpeed, highestSpeed, startAfter, null);
    }*/

    public void simulator(String routeId, double lowestSpeed, double highestSpeed, long startAfter){
        final long SCHEDULING_PERIOD = 1000;
        AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();
        String busNumber = "" + (++count);

        long currentTimeMillis = System.currentTimeMillis();
        long futureTimeMillis = currentTimeMillis + (startAfter * 1000);
        Date futureDate = new Date(futureTimeMillis);
        postRequest("set-bus-state?busNumber=" + busNumber, "scheduled_to_start: " + futureDate);

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

                    postRequest("set-bus-state?busNumber=" + busNumber, "in_transit");

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

                            String s = String.format("destination_reached: %s, time_taken: %s, departure_at: %s",
                                    futureDate2, differenceInMinutes, futureDate);
                            postRequest("set-bus-state?busNumber=" + busNumber, s);

                            speeds.remove(busNumber);

                            future.get().cancel(false);
                            if (latch != null) {
                                System.out.println("latch count down: " + latch.getCount());
                                latch.countDown();
                            }
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

                    postRequest("update-coordinate?busNumber=" + busNumber, coordinate);

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
                        StopInfo stopInfo = new StopInfo(new Coordinate(-1, -1), 0);
                        postRequest("set-stop-info?busNumber=" + busNumber, stopInfo);
                    }else{
                        StopInfo stopInfo = new StopInfo(busStop.get(), distance);
                        postRequest("set-stop-info?busNumber=" + busNumber, stopInfo);
                    }

//                    System.out.printf("current location: (%.1f, %.1f), bus stop: %s, distance: %.1f\n",
//                            coordinate.getX(), coordinate.getY(), busStop, distance);

                }, startAfter*1000, SCHEDULING_PERIOD, TimeUnit.MILLISECONDS)
        );
    }

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

    private <T> void postRequest(String suffix, T payload) {
        String url = "http://localhost:8080/api/" + suffix;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<T> request = new HttpEntity<>(payload, headers);
        try {
            restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        } catch (Exception e) {
//            logger.error("Error during POST to {}: {}", url, e.getMessage(), e);
        }
    }
}