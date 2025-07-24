package com.trademart.search;

import java.util.ArrayList;

import org.json.JSONObject;

import com.trademart.user.User;

public class MediaSearchItem extends SearchItem {

    private ArrayList<Integer> mediaIds;
    private User user;

    public MediaSearchItem(SearchIndexable indexable, ArrayList<Integer> mediaIds, User user) {
        super(indexable);
        this.mediaIds = mediaIds;
        this.user = user;
    }

    @Override
    public JSONObject parseJSON(){
        JSONObject json = super.parseJSON()
            .put("media_ids", mediaIds);
        json.getJSONObject("entity").put("user", user.parseJson());
        return json;
    }
    
}
