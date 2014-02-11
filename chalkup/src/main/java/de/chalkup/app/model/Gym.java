package de.chalkup.app.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.chalkup.app.persistence.BoulderNotFoundException;
import de.chalkup.app.persistence.GymManager;

public class Gym {
    private final long id;
    private final String name;
    private final List<Boulder> boulders = new ArrayList<Boulder>();

    private long nextBoulderId = 0;

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

    public synchronized void addBoulder(Boulder boulder) {
        boulder.setId(nextBoulderId++);
        boulders.add(boulder);

        GymManager.getInstance().boulderAdded(this, boulder);
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
