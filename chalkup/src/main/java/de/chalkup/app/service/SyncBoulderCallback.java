package de.chalkup.app.service;

import java.util.List;

import de.chalkup.app.model.Boulder;

public interface SyncBoulderCallback {
    void boulderSyncStarted();

    void boulderSynced(List<Boulder> boulder);

    void boulderSyncFailed();
}
