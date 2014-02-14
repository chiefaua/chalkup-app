package de.chalkup.app.service;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;

@Singleton
public class GymService {
    private final DataSetObservable gymsObservable = new DataSetObservable();
    private final Map<Long, DataSetObservable> gymObservables =
            new HashMap<Long, DataSetObservable>();
    private List<Gym> gyms = new ArrayList<Gym>();

    GymService() {
    }

    public void syncGyms() {
        new LoadGymsAsyncTask(this).execute(null);
    }

    public List<Gym> getGyms() {
        return Collections.unmodifiableList(gyms);
    }

    public void setGyms(List<Gym> gyms) {
        this.gyms = gyms;

        gymObservables.clear();
        for (Gym gym : gyms) {
            gymObservables.put(gym.getId(), new DataSetObservable());
        }

        gymsObservable.notifyChanged();
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

        gymObservables.get(gym.getId()).notifyChanged();
    }

    public void registerGymsObserver(DataSetObserver observer) {
        gymsObservable.registerObserver(observer);
    }

    public void unregisterGymsObserver(DataSetObserver observer) {
        gymsObservable.unregisterObserver(observer);
    }

    public void registerGymObserver(Gym gym, DataSetObserver observer) {
        gymObservables.get(gym.getId()).registerObserver(observer);
    }

    public void unregisterGymObserver(Gym gym, DataSetObserver observer) {
        gymObservables.get(gym.getId()).unregisterObserver(observer);
    }
}
