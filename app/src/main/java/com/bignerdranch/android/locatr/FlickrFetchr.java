package com.bignerdranch.android.locatr;

import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
    private static final String TAG = "FLICKR_FETCHR";
    private static final String API_KEY = "5a454ebf642459e3ee877e0d601f7c34";
    private static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s,geo")
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            in.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos(int page) {
        String url = buildUrl(page);
        Log.i(TAG, "Fetch recent photos[" + page + "]: " + url);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query, int page) {
        String url = buildUrl(query, page);
        Log.i(TAG, "Search photos[" + query + "]: " + url);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(Location location, int page) {
        String url = buildUrl(location, page);
        return downloadGalleryItems(url);
    }

    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();
        try {
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            parseItems(items, jsonString);
        } catch(IOException e) {
            Log.e(TAG, "Failed to fetch items.", e);
        }
        return items;
    }

    private String buildUrl(String query, int page) {
        return ENDPOINT.buildUpon()
                .appendQueryParameter("method", SEARCH_METHOD)
                .appendQueryParameter("page", "" + page)
                .appendQueryParameter("text", query)
                .build().toString();
    }

    private String buildUrl(int page) {
        return ENDPOINT.buildUpon()
                .appendQueryParameter("method", FETCH_RECENT_METHOD)
                .appendQueryParameter("page", "" + page)
                .build().toString();
    }

    private String buildUrl(Location location, int page) {
        return ENDPOINT.buildUpon()
                .appendQueryParameter("method", SEARCH_METHOD)
                .appendQueryParameter("lat", "" + location.getLatitude())
                .appendQueryParameter("lon", "" + location.getLongitude())
                .build().toString();
    }

    private void parseItems(List<GalleryItem> items, String body) {
        Gson gson = new Gson();
        Flikr flikr = gson.fromJson(body, Flikr.class);
        for (int i = 0 ; i < flikr.photos.photo.size() ; i++) {
            Photo photo = flikr.photos.photo.get(i);
            if (photo.url_s == "") {
                continue;
            }
            GalleryItem item = new GalleryItem();
            item.setId(photo.id);
            item.setCaption(photo.title);
            item.setUrl(photo.url_s);
            item.setOwner(photo.owner);
            item.setLat(photo.latitude);
            item.setLon(photo.longitude);
            items.add(item);
        }
    }

    private class Flikr {
        public Photos photos;
    }

    private class Photos {
        public List<Photo> photo;
        public int page;
    }

    private class Photo {
        public String title;
        public String id;
        public String url_s;
        public String owner;
        public double latitude;
        public double longitude;
    }
}
