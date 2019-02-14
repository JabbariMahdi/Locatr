package com.maktab.mahdi.locatr;

import android.net.Uri;

public class GalleryItem {

    private String mId;
    private String mCaption;
    private String mUrl;
    private String mOwner;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        this.mCaption = caption;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        this.mOwner = owner;
    }

    public Uri getPhotoPage() {
        return Uri.parse("https://www.flickr.com/photos")
                .buildUpon()
                .appendPath(getOwner())
                .appendPath(getId())
                .build();
    }

}