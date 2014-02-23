package de.chalkup.app.model;

public class BoulderColor {
    private final int color;
    private final String germanName;

    public BoulderColor(int color, String germanName) {
        this.color = color;
        this.germanName = germanName;
    }

    public int getColor() {
        return color;
    }

    public String getGermanName() {
        return germanName;
    }
}
