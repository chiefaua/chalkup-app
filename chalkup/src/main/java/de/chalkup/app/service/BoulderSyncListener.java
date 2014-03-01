package de.chalkup.app.service;

import de.chalkup.app.model.Boulder;

public interface BoulderSyncListener {
    void boulderSyncStarted(Boulder boulder);

    void boulderSynced(Boulder boulder);

    void boulderSyncFailed(Boulder boulder);
}
