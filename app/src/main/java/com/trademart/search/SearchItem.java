package com.trademart.search;

public class SearchItem {

    private int id;
    private String term;
    private double relPoints;

    public SearchItem(int id, String term) {
        this.id = id;
        this.term = term;
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
        return id;
    }

    public String getTerm() {
        return term;
    }

    public double getRelPoints() {
        return relPoints;
    }

}
