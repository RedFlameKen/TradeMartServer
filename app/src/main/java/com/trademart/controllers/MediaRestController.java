package com.trademart.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.media.MediaController;
import com.trademart.post.PostController;
import com.trademart.user.User;
import com.trademart.user.UserController;
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

    @GetMapping("/media/thumbnail/{media_id}")
    private ResponseEntity<byte[]> serveMediaThumbnailByIDMapping(@PathVariable("media_id") Integer mediaId){
        File file = null;
        try {
            file = new File(mediaController.getMediaPathByID(mediaId));
        } catch (SQLException e) {
            return ResponseEntity.notFound().build();
        } catch (InterruptedException e) {
            sharedResource.unlock();
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

    @PostMapping("/media/{user_id}/upload")
    private ResponseEntity<String> uploadMediaMapping(@PathVariable("user_id") int userId, @RequestHeader("Content-Disposition") String dispositionStr, @RequestBody byte[] data){
        String filename = ContentDisposition.parse(dispositionStr).getFilename();
        int mediaId = -1;
        try {
            File file = mediaController.writeFile(filename, data);
            String filepath = file.getAbsolutePath();
            String ext = FileUtil.getExtension(file.getName());
            if(ext.equals("mp4")){
                filepath = FileUtil.removeExtension(filepath).concat(".m3u8");
            }
            mediaId = mediaController.generateMediaID();
            mediaController.insertMediaToDB(filepath, mediaId, userId);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return internalServerErrorResponse("unable to upload media");
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("unable to upload media");
        }

        return ResponseEntity.ok(createResponse("success", "image uploaded successfully")
                .put("media_id", mediaId).toString());
    }

    @GetMapping("/media/{media_id}")
    private ResponseEntity<byte[]> serveMediaByIDMapping(@PathVariable("media_id") Integer mediaId){
        File file = null;
        try {
            file = new File(mediaController.getMediaPathByID(mediaId));
        } catch (SQLException e) {
            return ResponseEntity.notFound().build();
        } catch (InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
        byte[] bytes = mediaController.readFileBytes(file);
        HttpHeaders headers = new HttpHeaders();
        URI location = null;
        if(mediaController.isHLSType(FileUtil.getExtension(file.getName()))){
            location = URI.create("/media/video/".concat(file.getName()));
        } else if(mediaController.isImageType(FileUtil.getExtension(file.getName()))) {
            location = URI.create("/media/image/".concat(file.getName()));
        }
        // URI uri = new URI(new StringBuilder()
        //         .append("/media/"));
        headers.setContentType(mediaController.getMediaTypeEnum(file.getName()));
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(file.getName())
                .build());
        BodyBuilder builder = ResponseEntity.ok().headers(headers);
        if(location != null){
            builder.location(location);
        }
        return builder.body(bytes);
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
