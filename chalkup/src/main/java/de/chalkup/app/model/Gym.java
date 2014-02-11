package de.chalkup.app.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.chalkup.app.persistence.BoulderNotFoundException;

public class Gym {
    private final long id;
    private final String name;
    private final List<Boulder> boulders = new ArrayList<Boulder>();

    public Gym(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Boulder> getBoulders() {
        return Collections.unmodifiableList(this.boulders);
    }

    public void addBoulder(Boulder boulder) {
        boulders.add(boulder);
    }

    public Boulder getBoulder(long id) throws BoulderNotFoundException {
        for (Boulder b : getBoulders()) {
            if (b.getId() == id) {
                return b;
            }
        }

        throw new BoulderNotFoundException();
    }
}
