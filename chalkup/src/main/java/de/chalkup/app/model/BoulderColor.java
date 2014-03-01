package de.chalkup.app.model;

import java.util.List;

public class BoulderColor {
    private final String name;
    private final List<Integer> colors;
    private final String germanName;

    public BoulderColor(String name, List<Integer> colors, String germanName) {
        this.name = name;
        this.colors = colors;
        this.germanName = germanName;
    }

    public String getName() {
        return name;
    }

    public List<Integer> getColors() {
        return colors;
    }

    public String getGermanName() {
        return germanName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoulderColor that = (BoulderColor) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
