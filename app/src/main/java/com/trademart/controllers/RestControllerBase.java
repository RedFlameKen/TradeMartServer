package com.trademart.controllers;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

public class RestControllerBase {

    protected JSONObject createResponse(String status, String message) {
        JSONObject json = new JSONObject()
                .put("status", status)
                .put("message", message);
        assert (status.equals("failed") || status.equals("success")) : "Status should only either be failed or success";
        return json;
    }
    
    protected ResponseEntity<String> internalServerErrorResponse(String message){
        return ResponseEntity.internalServerError().body(createResponse("failed", message).toString());
    }

    protected ResponseEntity<String> internalServerErrorResponse(){
        return ResponseEntity.internalServerError().body(createResponse("failed", "an internal server error occured").toString());
    }


    protected ResponseEntity<String> badRequestResponse(String message){
        return ResponseEntity.badRequest().body(createResponse("failed", message).toString());
    }

    protected ResponseEntity<String> notFoundResponse(){
        return ResponseEntity.notFound().build();
    }

}
