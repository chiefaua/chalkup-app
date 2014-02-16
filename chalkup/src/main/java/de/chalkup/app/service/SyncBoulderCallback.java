package de.chalkup.app.service;

import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;

public interface SyncBoulderCallback {
    void boulderSyncStarted();
    void boulderSynced(Boulder boulder);
    void boulderSyncFailed();
}
