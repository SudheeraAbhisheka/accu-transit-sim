package org.example.multidisplaycore.service;

import lombok.Getter;

@Getter
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
