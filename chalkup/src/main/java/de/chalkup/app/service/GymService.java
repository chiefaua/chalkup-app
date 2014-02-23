package de.chalkup.app.service;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.widget.Toast;

import com.google.common.collect.Sets;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.chalkup.app.R;
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

    public void syncGyms(final Context context, final SyncMode syncMode,
                         final GymSyncCallback callback) {
        callback.syncStarted();
        new LoadGymsAsyncTask(context, syncMode, new LoadGymsCallback() {
            @Override
            public void gymsLoaded(List<Gym> gyms) {
                setGyms(gyms);
                callback.syncFinished();
            }

            @Override
            public void gymsLoadingFailed() {
                Toast.makeText(context, R.string.gym_sync_failed, Toast.LENGTH_SHORT).show();

                if (syncMode == SyncMode.FORCE_SYNC) {
                    // remove existing gyms if sync failed
                    setGyms(Collections.<Gym>emptyList());
                }

                callback.syncFinished();
            }
        }).execute();
    }

    public List<Gym> getGyms() {
        return Collections.unmodifiableList(gyms);
    }

    private void setGyms(List<Gym> gyms) {
        this.gyms = gyms;

        Set<Long> newIdSet = new HashSet<Long>();
        Set<Long> oldIdSet = gymObservables.keySet();

        for (Gym gym : gyms) {
            newIdSet.add(gym.getId());
        }

        for (Long gymId : Sets.difference(newIdSet, oldIdSet)) {
            gymObservables.put(gymId, new DataSetObservable());
        }
        for (Long gymId : Sets.difference(oldIdSet, newIdSet)) {
            gymObservables.remove(gymId);
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

    public void boulderChanged(Boulder boulder) {
        gymObservables.get(boulder.getGym().getId()).notifyChanged();
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

    public enum SyncMode {
        FAST_FROM_CACHE,
        ALLOW_CACHE,
        FORCE_SYNC
    }
}
