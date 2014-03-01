package de.chalkup.app.service;

import android.app.Application;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.util.Log;

import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Singleton
public class BackendService {
    private static final String TAG = BackendService.class.getName();
    private static final String API_PROTOCOL = "http";
    private static final String API_DOMAIN = "demo.chalkup.de";
    private static final String API_PATH = "/rest/v1";

    @Inject
    public BackendService(Application application) {
        try {
            File httpCacheDir = new File(application.getCacheDir(), "http");
            long httpCacheSize = 20 * 1024 * 1024; // 20 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i(TAG, "HTTP response cache installation failed:" + e);
        }

        // Work around pre-Froyo bugs in HTTP connection reuse.
        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    public String loadJSON(String path) throws IOException {
        return loadJSON(path, null);
    }

    public String loadJSON(String path, Integer timeoutMillis) throws IOException {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) getApiUrl(path).openConnection();

            if (timeoutMillis != null) {
                connection.setConnectTimeout(timeoutMillis);
                connection.setReadTimeout(timeoutMillis);
            }

            String contentEncoding = connection.getContentEncoding();
            if (contentEncoding != null) {
                return IOUtils.toString(connection.getInputStream(), contentEncoding);
            } else {
                return IOUtils.toString(connection.getInputStream());
            }
        } finally {
            quietCloseConnection(connection);
        }
    }

    public String getBaseUrl() {
        return API_PROTOCOL + "://" + API_DOMAIN;
    }

    public String getApiBaseUrl() {
        return getBaseUrl() + API_PATH;
    }

    public void downloadFile(String path, File targetFile) throws IOException {
        downloadFile(path, targetFile, null);
    }

    public void downloadFile(String path, File targetFile, Integer timeoutMillis) throws IOException {
        downloadFile(getAbsoluteUrl(path), targetFile, timeoutMillis);
    }

    public void downloadFile(URL fileUrl, File targetFile) throws IOException {
        downloadFile(fileUrl, targetFile, null);
    }

    public void downloadFile(URL fileUrl, File targetFile, Integer timeoutMillis) throws IOException {
        HttpURLConnection connection = null;
        OutputStream targetOutputStream = null;
        try {
            connection = (HttpURLConnection) fileUrl.openConnection();

            if (timeoutMillis != null) {
                connection.setConnectTimeout(timeoutMillis);
                connection.setReadTimeout(timeoutMillis);
            }

            if (targetFile.exists()) {
                connection.setIfModifiedSince(targetFile.lastModified());
            }

            // get input stream first so it throws an exception if any. This avoids truncating
            // the output file during creation of the FileOutputStream
            InputStream is = connection.getInputStream();
            targetOutputStream = new FileOutputStream(targetFile);
            IOUtils.copy(connection.getInputStream(), targetOutputStream);

            IOUtils.closeQuietly(targetOutputStream);
            targetFile.setLastModified(connection.getLastModified());
        } finally {
            quietCloseConnection(connection);
            IOUtils.closeQuietly(targetOutputStream);
        }
    }

    public long getFileLastModified(URL url) throws IOException {
        return getFileLastModified(url, null);
    }

    public long getFileLastModified(URL url, Integer timeoutMillis) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");

            if (timeoutMillis != null) {
                connection.setConnectTimeout(timeoutMillis);
                connection.setReadTimeout(timeoutMillis);
            }

            connection.getHeaderFields();
            return connection.getLastModified();
        } finally {
            quietCloseConnection(connection);
        }
    }

    public URL uploadFile(File sourceFile, String path, String contentType) throws IOException {
        return uploadFile(sourceFile, getAbsoluteUrl(path), contentType);
    }

    public URL uploadFile(File sourceFile, URL url, String contentType) throws IOException {
        HttpURLConnection connection = null;
        InputStream sourceInputStream = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, contentType);
            connection.setFixedLengthStreamingMode((int) sourceFile.length());

            sourceInputStream = new FileInputStream(sourceFile);

            IOUtils.copy(sourceInputStream, connection.getOutputStream());

            if (connection.getDate() > 0) {
                sourceFile.setLastModified(connection.getDate());
            }

            String location = connection.getHeaderField(HttpHeaders.LOCATION);
            if (location != null) {
                return getAbsoluteUrl(location);
            } else {
                return url;
            }
        } finally {
            quietCloseConnection(connection);
            IOUtils.closeQuietly(sourceInputStream);
        }
    }

    private void quietCloseConnection(HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }

    public URL getApiUrl(String path) {
        try {
            return new URL(getApiBaseUrl() + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to construct URL", e);
        }
    }

    public URL getAbsoluteUrl(String url) {
        try {
            if (url.startsWith("http")) {
                return new URL(url);
            } else {
                return new URL(getBaseUrl() + url);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to construct URL", e);
        }
    }
}
