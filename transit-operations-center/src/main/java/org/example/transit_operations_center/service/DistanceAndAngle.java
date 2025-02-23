package org.example.transit_operations_center.service;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DistanceAndAngle {
    private final Coordinate coordinate;
    private double distance;
    private double angle;

    public DistanceAndAngle(Coordinate coordinate, double distance, double angle) {
        this.coordinate = coordinate;
        this.distance = distance;
        this.angle = angle;
    }

    public DistanceAndAngle(Coordinate coordinate) {
        this.coordinate = coordinate;
    }
}
