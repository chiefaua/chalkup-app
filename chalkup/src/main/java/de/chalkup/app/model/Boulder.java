package de.chalkup.app.model;

import android.content.Context;
import android.net.Uri;

public class Boulder {
    public static final long INVALID_ID = -1;
    private long id = INVALID_ID;
    private final Gym gym;
    private final String name;
    private Grade grade;

    public Boulder(Gym gym, String name) {
        this(gym, INVALID_ID, name, Grade.zero());
    }

    public Boulder(Gym gym, long id, String name, Grade grade) {
        this.gym = gym;
        this.id = id;
        this.name = name;
        this.grade = grade;
    }

    public boolean isPersisted() {
        return id != INVALID_ID;
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

    public Grade getGrade() {
        return grade;
    }

    public Uri getPhotoUri(Context context) {
        return Uri.fromFile(context.getFileStreamPath(getGym().getId() + "_" + getId()));
    }

    @Override
    public String toString() {
        return name;
    }
}
