package org.example.multidisplaycore.service;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Coordinate {
    double x;
    double y;

    public Coordinate(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
