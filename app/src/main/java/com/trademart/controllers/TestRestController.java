package com.trademart.controllers;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestRestController {

    @GetMapping("/test")
    public ResponseEntity<String> testRequestParamsMapping(@RequestParam("q")String query){
        JSONObject json = new JSONObject()
            .put("value", query);
        return ResponseEntity.ok(json.toString());
    }
    
}
