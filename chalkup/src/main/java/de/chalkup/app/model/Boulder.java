package de.chalkup.app.model;

import android.content.Context;

import java.io.File;
import java.net.URL;

public class Boulder {
    public static final long INVALID_ID = -1;
    private long id = INVALID_ID;
    public static final long NEVER_SYNCED = -1;
    private long lastSynced = NEVER_SYNCED;
    private final Gym gym;
    private final String name;
    private Grade grade;
    private URL photoUrl;
    private final BoulderLocation location;

    public Boulder(Gym gym, String name) {
        this(gym, INVALID_ID, name, Grade.zero(), null, new BoulderLocation(0.0, 0.0));
    }

    public Boulder(Gym gym, long id, String name, Grade grade, URL photoUrl,
                   BoulderLocation location) {
        this.gym = gym;
        this.id = id;
        this.name = name;
        this.grade = grade;
        this.photoUrl = photoUrl;
        this.location = location;
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

    public URL getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(URL photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean hasPhoto() {
        return photoUrl != null;
    }

    public File getCachePhotoFile(Context context) {
        return new File(context.getCacheDir(), getGym().getId() + "_" + getId());
    }

    public boolean hasCachedPhoto(Context context) {
        return getCachePhotoFile(context).isFile();
    }

    public BoulderLocation getLocation() {
        return location;
    }

    public long getLastSyncedTimestamp() {
        return lastSynced;
    }

    public void setLastSyncedTimestamp(long lastSynced) {
        this.lastSynced = lastSynced;
    }

    @Override
    public String toString() {
        return name;
    }
}
