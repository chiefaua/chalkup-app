package de.chalkup.app.service;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.BoulderColor;
import de.chalkup.app.model.BoulderLocation;
import de.chalkup.app.model.FloorPlan;
import de.chalkup.app.model.Grade;
import de.chalkup.app.model.Gym;
import roboguice.RoboGuice;

public class LoadGymsAsyncTask extends AsyncTask<Void, Void, List<Gym>> {
    public static final int FAST_TIMEOUT = 2000;
    public static final Pattern COLOR_PATTERN =
            Pattern.compile("rgb\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)");
    private static final String TAG = LoadGymsAsyncTask.class.getName();
    @Inject
    private Application application;
    @Inject
    private BackendService backendService;

    private GymSyncMode gymSyncMode;
    private LoadGymsCallback callback;

    public LoadGymsAsyncTask(Context context, GymSyncMode gymSyncMode,
                             LoadGymsCallback callback) {
        this.gymSyncMode = gymSyncMode;
        this.callback = callback;
        RoboGuice.getInjector(context).injectMembers(this);
    }

    @Override
    protected List<Gym> doInBackground(Void... params) {
        try {
            String json;
            try {
                json = backendService.loadJSON("/gyms", getTimeoutMillis());
                FileUtils.writeStringToFile(getGymCacheFile(), json, Charsets.UTF_8.name());
            } catch (IOException e) {
                rethrowIfNeededBySyncMode(e);
                try {
                    json = FileUtils.readFileToString(getGymCacheFile(), Charsets.UTF_8.name());
                } catch (Exception inner) {
                    Log.e(TAG, "Failed to load gyms from cache", inner);
                    throw e;
                }
            }

            return loadGyms(json);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load gyms", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<Gym> gyms) {
        if (gyms != null) {
            callback.gymsLoaded(gyms);
        } else {
            callback.gymsLoadingFailed();
        }
    }

    private List<Gym> loadGyms(String json) {
        try {
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

            List<BoulderColor> colors = Lists.newArrayList();
            JSONArray jsonColors = jsonGym.getJSONArray("colors");
            for (int j = 0; j < jsonColors.length(); j++) {
                colors.add(parseBoulderColor(jsonColors.getJSONObject(j)));
            }

            // TODO: handle zero/multiple floor plans
            gyms.add(new Gym(id, name, floorPlans.get(0), colors));
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
            URL url = backendService.getAbsoluteUrl(jsonImg.getString("url"));
            File floorPlansDir = new File(application.getCacheDir(), "floorplans");
            floorPlansDir.mkdirs();
            File floorPlanFile = new File(floorPlansDir, gymId + "_" + id);
            downloadFloorPlan(url, floorPlanFile);

            floorPlans.add(new FloorPlan(width, height, Uri.fromFile(floorPlanFile)));
        }

        return floorPlans;
    }

    private void downloadFloorPlan(URL url, File floorPlanFile) throws IOException {
        try {
            backendService.downloadFile(url, floorPlanFile, getTimeoutMillis());
        } catch (IOException e) {
            rethrowIfNeededBySyncMode(e);

            if (!floorPlanFile.isFile()) {
                throw e;
            }

            // otherwise: use cached file, it's already where it should be
        }
    }

    private void loadBoulders(Gym gym) throws IOException, JSONException {
        String json;
        try {
            json = backendService.loadJSON("/gyms/" + gym.getId() + "/boulders");

            FileUtils.writeStringToFile(getBoulderCacheFile(gym), json, Charsets.UTF_8.name());
        } catch (IOException e) {
            rethrowIfNeededBySyncMode(e);
            try {
                json = FileUtils.readFileToString(getBoulderCacheFile(gym), Charsets.UTF_8.name());
            } catch (Exception inner) {
                Log.e(TAG, "Failed to load boulders from cache", inner);
                throw e;
            }
        }

        List<Boulder> boulders = parseBoulders(gym, new JSONArray(json));
        Collections.sort(boulders, new Comparator<Boulder>() {
            @Override
            public int compare(Boulder lhs, Boulder rhs) {
                return lhs.getColor().getGermanName().compareTo(rhs.getColor().getGermanName()) * 2 +
                        Long.valueOf(lhs.getId()).compareTo(Long.valueOf(rhs.getId()));
            }
        });
        for (Boulder boulder : boulders) {
            gym.addBoulder(boulder);
        }
    }

    private List<Boulder> parseBoulders(Gym gym, JSONArray jsonBoulders) throws JSONException,
            IOException {
        List<Boulder> boulders = new ArrayList<Boulder>();
        for (int i = 0; i < jsonBoulders.length(); i++) {
            JSONObject jsonBoulder = jsonBoulders.getJSONObject(i);
            long id = jsonBoulder.getLong("id");
            BoulderColor boulderColor = parseBoulderColor(jsonBoulder.getJSONObject("color"));
            Grade grade = parseGrade(jsonBoulder.getJSONObject("grade").getJSONObject("mean"));
            URL photoUrl = null;
            if (jsonBoulder.has("photo")) {
                String url = jsonBoulder.getJSONObject("photo").getString("url");
                photoUrl = backendService.getAbsoluteUrl(url);
            }

            JSONObject jsonLocation = jsonBoulder.getJSONObject("location");
            double locationX = jsonLocation.getDouble("x");
            double locationY = jsonLocation.getDouble("y");
            BoulderLocation location = new BoulderLocation(locationX, locationY);

            boulders.add(new Boulder(gym, id, grade, boulderColor, photoUrl, location));
        }

        return boulders;
    }

    private BoulderColor parseBoulderColor(JSONObject jsonColor) throws JSONException, IOException {
        String name = jsonColor.getString("name");
        String colorName = jsonColor.getString("germanName");
        List<Integer> colors = Lists.newArrayList();
        for (String s : Arrays.asList("primary", "secondary", "ternary")) {
            if (jsonColor.has(s)) {
                colors.add(parseColor(jsonColor.getString(s)));
            }
        }

        return new BoulderColor(name, colors, colorName);
    }

    private int parseColor(String primary) throws IOException {
        Matcher matcher = COLOR_PATTERN.matcher(primary);
        if (matcher.matches()) {
            int r = Integer.parseInt(matcher.group(1));
            int g = Integer.parseInt(matcher.group(2));
            int b = Integer.parseInt(matcher.group(3));

            return Color.argb(255, r, g, b);
        } else {
            throw new IOException("Failed to parse color: " + primary);
        }
    }

    private Grade parseGrade(JSONObject gradeObject) throws JSONException {
        long value = gradeObject.getLong("value");
        return new Grade(value);
    }

    private void rethrowIfNeededBySyncMode(IOException e) throws IOException {
        if (gymSyncMode == GymSyncMode.FORCE_SYNC) {
            throw e;
        } else if (e instanceof SocketTimeoutException) {
            // OK
            return;
        } else if (e instanceof UnknownHostException) {
            // OK
            return;
        } else if (e instanceof FileNotFoundException) {
            // OK...?
            return;
        }

        throw e;
    }

    private File getGymCacheFile() {
        return new File(application.getCacheDir(), "gyms_cache.json");
    }

    private File getBoulderCacheFile(Gym gym) {
        return new File(application.getCacheDir(), "gym_" + gym.getId() + "_boulders_cache.json");
    }

    private Integer getTimeoutMillis() {
        if (gymSyncMode == GymSyncMode.FAST_FROM_CACHE) {
            return FAST_TIMEOUT;
        }
        return null;
    }
}
