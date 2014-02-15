package de.chalkup.app.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Grade;
import de.chalkup.app.model.Gym;

public class LoadGymsAsyncTask extends AsyncTask<LoadGymsCallback, Void, List<Gym>> {
    private static final String TAG = LoadGymsAsyncTask.class.getName();

    private static final String API_PROTOCOL = "http";
    private static final String API_DOMAIN = "demo.chalkup.de";
    private final HttpClient httpClient;
    private LoadGymsCallback callback;

    public LoadGymsAsyncTask() {
        this.httpClient = new DefaultHttpClient(new BasicHttpParams());
    }

    @Override
    protected List<Gym> doInBackground(LoadGymsCallback... params) {
        callback = params[0];
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
            String json = loadJSON("gyms");
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
        String json = loadJSON("gyms/" + gym.getId() + "/boulders");
        List<Boulder> boulders = parseBoulders(gym, new JSONArray(json));
        for (Boulder boulder : boulders) {
            gym.addBoulder(boulder);
        }
    }

    private List<Boulder> parseBoulders(Gym gym, JSONArray jsonBoulders) throws JSONException {
        List<Boulder> boulders = new ArrayList<Boulder>();
        for (int i = 0; i < jsonBoulders.length(); i++) {
            JSONObject jsonBoulder = jsonBoulders.getJSONObject(i);
            long id = jsonBoulder.getLong("id");
            String colorName = jsonBoulder.getJSONObject("color").getString("germanName");
            Grade grade = parseGrade(jsonBoulder.getJSONObject("grade").getJSONObject("mean"));
            boulders.add(new Boulder(gym, id, colorName, grade));
        }

        return boulders;
    }

    private Grade parseGrade(JSONObject gradeObject) throws JSONException {
        long value = gradeObject.getLong("value");
        return new Grade(value);
    }

    private String loadJSON(String path) throws IOException {
        HttpGet getGyms = new HttpGet(getBaseUrl() + path);

        InputStream contentInputStream = null;
        try {
            HttpResponse response = httpClient.execute(getGyms);
            HttpEntity entity = response.getEntity();

            Header contentEncoding = entity.getContentEncoding();
            contentInputStream = entity.getContent();

            if (contentEncoding != null) {
                return IOUtils.toString(contentInputStream, contentEncoding.getValue());
            } else {
                return IOUtils.toString(contentInputStream);
            }
        } finally {
            IOUtils.closeQuietly(contentInputStream);
        }
    }

    private String getBaseUrl() {
        return API_PROTOCOL + "://" + API_DOMAIN + "/";
    }
}
