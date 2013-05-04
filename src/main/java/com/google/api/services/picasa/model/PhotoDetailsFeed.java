package com.google.api.services.picasa.model;

import com.google.api.client.util.Key;

/**
 * @author Eirik Sand
 */
public class PhotoDetailsFeed extends Feed {
    @Key("gphoto:id")
    public String id;

    @Key("gphoto:albumid")
    public String albumId;

    @Key
    public String title;

    @Key("subtitle")
    public String summary;

    @Key
    public String icon;

    @Key
    public String updated;

    @Key
    public Category category = Category.newKind("photo");

    @Key("media:group")
    public MediaGroup mediaGroup;
}