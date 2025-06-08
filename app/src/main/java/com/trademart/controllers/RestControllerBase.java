package com.trademart.controllers;

import org.json.JSONObject;

public class RestControllerBase {

    protected JSONObject createResponse(String status, String message) {
        JSONObject json = new JSONObject()
                .put("status", status)
                .put("message", message);
        assert (status.equals("failed") || status.equals("success")) : "Status should only either be failed or success";
        return json;
    }
    
}
