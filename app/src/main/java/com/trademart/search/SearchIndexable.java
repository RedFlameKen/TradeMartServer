package com.trademart.search;

import org.json.JSONObject;

public interface SearchIndexable {

   public int getIndexId();
   public String getKeyTerm();
   public JSONObject getIndexJson(double relPoints);
    
}
