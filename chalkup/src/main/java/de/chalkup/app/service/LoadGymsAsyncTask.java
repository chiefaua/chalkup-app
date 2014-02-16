package de.chalkup.app.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Grade;
import de.chalkup.app.model.Gym;
import roboguice.RoboGuice;

public class LoadGymsAsyncTask extends AsyncTask<Void, Void, List<Gym>> {
    private static final String TAG = LoadGymsAsyncTask.class.getName();

    @Inject
    private BackendService backendService;

    private LoadGymsCallback callback;

    public LoadGymsAsyncTask(Context context, LoadGymsCallback callback) {
        this.callback = callback;
        RoboGuice.getInjector(context).injectMembers(this);
    }

    @Override
    protected List<Gym> doInBackground(Void... params) {
        return loadGyms();
    }

    @Override
    protected void onPostExecute(List<Gym> gyms) {
        if (gyms != null) {
            callback.gymsLoaded(gyms);
        } else {
            callback.gymsLoadingFailed();
        }
    }

    private List<Gym> loadGyms() {
        try {
            String json = backendService.loadJSON("/gyms");
            List<Gym> gyms = parseGyms(new JSONArray(json));

            for (Gym gym : gyms) {
                loadBoulders(gym);
            }

            return gyms;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load gyms", e);
            return null;
        }
    }

    private List<Gym> parseGyms(JSONArray jsonGyms) throws JSONException {
        List<Gym> gyms = new ArrayList<Gym>();
        for (int i = 0; i < jsonGyms.length(); i++) {
            JSONObject jsonGym = jsonGyms.getJSONObject(i);
            long id = jsonGym.getLong("id");
            String name = jsonGym.getString("name");
            gyms.add(new Gym(id, name));
        }

        return gyms;
    }

    private void loadBoulders(Gym gym) throws IOException, JSONException {
        String json = backendService.loadJSON("/gyms/" + gym.getId() + "/boulders");
        List<Boulder> boulders = parseBoulders(gym, new JSONArray(json));
        for (Boulder boulder : boulders) {
            gym.addBoulder(boulder);
        }
    }

    private List<Boulder> parseBoulders(Gym gym, JSONArray jsonBoulders) throws JSONException,
            MalformedURLException {
        List<Boulder> boulders = new ArrayList<Boulder>();
        for (int i = 0; i < jsonBoulders.length(); i++) {
            JSONObject jsonBoulder = jsonBoulders.getJSONObject(i);
            long id = jsonBoulder.getLong("id");
            String colorName = jsonBoulder.getJSONObject("color").getString("germanName");
            Grade grade = parseGrade(jsonBoulder.getJSONObject("grade").getJSONObject("mean"));
            URL photoUrl = null;
            if (jsonBoulder.has("photo")) {
                String url = jsonBoulder.getJSONObject("photo").getString("url");
                if (!url.startsWith("http")) {
                    url = backendService.getBaseUrl() + url;
                }
                photoUrl = new URL(url);
            }
            boulders.add(new Boulder(gym, id, colorName + " " + id, grade, photoUrl));
        }

        return boulders;
    }

    private Grade parseGrade(JSONObject gradeObject) throws JSONException {
        long value = gradeObject.getLong("value");
        return new Grade(value);
    }
}
