package de.chalkup.app.model;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.chalkup.app.service.BoulderNotFoundException;

public class Gym {
    private final long id;
    private final String name;
    private final FloorPlan floorPlan;
    private final List<Boulder> boulders = new ArrayList<Boulder>();

    private long nextBoulderId = 0;

    public Gym(long id, String name, FloorPlan floorPlan) {
        this.id = id;
        this.name = name;
        this.floorPlan = floorPlan;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public FloorPlan getFloorPlan() {
        return floorPlan;
    }

    public List<Boulder> getBoulders() {
        return Collections.unmodifiableList(this.boulders);
    }

    public void addBoulder(Boulder boulder) {
        if (!boulder.isPersisted()) {
            boulder.setId(nextBoulderId++);
        }
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
