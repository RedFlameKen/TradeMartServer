package com.trademart.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

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
import com.trademart.media.MediaController;
import com.trademart.post.Post;
import com.trademart.post.Post.PostBuilder;
import com.trademart.post.PostController;
import com.trademart.user.User;
import com.trademart.user.UserController;
import com.trademart.util.Encoder;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

@RestController
public class PostRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private UserController userController;
    private PostController postController;

    private MediaController mediaController;

    public PostRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        this.userController = new UserController(sharedResource);
        this.postController = new PostController(sharedResource);
        this.mediaController = new MediaController(sharedResource);
    }

    @PostMapping("/post/publish")
    public ResponseEntity<String> publishPostMapping(@RequestBody String post_data){
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(post_data));
        } catch (JSONException e) {
            Logger.log("received a bad request upon publishing a post", LogLevel.WARNING);
            return ResponseEntity
                .badRequest()
                .body(createResponse("failed", "received a bad request upon login").toString());
        }
        int userId = json.getInt("user_id");
        User user = userController.getUserFromDB(userId);
        if (user == null) {
            return ResponseEntity.ok(createResponse("failed", "no user with the given user_id was found").toString());
        }
        Post createdPost = null;
        try {
            createdPost = publishPost(json, userId);
        } catch (JSONException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(createdPost.parseJSON().toString());
    }

    @GetMapping("/post/{post_id}")
    public ResponseEntity<String> fetchPostMapping(@PathVariable("post_id") int postId){
        Post post = postController.findPostByID(postId);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        JSONObject json = post.parseJSON();
        return ResponseEntity.ok(json.toString());
    }

    @GetMapping("/post/{post_id}/media")
    public ResponseEntity<String> fetchPostMediaIDsMapping(@PathVariable("post_id") int postId){
        JSONObject json = new JSONObject();
        try {
            ArrayList<Integer> ids = postController.getPostMediaIDs(postId);
            json.put("media_ids", ids);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(json.toString());
    }

    @PostMapping("/post/publish/{post_id}/media")
    public ResponseEntity<String> uploadPostMediaMapping(@PathVariable("post_id") int postId, @RequestBody String media_data){
        Post post = postController.findPostByID(postId);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(media_data));
        } catch (JSONException e) {
            return ResponseEntity.badRequest().build();
        }
        String filename = json.getString("filename");
        String encodedData = json.getString("data");
        byte[] data = Encoder.decodeBase64(encodedData);
        try {
            File file = mediaController.writeFile(filename, data);
            mediaController.insertPostMediaToDB(file.getAbsolutePath(), post.getUserId(), postId);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(createResponse("success", "image uploaded successfully").toString());
    }

    private Post publishPost(JSONObject json, int userId) throws JSONException {
        String title = json.getString("title");
        String description = json.getString("description");
        int id = postController.generatePostID();

        Post post = new PostBuilder()
            .setTitle(title)
            .setDescription(description)
            .setUserId(userId)
            .setPostId(id)
            .setLikes(0)
            .build();
        try {
            postController.insertPostToDB(post);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return post;
    }

}
