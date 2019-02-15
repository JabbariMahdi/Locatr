package com.maktab.mahdi.locatr;

import android.location.Location;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {

    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "79b5c28546b0c0fd5a0bdc65ac9eab18";
    private static final String METHOD_RECENT = "flickr.photos.getPopular";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
           // .appendQueryParameter("user_id", "34427466731@N01")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("extras", "url_s,geo")
            .appendQueryParameter("nojsoncallback", "1")
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + " with: " + urlSpec);
            }

            byte[] buffer = new byte[1024];
            int readSize = 0;
            while ((readSize = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, readSize);
            }

            return outputStream.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    /**
     * must be run on background thread
     * 1. create url for flicker
     * 2. get response string from flicker
     * 3. parse response string in json
     * 4. convert json to list of GalleryItems
     * 5. return GalleryItems
     */
    public List<GalleryItem> downloadGalleryItem(String url) {
        List<GalleryItem> items = new ArrayList<>();
        try {
            String jsonResponse = getUrlString(url);
            Log.d(TAG, "json received: " + jsonResponse);
            JSONObject jsonBody = new JSONObject(jsonResponse);
            parseItem(jsonBody, items);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch", e);
        } catch (JSONException e) {
            Log.e(TAG, "Cannot parse json", e);
        }

        return items;
    }

    public List<GalleryItem> getRecentPhotos() {
        String url = buildUrl(METHOD_RECENT, null);
        return downloadGalleryItem(url);
    }

    public List<GalleryItem> getSearchPhotos(String query) {
        String url = buildUrl(METHOD_SEARCH, query);
        return downloadGalleryItem(url);
    }


    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter("method", method);
        if (method.equals(METHOD_SEARCH))
            uriBuilder.appendQueryParameter("text", query);

        return uriBuilder.build().toString();
    }

    private String bindUrl(Location location){
        return ENDPOINT.buildUpon().appendQueryParameter("method",METHOD_SEARCH)
                .appendQueryParameter("lat",location.getLatitude()+"")
                .appendQueryParameter("lon",location.getLongitude()+"")
                .build().toString();
    }

    public List<GalleryItem> searchPhoto(Location location){
        String url=bindUrl(location);
        return downloadGalleryItem(url);
    }


    public void parseItem(JSONObject jsonBody, List<GalleryItem> items) throws JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            if (!photoJsonObject.has("url_s"))
                continue;

            GalleryItem galleryItem = new GalleryItem();
            galleryItem.setId(photoJsonObject.getString("id"));
            galleryItem.setCaption(photoJsonObject.getString("title"));
            galleryItem.setUrl(photoJsonObject.getString("url_s"));
            galleryItem.setOwner(photoJsonObject.getString("owner"));
            galleryItem.setmLat(photoJsonObject.getDouble("latitude"));
            galleryItem.setmLon(photoJsonObject.getDouble("longitude"));



            items.add(galleryItem);
        }
    }

}
