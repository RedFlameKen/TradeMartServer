package com.trademart.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.tomcat.util.http.parser.ContentRange;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
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

    @GetMapping("/lorem")
    public ResponseEntity<String> testMapping(@RequestHeader("Content-Range") String range){
        String data = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        ContentRange contentRange = null;
        try {
            contentRange = ContentRange.parse(new StringReader(range));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Content-Range: %s %d-%d/%d", contentRange.getUnits(), contentRange.getStart(), contentRange.getEnd(), contentRange.getLength());
        ResponseEntity<String> response = ResponseEntity.status(206).contentLength(29).body(data.substring((int) contentRange.getStart(), (int) contentRange.getEnd()));
        return response;

    }

    @GetMapping("/videoranged")
    public ResponseEntity<byte[]> videoRangedMapping(@RequestHeader("Content-Length") int length, @RequestHeader("Content-Range") String rangeStr) throws IOException{
        ContentRange range = ContentRange.parse(new StringReader(rangeStr));

        byte[] bytes = new byte[length];
        File file = new File("/home/redflameken/Storage/media/videos/output.mp4");
        try (FileInputStream reader = new FileInputStream(file)) {
            reader.readNBytes(bytes, (int) range.getStart(), (int) range.getEnd());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(206).contentType(MediaTypeFactory.getMediaType(file.getAbsolutePath()).orElse(MediaType.APPLICATION_OCTET_STREAM)).body(bytes);

    }

    @GetMapping("/videothing")
    public ResponseEntity<byte[]> videoMapping(){
        // try {
        //     range = ContentRange.parse(new StringReader(rangeString));
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        // byte[] bytes = new byte[4096];
        // File file = new File("/home/redflameken/Storage/media/videos/output.mp4");
        // try (FileInputStream reader = new FileInputStream(file)) {
        //     reader.readNBytes(bytes, 0, 4096);
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        byte[] bytes = null;
        File file = new File("/home/redflameken/Storage/media/videos/hls/Qjhha2VFZWdXa3UrcHBZMFkzdjBYQT09.m3u8");
        try (FileInputStream reader = new FileInputStream(file)) {
            bytes = reader.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(206).contentType(MediaTypeFactory.getMediaType(file.getAbsolutePath()).orElse(MediaType.APPLICATION_OCTET_STREAM)).body(bytes);

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
        int mediaId = -1;
        byte[] data = Encoder.decodeBase64(encodedData);
        try {
            File file = mediaController.writeFile(filename, data);
            mediaId = mediaController.insertPostMediaToDB(file.getAbsolutePath(), post.getUserId(), postId);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).header("Location",
                new StringBuilder()
                .append("/media/")
                .append(mediaId)
                .toString())
            .body(createResponse("success", "image uploaded successfully").toString());
    }

    @PostMapping("/post/user/{user_id}")
    public ResponseEntity<String> fetchPostsByUser(@PathVariable("user_id") int userId, @RequestBody String loadedIds){
        User user = userController.getUserFromDB(userId);
        if(user == null){
            return ResponseEntity.notFound().build();
        }
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<Integer> newIds = null;
        JSONObject newIdsJson = new JSONObject();
        try {
            JSONObject json = new JSONObject(new JSONTokener(loadedIds));
            JSONArray idsJson = json.getJSONArray("post_ids");
            for(int i = 0; i < idsJson.length(); i++){
                ids.add(idsJson.getInt(i));
            }
            newIds = getUnloadedPostIDs(ids, userId);
            newIdsJson.put("post_ids", newIds);
        } catch (JSONException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        Logger.log("resposne json: " + newIdsJson.toString(), LogLevel.INFO);
        return ResponseEntity.ok(newIdsJson.toString());
    }

    private ArrayList<Integer> getUnloadedPostIDs(ArrayList<Integer> loadedIds, int userId){
        String command = buildUnloadedPostIDsCommand(loadedIds, userId);
        ArrayList<Integer> newIds = new ArrayList<>();
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DatabaseController db = sharedResource.getDatabaseController();
        try {
            PreparedStatement prep = db.prepareStatement(command);
            ResultSet rs = prep.executeQuery();
            while(rs.next()){
                newIds.add(rs.getInt("post_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        sharedResource.unlock();
        return newIds;
    }

    private String buildUnloadedPostIDsCommand(ArrayList<Integer> loadedIds, int userId){
        StringBuilder command = new StringBuilder()
            .append("select post_id from posts where user_id=").append(userId);
        if(loadedIds.size() > 0){
            command.append(" and");
            for (int i = 0; i < loadedIds.size(); i++) {
                command.append(" not post_id=").append(loadedIds.get(i));
                if(i < loadedIds.size()-1){
                    command.append(" and");
                }
            }
        }
        // command.append(" limit 8");
        Logger.log("command was: " + command, LogLevel.INFO);
        return command.toString();
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
