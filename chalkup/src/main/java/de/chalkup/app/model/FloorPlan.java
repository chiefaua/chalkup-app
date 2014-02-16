package de.chalkup.app.model;

import android.net.Uri;

public class FloorPlan {
    private final int width;
    private final int height;
    private final Uri uri;

    public FloorPlan(int width, int height, Uri uri) {
        this.width = width;
        this.height = height;
        this.uri = uri;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Uri getUri() {
        return uri;
    }
}
