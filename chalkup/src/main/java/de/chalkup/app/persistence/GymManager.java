package de.chalkup.app.persistence;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

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
                Boulder b = new Boulder(gym,
                        "Boulder " + i + " in " + gym.getName());
                gym.addBoulder(b);
            }
        }
    }

    private final DataSetObservable dataSetObservable = new DataSetObservable();

    private GymManager() {
    }

    public static GymManager getInstance() {
        return instance;
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

    public void registerDataSetObserver(DataSetObserver observer) {
        dataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        dataSetObservable.unregisterObserver(observer);
    }

    public void boulderAdded(Gym gym, Boulder boulder) {
        dataSetObservable.notifyChanged();
    }
}
