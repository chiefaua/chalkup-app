package de.chalkup.app.model;

import android.content.Context;
import android.net.Uri;

import java.io.File;

public class Boulder {
    public static final long INVALID_ID = -1;

    private final Gym gym;
    private final String name;
    private long id = INVALID_ID;

    public Boulder(Gym gym, String name) {
        this.gym = gym;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        if (this.id != INVALID_ID) {
            throw new IllegalStateException("Boulder had already a valid ID!");
        }
        this.id = id;
    }

    public Gym getGym() {
        return gym;
    }

    public String getName() {
        return name;
    }

    public Uri getPhotoUri(Context context) {
        return Uri.fromFile(context.getFileStreamPath(getGym().getId() + "_" + getId()));
    }

    @Override
    public String toString() {
        return name;
    }
}
