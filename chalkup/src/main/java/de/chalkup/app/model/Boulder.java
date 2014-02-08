package de.chalkup.app.model;

public class Boulder {
    public String id;
    public String content;

    public Boulder(String id, String content) {
        this.id = id;
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}
