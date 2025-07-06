package com.trademart.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.media.MediaController;
import com.trademart.post.PostController;
import com.trademart.user.UserController;
import com.trademart.util.Encoder;
import com.trademart.util.FileUtil;

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

    // @GetMapping("/media/{media_id}")
    // public ResponseEntity<String> fetchMediaMapping(@PathVariable("media_id") int mediaId){
    //     String filepath = null;
    //     try {
    //         filepath = getMediaPathByID(mediaId);
    //     } catch (InterruptedException e) {
    //         return ResponseEntity.internalServerError().build();
    //     } catch (SQLException e) {
    //         return ResponseEntity.notFound().build();
    //     }
    //
    //     File file = new File(filepath);
    //     byte[] fileData = mediaController.readFileBytes(file);
    //     String encodedData = Encoder.encodeBase64(fileData);
    //     JSONObject json = new JSONObject()
    //         .put("filename", file.getName())
    //         .put("data", encodedData);
    //
    //     return ResponseEntity.ok(json.toString());
    // }

    private String getMediaPathByID(int mediaId) throws SQLException, InterruptedException {
        DatabaseController db = sharedResource.getDatabaseController();
        String command = "select * from media where media_id=?";
        String filepath = null;
        sharedResource.lock();
        PreparedStatement prep = db.prepareStatement(command);
        prep.setInt(1, mediaId);
        ResultSet rs = prep.executeQuery();
        rs.next();
        sharedResource.unlock();
        filepath = rs.getString("media_url");
        return filepath;
    }

    @GetMapping("/media/thumbnail/{media_id}")
    private ResponseEntity<byte[]> serveMediaThumbnailByIDMapping(@PathVariable("media_id") Integer mediaId){
        File file = null;
        try {
            file = new File(getMediaPathByID(mediaId));
        } catch (SQLException e) {
            return ResponseEntity.notFound().build();
        } catch (InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
        byte[] bytes = null;
        String ext = FileUtil.getExtension(file.getName());
        if(ext.equals("m3u8") || ext.equals("mp4")){
            File thumbnailFile = mediaController.getThumbnailFile(file.getName());
            bytes = mediaController.readFileBytes(thumbnailFile);
            file = thumbnailFile;
        } else {
            bytes = mediaController.readFileBytes(file);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaController.getMediaTypeEnum(file.getName()));
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(file.getName())
                .build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/media/{media_id}")
    private ResponseEntity<byte[]> serveMediaByIDMapping(@PathVariable("media_id") Integer mediaId){
        File file = null;
        try {
            file = new File(getMediaPathByID(mediaId));
        } catch (SQLException e) {
            return ResponseEntity.notFound().build();
        } catch (InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
        byte[] bytes = mediaController.readFileBytes(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaController.getMediaTypeEnum(file.getName()));
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(file.getName())
                .build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/media/image/{filename}")
    private ResponseEntity<byte[]> serveImageMapping(@PathVariable("filename") String filename){
        File file = mediaController.getImageFile(filename);
        if(!file.exists()){
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = mediaController.readFileBytes(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaController.getMediaTypeEnum(filename));
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(filename)
                .build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/media/video/{filename}")
    private ResponseEntity<byte[]> serveVideoMapping(@PathVariable("filename") String filename){
        File file = mediaController.getVideoHLSFile(filename);
        if(!file.exists()){
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = mediaController.readFileBytes(file);
        HttpHeaders headers = new HttpHeaders();
        if(FileUtil.getExtension(filename).equalsIgnoreCase("m3u8")){
            headers.setContentType(MediaType.parseMediaType("application/x-mpegURL"));
        } else if(FileUtil.getExtension(filename).equalsIgnoreCase("ts")){
            headers.setContentType(MediaType.parseMediaType("video/mp2t"));
        }
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

}
