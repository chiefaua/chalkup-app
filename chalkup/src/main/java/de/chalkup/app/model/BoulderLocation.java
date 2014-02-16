package de.chalkup.app.model;

public class BoulderLocation {
    private final double x;
    private final double y;

    public BoulderLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
