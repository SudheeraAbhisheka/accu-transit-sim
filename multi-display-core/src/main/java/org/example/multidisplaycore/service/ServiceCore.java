package org.example.multidisplaycore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ServiceCore {
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);
    private final Map<String, Deque<DistanceAndAngle>> routes = new HashMap<>();
    @Getter
    private final Map<String, Coordinate> coordinatesMap = new ConcurrentHashMap<>();

    public ServiceCore() {
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
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + file.getName() + " -> " + e.getMessage());
                    }
                }

            }
        } else {
            System.err.println("Folder path is invalid!");
        }

        ArrayList<Bus> buses = new ArrayList<>(List.of(
                new Bus("1", 45.5, 55.5, 2),
                new Bus("2", 45.5, 65.5, 6),
                new Bus("3", 45.5, 75.5, 18)
        ));

        start(buses);

//        start(startAfter, speedBetween);
    }

    public void start(ArrayList<Bus> buses){
        int count = 0;
        for(Bus bus : buses){
            String busNumber = "" + (count++);
            String routeId = bus.getRouteId();
            double lowestSpeed = bus.getLowestSpeed();
            double highestSpeed = bus.getHighestSpeed();
            long startAfter = bus.getStartAfter();

            Deque<DistanceAndAngle> route = routes.get(routeId);


            AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();

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

                        DistanceAndAngle distanceAndAngle1 = new DistanceAndAngle(coordinate, newDistance3, angleRadians);
                        route.push(distanceAndAngle1);

                    }, startAfter, 1, TimeUnit.SECONDS)
            );
        }

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println(coordinatesMap);
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }
}

/*            future.set(
                    scheduler.scheduleAtFixedRate(() -> {
                        double distanceInASecond = ThreadLocalRandom.current().nextDouble(lowestSpeed, highestSpeed);
                        DistanceAndAngle distanceAndAngle = route.peek();

                        if(distanceAndAngle == null){
                            future.get().cancel(false);
                            return;
                        }

                        double distance = distanceAndAngle.getDistance();
                        double lastDistance = 0;

                        if(distanceInASecond == distance){
                            route.poll();
                            return;

                        } else if(distanceInASecond < distance){
                            route.poll();
                        }

                        while(distanceInASecond > distance){
                            route.remove();
                            distanceAndAngle = route.peek();

                            if(distanceAndAngle == null){
                                future.get().cancel(false);
                                return;
                            }

                            lastDistance = distanceAndAngle.getDistance();

                            distance += lastDistance;
                        }

                        distance -= lastDistance;

                        double distance_s = distanceInASecond - distance;
                        double angleRadians = distanceAndAngle.getAngle();

                        double x = distance_s * Math.cos(angleRadians);
                        double y = distance_s * Math.sin(angleRadians);

                        Coordinate coordinate = new Coordinate(x, y);
                        coordinatesMap.put(busNumber, coordinate);

                        DistanceAndAngle distanceAndAngle1 = new DistanceAndAngle(coordinate, distance_s, angleRadians);
                        route.push(distanceAndAngle1);

                        System.out.println(coordinatesMap);

                    }, startAfter, 1000, TimeUnit.MILLISECONDS)
            );*/

/*            future.set(
                    scheduler.scheduleAtFixedRate(() -> {
                        double distanceInASecond = ThreadLocalRandom.current().nextDouble(lowestSpeed, highestSpeed);
                        DistanceAndAngle distanceAndAngle = route.poll();

                        if(distanceAndAngle == null){
                            future.get().cancel(false);
                            return;
                        }

                        double distance = distanceAndAngle.getDistance();
                        double lastDistance = 0;

                        while(distanceInASecond > distance){
                            distanceAndAngle = route.poll();

                            if(distanceAndAngle == null){
                                future.get().cancel(false);
                                return;
                            }

                            lastDistance = distanceAndAngle.getDistance();

                            distance += lastDistance;
                        }

                        distance -= lastDistance;
                        route.push(distanceAndAngle);

                        double distance_s = distanceInASecond - distance;
                        double angleRadians = distanceAndAngle.getAngle();

                        double x = distance_s * Math.cos(angleRadians);
                        double y = distance_s * Math.sin(angleRadians);

                        Coordinate coordinate = new Coordinate(x, y);
                        coordinatesMap.put(busNumber, coordinate);

                        DistanceAndAngle distanceAndAngle1 = new DistanceAndAngle(coordinate, distance_s, angleRadians);
                        route.push(distanceAndAngle1);

                        System.out.println(coordinatesMap);

                    }, startAfter, 1000, TimeUnit.MILLISECONDS)
            );*/

/*            future.set(
                    scheduler.scheduleAtFixedRate(() -> {
                        double distanceInASecond = ThreadLocalRandom.current().nextDouble(lowestSpeed, highestSpeed);

                        DistanceAndAngle distanceAndAngle = route.peek();
                        DistanceAndAngle distanceAndAngleOld;
                        double distance = 0;

                        do{
                            distanceAndAngleOld = distanceAndAngle;
                            distanceAndAngle = route.poll();

                            if(distanceAndAngle == null){
                                future.get().cancel(false);
                                return;
                            }

                            distance += distanceAndAngle.getDistance();

                        }while(distanceInASecond > distance);

                        distance -= distanceAndAngle.getDistance();
                        route.push(distanceAndAngle);

                        double distance_s = distanceInASecond - distance;

                        double angleRadians = distanceAndAngleOld.getAngle();
                        double fromX = distanceAndAngleOld.getCoordinate().getX();
                        double fromY = distanceAndAngleOld.getCoordinate().getY();

                        double x = distance_s * Math.cos(angleRadians) + fromX;
                        double y = distance_s * Math.sin(angleRadians) + fromY;
                        Coordinate coordinate = new Coordinate(x, y);
                        coordinatesMap.put(busNumber, coordinate);

                        DistanceAndAngle distanceAndAngle1 = new DistanceAndAngle(coordinate, distance_s, angleRadians);
                        route.push(distanceAndAngle1);

                        System.out.println(coordinatesMap);

                    }, startAfter, 1000, TimeUnit.MILLISECONDS)
            );*/

/*            future.set(
                    scheduler.scheduleAtFixedRate(() -> {
                        double distanceInASecond = ThreadLocalRandom.current().nextDouble(lowestSpeed, highestSpeed);

                        DistanceAndAngle distanceAndAngle;
                        double distance = 0;

//                        int count = 0;

                        do{
//                            count++;
                            distanceAndAngle = route.poll();

                            if(distanceAndAngle == null){
                                Coordinate coordinate = new Coordinate(-1.0, -1.0);
                                coordinatesMap.put(busNumber, coordinate);

                                future.get().cancel(false);
                                return;
                            }

                            distance += distanceAndAngle.getDistance();

                        }while(distanceInASecond > distance);

                        double newDistance3 = distance - distanceInASecond;

                        distance -= distanceAndAngle.getDistance();
                        double distance_s = distanceInASecond - distance;

                        double angleRadians = distanceAndAngle.getAngle();
                        double distanceToNext = distanceAndAngle.getDistance();
                        double fromX = distanceAndAngle.getCoordinate().getX();
                        double fromY = distanceAndAngle.getCoordinate().getY();

                        double x = distance_s * Math.cos(angleRadians) + fromX;
                        double y = distance_s * Math.sin(angleRadians) + fromY;
                        Coordinate coordinate = new Coordinate(x, y);

                        Coordinate oldCoordinate = coordinatesMap.get(busNumber);
                        if(oldCoordinate != null){
                            if(oldCoordinate.getX() > coordinate.getX() || oldCoordinate.getY() > coordinate.getY()){
                                System.out.println(coordinatesMap.get(busNumber));
                            }
                        }

                        coordinatesMap.put(busNumber, coordinate);

                        if(oldCoordinate != null){
                            if(oldCoordinate.getX() > coordinate.getX() || oldCoordinate.getY() > coordinate.getY()){
                                System.out.println(coordinatesMap.get(busNumber));
                                System.out.println("error: " + angleRadians + ", " + distance_s);
                                System.out.println("error: " + fromX + ", " + fromY);
                                System.out.println();
                            }
                        }
//                        DistanceAndAngle daa = route.peek();
//                        double newDistance = 0;
//                        double newDistance2 = 0;
//
//                        if(daa != null){
//                            Coordinate currentCoordinate = daa.getCoordinate();
//                            if(coordinate.getX() > currentCoordinate.getX() || coordinate.getY() > currentCoordinate.getY()){
//                                newDistance = Math.sqrt(Math.pow(coordinate.getX() - currentCoordinate.getX(), 2) + Math.pow(coordinate.getY() - currentCoordinate.getY(), 2));
//
//                                System.out.println(count);
//                                System.out.println(fromX + ", " + fromY);
//                                System.out.println("new: "+coordinate);
//                                System.out.println("existing: "+currentCoordinate);
//
//                                System.out.println("new: "+distance_s);
//                                System.out.println(angleRadians);
//
//                                newDistance2 = distanceToNext - distance_s;
////                                System.out.println("distance: " + newDistance + ", " + newDistance2);
//
//                                System.out.println(coordinate);
//
//                                System.out.println();
//
//
//                            }
//                        }

                        DistanceAndAngle distanceAndAngle1 = new DistanceAndAngle(coordinate, newDistance3, angleRadians);


                        route.push(distanceAndAngle1);

                    }, startAfter, 1000, TimeUnit.MILLISECONDS)
            );*/
