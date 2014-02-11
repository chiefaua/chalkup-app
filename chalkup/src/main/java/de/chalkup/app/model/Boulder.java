package de.chalkup.app.model;

import android.graphics.Bitmap;

public class Boulder {
    private final long id;
    private final Gym gym;
    private final String name;

    public Boulder(long id, Gym gym, String name) {
        this.id = id;
        this.gym = gym;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public Gym getGym() { return gym; }

    public String getName() {
        return name;
    }

    public String getPhotoFilename() {
        return getGym().getId() + "_" + getId();
    }

    @Override
    public String toString() {
        return name;
    }
}
