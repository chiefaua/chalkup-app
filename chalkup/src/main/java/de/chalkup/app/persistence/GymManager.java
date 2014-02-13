package de.chalkup.app.persistence;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;

@Singleton
public class GymManager {
    private final List<Gym> gyms = new ArrayList<Gym>();

    private final DataSetObservable dataSetObservable = new DataSetObservable();

    GymManager() {
        gyms.add(new Gym(1, "Boulderwelt"));
        gyms.add(new Gym(2, "Heavens Gate"));

        for (Gym gym : gyms) {
            for (int i = 0; i < 5; i++) {
                Boulder b = new Boulder(gym,
                        "Boulder " + i + " in " + gym.getName());
                gym.addBoulder(b);
            }
        }
    }

    public List<Gym> getGyms() {
        return Collections.unmodifiableList(gyms);
    }

    public Gym getGym(long id) throws GymNotFoundException {
        for (Gym g : getGyms()) {
            if (g.getId() == id) {
                return g;
            }
        }

        throw new GymNotFoundException();
    }

    public void addBoulderToGym(Gym gym, Boulder boulder) {
        gym.addBoulder(boulder);
        dataSetObservable.notifyChanged();
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        dataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        dataSetObservable.unregisterObserver(observer);
    }
}
