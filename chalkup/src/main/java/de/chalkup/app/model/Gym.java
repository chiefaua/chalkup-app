package de.chalkup.app.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Gym {

    public static List<Boulder> BOULDERS = new ArrayList<Boulder>();
    public static Map<String, Boulder> BOULDER_MAP = new HashMap<String, Boulder>();

    static {
        addItem(new Boulder("1", "Boulder 1"));
        addItem(new Boulder("2", "Boulder 2"));
        addItem(new Boulder("3", "Boulder 3"));
    }

    private static void addItem(Boulder boulder) {
        BOULDERS.add(boulder);
        BOULDER_MAP.put(boulder.id, boulder);
    }

}
