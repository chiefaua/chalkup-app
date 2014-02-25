package de.chalkup.app.service;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.widget.Toast;

import com.google.common.collect.Lists;
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

    public void syncGyms(final Context context, final GymSyncMode gymSyncMode,
                         final BoulderSyncMode boulderSyncMode, final GymSyncCallback callback) {
        callback.syncStarted();
        new LoadGymsAsyncTask(context, gymSyncMode, new LoadGymsCallback() {
            @Override
            public void gymsLoaded(List<Gym> gyms) {
                setGyms(gyms);

                if (boulderSyncMode != BoulderSyncMode.NO_SYNC) {
                    syncBoulders();
                } else {
                    gymsLoadingFinished();
                }
            }

            @Override
            public void gymsLoadingFailed() {
                Toast.makeText(context, R.string.sync_gyms_failed, Toast.LENGTH_SHORT).show();

                if (gymSyncMode == GymSyncMode.FORCE_SYNC) {
                    // remove existing gyms if sync failed
                    setGyms(Collections.<Gym>emptyList());
                }

                gymsLoadingFinished();
            }

            private void syncBoulders() {
                List<Boulder> allBoulders = Lists.newArrayList();
                for (Gym gym : getGyms()) {
                    allBoulders.addAll(gym.getBoulders());
                }

                new SyncBoulderAsyncTask(context, boulderSyncMode, new SyncBoulderCallback() {
                    @Override
                    public void boulderSyncStarted() {}

                    @Override
                    public void boulderSynced(List<Boulder> boulder) {
                        gymsLoadingFinished();
                    }

                    @Override
                    public void boulderSyncFailed() {
                        Toast.makeText(context, R.string.sync_all_boulders_failed,
                                Toast.LENGTH_SHORT).show();
                        gymsLoadingFinished();
                    }
                }).execute(allBoulders.toArray(new Boulder[0]));
            }

            private void gymsLoadingFinished() {
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
}
