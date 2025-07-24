package com.trademart.search;

import org.json.JSONObject;

public class SearchItem {

    private double relPoints;
    private SearchIndexable indexable;

    public SearchItem(SearchIndexable indexable) {
        this.indexable = indexable;
        relPoints = 0;
    }

    public void setRelPoints(int pts){
        relPoints = pts;
    }

    public void addRelPoints(double pts){
        relPoints += pts;
    }

    public void addRelPoint(){
        relPoints++;
    }

    public int getId() {
        return indexable.getIndexId();
    }

    public String getTerm() {
        return indexable.getKeyTerm();
    }

    public double getRelPoints() {
        return relPoints;
    }

    public JSONObject parseJSON(){
        return indexable.getIndexJson(relPoints);
    }

}
