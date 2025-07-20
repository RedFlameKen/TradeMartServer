package com.trademart.user;

import com.trademart.feed.FeedCategory;
import static com.trademart.feed.FeedCategory.*;

import org.json.JSONObject;

public class UserPreferences {

    private int userId;
    private FeedCategory preferredCategory;

    public UserPreferences(int userId){
        preferredCategory = NONE;
    }

    public void setPreferredCategory(FeedCategory preferredCategory) {
        this.preferredCategory = preferredCategory;
    }

    public int getUserId() {
        return userId;
    }

    public FeedCategory getPreferredCategory() {
        return preferredCategory;
    }
    
    public JSONObject parseJson(){
        return new JSONObject()
            .put("user_id", userId)
            .put("preferred_category", preferredCategory.toString());
    }

}
