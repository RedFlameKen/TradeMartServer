package com.trademart.controllers;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.media.MediaController;
import com.trademart.util.Encoder;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

@RestController
public class MediaRestController extends RestControllerBase {

    @Autowired
    private MediaController controller;

    @PostMapping("/media/upload")
    public ResponseEntity<String> uploadMedia(@RequestBody String file_data){
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(file_data));
        } catch (JSONException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("");
        }
        String filename = json.getString("filename");
        String encodedBytes = json.getString("data");
        byte[] data = Encoder.decodeBase64(encodedBytes);
        try {
            controller.writeFile(filename, data);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(createResponse("successful", "file was successfully stored").toString());
    }

}
