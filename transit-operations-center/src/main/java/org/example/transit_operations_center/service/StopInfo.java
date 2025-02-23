package org.example.transit_operations_center.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class StopInfo {
    private Coordinate coordinate;
    private double distanceNextStop;

    public StopInfo(Coordinate coordinate, double distanceNextStop) {
        this.coordinate = coordinate;
        this.distanceNextStop = distanceNextStop;
    }

    public StopInfo() {
    }
}
