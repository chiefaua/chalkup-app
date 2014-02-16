package de.chalkup.app.service;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.BoulderLocation;
import de.chalkup.app.model.FloorPlan;
import de.chalkup.app.model.Grade;
import de.chalkup.app.model.Gym;
import roboguice.RoboGuice;

public class LoadGymsAsyncTask extends AsyncTask<Void, Void, List<Gym>> {
    private static final String TAG = LoadGymsAsyncTask.class.getName();

    @Inject
    private Application application;
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

    private List<Gym> parseGyms(JSONArray jsonGyms) throws JSONException, IOException {
        List<Gym> gyms = new ArrayList<Gym>();
        for (int i = 0; i < jsonGyms.length(); i++) {
            JSONObject jsonGym = jsonGyms.getJSONObject(i);
            long id = jsonGym.getLong("id");
            String name = jsonGym.getString("name");

            List<FloorPlan> floorPlans = loadFloorPlans(id, jsonGym.getJSONArray("floorPlans"));

            // TODO: handle zero/multiple floor plans
            gyms.add(new Gym(id, name, floorPlans.get(0)));
        }

        return gyms;
    }

    private List<FloorPlan> loadFloorPlans(long gymId, JSONArray jsonFloorPlans)
            throws JSONException, IOException {
        List<FloorPlan> floorPlans = new ArrayList<FloorPlan>();
        for (int i = 0; i < jsonFloorPlans.length(); i++) {
            JSONObject jsonFloorPlan = jsonFloorPlans.getJSONObject(i);
            JSONObject jsonImg = jsonFloorPlan.getJSONObject("img");

            long id = jsonFloorPlan.getLong("id");
            int width = jsonImg.getInt("widthInPx");
            int height = jsonImg.getInt("heightInPx");
            String url = jsonImg.getString("url");
            File floorPlansDir = new File(application.getCacheDir(), "floorplans");
            floorPlansDir.mkdirs();
            File floorPlanFile = new File(floorPlansDir, gymId + "_" + id);
            backendService.downloadFile(url, floorPlanFile);

            floorPlans.add(new FloorPlan(width, height, Uri.fromFile(floorPlanFile)));
        }

        return floorPlans;
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

            JSONObject jsonLocation = jsonBoulder.getJSONObject("location");
            double locationX = jsonLocation.getDouble("x");
            double locationY = jsonLocation.getDouble("y");
            BoulderLocation location = new BoulderLocation(locationX, locationY);

            boulders.add(new Boulder(gym, id, colorName + " " + id, grade, photoUrl, location));
        }

        return boulders;
    }

    private Grade parseGrade(JSONObject gradeObject) throws JSONException {
        long value = gradeObject.getLong("value");
        return new Grade(value);
    }
}
