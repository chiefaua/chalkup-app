package de.chalkup.app.service;

import java.util.List;

import de.chalkup.app.model.Gym;

public interface LoadGymsCallback {
    void gymsLoaded(List<Gym> gyms);
    void gymsLoadingFailed();
}
