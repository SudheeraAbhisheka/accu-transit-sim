package org.example.multidisplaycore.service;

import lombok.Getter;

@Getter
public class Bus {
    private final String routeId;
    private final double lowestSpeed;
    private final double highestSpeed;
    private final long startAfter;

    public Bus(String routeId, double lowestSpeed, double highestSpeed, long startAfter) {
        this.routeId = routeId;
        this.lowestSpeed = lowestSpeed;
        this.highestSpeed = highestSpeed;
        this.startAfter = startAfter;
    }
}
