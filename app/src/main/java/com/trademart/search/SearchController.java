package com.trademart.search;

import java.util.ArrayList;

import org.json.JSONObject;

public class SearchController<T extends SearchItem> {

    public ArrayList<T> filter(String searchQuery, ArrayList<T> searchItems){
        ArrayList<T> out = new ArrayList<>();
        ArrayList<String> terms = extractTerms(searchQuery);
        for (T item : searchItems) {
            for (String term : terms) {
                if(item.getTerm().toLowerCase().contains(term.toLowerCase())){
                    if(!out.contains(item))
                        out.add(item);
                    item.addRelPoint();
                }
            }
        }
        if(out.isEmpty()){
            return out;
        }
        out.sort((i1, i2) -> {
            return i1.getTerm().compareTo(i2.getTerm());

        });
        out = sortByRelevance(out);
        return out;
    }

    public ArrayList<T> sortByRelevance(ArrayList<T> searchItems){
        ArrayList<T> items = new ArrayList<>();
        items.add(searchItems.get(0));
        for (int i = 1; i < searchItems.size(); i++) {
            int addIndex = getAddIndex(items, searchItems.get(i), i);
            items.add(addIndex, searchItems.get(i));
        }
        return items;
    }

    public int getAddIndex(ArrayList<T> items, SearchItem item, int i){
        if(i <= 0){
            return 0;
        }
        double target = items.get(i-1).getRelPoints();
        double insert = item.getRelPoints();
        if(insert > target){
            return getAddIndex(items, item, i-1);
        }
        return i;
    }

    public ArrayList<String> extractTerms(String searchQuery) {
        ArrayList<String> terms = new ArrayList<>();
        char[] chars = searchQuery.toCharArray();
        int termStart = 0;
        char prev = 0;
        for (int i = 0; i < chars.length; i++) {
            char now = chars[i];
            if(i == chars.length-1){
                terms.add(searchQuery.substring(termStart, i+1));
                break;
            }
            if(i == 0){
                prev = now;
                continue;
            }
            if(Character.isWhitespace(now)){
                if(!Character.isWhitespace(prev)){
                    terms.add(searchQuery.substring(termStart, i));
                }
                termStart = i+1;
                prev = now;
                continue;
            }
            if(!sameType(prev, now)){
                if(!Character.isWhitespace(prev)){
                    terms.add(searchQuery.substring(termStart, i));
                }
                termStart = i;
                prev = now;
            }
        }
        return terms;
    }


    private boolean sameType(char prev, char now){
        if(Character.isLetter(prev) && Character.isLetter(now))
            return true;
        if(Character.isDigit(prev) && Character.isDigit(now))
            return true;
        return false;
    }

    public JSONObject searchItemsToJSON(ArrayList<T> items){
        JSONObject json = new JSONObject();
        for (T searchItem : items) {
            json.append("results", searchItem.parseJSON());
        }
        return json;
    }

}
