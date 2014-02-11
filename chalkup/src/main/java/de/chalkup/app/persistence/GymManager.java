package de.chalkup.app.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;

public class GymManager {
    private static GymManager instance = new GymManager();
    private static List<Gym> GYMS = new ArrayList<Gym>();

    static {
        GYMS.add(new Gym(1, "Boulderwelt"));
        GYMS.add(new Gym(2, "Heavens Gate"));

        for (Gym gym : GYMS) {
            for (int i = 0; i < 5; i++) {
                Boulder b = new Boulder(i, gym,
                        "Boulder " + i + " in " + gym.getName());
                gym.addBoulder(b);
            }
        }
    }

    private GymManager() {
    }

    public List<Gym> getGyms() {
        return Collections.unmodifiableList(GYMS);
    }

    public Gym getGym(long id) throws GymNotFoundException {
        for (Gym g : getGyms()) {
            if (g.getId() == id) {
                return g;
            }
        }

        throw new GymNotFoundException();
    }

    public static GymManager getInstance() {
        return instance;
    }
}
