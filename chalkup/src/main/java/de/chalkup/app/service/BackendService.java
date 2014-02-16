package de.chalkup.app.service;

import android.app.Application;
import android.net.http.AndroidHttpClient;

import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Singleton
public class BackendService {
    private static final String TAG = BackendService.class.getName();
    private static final String API_PROTOCOL = "http";
    private static final String API_DOMAIN = "demo.chalkup.de";

    private final HttpClient httpClient;

    @Inject
    public BackendService(Application application) {
        this.httpClient = AndroidHttpClient.newInstance("chalkUp REST client", application);
    }

    public String loadJSON(String path) throws IOException {
        HttpGet get = new HttpGet(getBaseUrl() + path);

        InputStream contentInputStream = null;
        try {
            HttpResponse response = httpClient.execute(get);
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

    public String getBaseUrl() {
        return API_PROTOCOL + "://" + API_DOMAIN;
    }

    public void downloadPhoto(URL photoUrl, File targetFile) throws IOException {
        HttpGet get = new HttpGet(urlToUri(photoUrl));

        InputStream contentInputStream = null;
        OutputStream targetOutputStream = null;
        try {
            HttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            contentInputStream = entity.getContent();

            targetOutputStream = new FileOutputStream(targetFile);

            IOUtils.copy(contentInputStream, targetOutputStream);
        } finally {
            IOUtils.closeQuietly(contentInputStream);
            IOUtils.closeQuietly(targetOutputStream);
        }
    }

    public URL uploadPhoto(File sourceFile, String path, String contentType) throws IOException {
        HttpPut put = new HttpPut(getBaseUrl() + path);

        HttpEntity entity = new FileEntity(sourceFile, contentType);
        put.setEntity(entity);

        HttpResponse response = httpClient.execute(put);

        Header locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
        if (locationHeader != null) {
            String photoUrl = locationHeader.getValue();
            if (!photoUrl.startsWith("http")) {
                photoUrl = getBaseUrl() + photoUrl;
            }
            return new URL(photoUrl);
        } else {
            return new URL(getBaseUrl() + path);
        }
    }

    private URI urlToUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to convert URL to URI", e);
        }
    }
}
