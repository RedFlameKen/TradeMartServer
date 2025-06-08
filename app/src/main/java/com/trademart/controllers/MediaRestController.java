package com.trademart.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.media.MediaController;
import com.trademart.post.PostController;
import com.trademart.user.UserController;
import com.trademart.util.Encoder;

@RestController
public class MediaRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private MediaController mediaController;
    private PostController postController;
    private UserController userController;

    public MediaRestController(SharedResource sharedResource) {
        this.sharedResource = sharedResource;
        this.mediaController = new MediaController(sharedResource);
        this.postController = new PostController(sharedResource);
        this.userController = new UserController(sharedResource);
    }

    // @PostMapping("/media/upload")
    // public ResponseEntity<String> uploadMedia(@RequestBody String file_data){
    //     JSONObject json = null;
    //     try {
    //         json = new JSONObject(new JSONTokener(file_data));
    //     } catch (JSONException e) {
    //         e.printStackTrace();
    //         return ResponseEntity.badRequest().body("");
    //     }
    //     String filename = json.getString("filename");
    //     String encodedBytes = json.getString("data");
    //     byte[] data = Encoder.decodeBase64(encodedBytes);
    //     try {
    //         controller.writeFile(filename, data);
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //         return ResponseEntity.internalServerError().build();
    //     }
    //     return ResponseEntity.ok(createResponse("successful", "file was successfully stored").toString());
    // }

    @GetMapping("/media/{media_id}")
    public ResponseEntity<String> fetchMediaMapping(@PathVariable("media_id") int mediaId){
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        DatabaseController db = sharedResource.getDatabaseController();
        String command = "select * from media where media_id=?";

        String filepath = null;
        try {
            PreparedStatement prep = db.prepareStatement(command);
            prep.setInt(1, mediaId);
            ResultSet rs = prep.executeQuery();
            rs.next();
            filepath = rs.getString("media_url");
        } catch (SQLException e) {
            return ResponseEntity.notFound().build();
        }

        sharedResource.unlock();

        File file = new File(filepath);
        byte[] fileData = mediaController.readFileBytes(file);
        String encodedData = Encoder.encodeBase64(fileData);
        JSONObject json = new JSONObject()
            .put("filename", file.getName())
            .put("data", encodedData);

        return ResponseEntity.ok(json.toString());
    }

}
