package de.chalkup.app.model;

import android.content.Context;
import android.graphics.Color;

import java.io.File;
import java.net.URL;
import java.util.Collections;

public class Boulder {
    public static final long INVALID_ID = -1;
    private long id = INVALID_ID;
    private final Gym gym;
    private final BoulderLocation location;
    private Grade grade;
    private BoulderColor color;
    private URL photoUrl;

    public Boulder(Gym gym) {
        this(gym, INVALID_ID, Grade.zero(),
                new BoulderColor("DEFAULT", Collections.singletonList(Color.BLACK), ""),
                null, new BoulderLocation(0.0, 0.0));
    }

    public Boulder(Gym gym, long id, Grade grade, BoulderColor color, URL photoUrl,
                   BoulderLocation location) {
        this.gym = gym;
        this.id = id;
        this.color = color;
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

    public BoulderColor getColor() {
        return color;
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
        return new File(context.getFilesDir(), getGym().getId() + "_" + getId());
    }

    public boolean hasCachedPhoto(Context context) {
        return getCachePhotoFile(context).isFile();
    }

    public BoulderLocation getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return color.getGermanName();
    }
}
