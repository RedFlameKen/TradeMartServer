package com.trademart.controllers;

import static com.trademart.util.Logger.LogLevel.INFO;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.db.IDGenerator;
import com.trademart.encryption.Decryptor;
import com.trademart.encryption.Hasher;
import com.trademart.feed.FeedCategory;
import com.trademart.media.FFmpegUtil;
import com.trademart.media.MediaController;
import com.trademart.post.PostController;
import com.trademart.user.User;
import com.trademart.user.User.UserBuilder;
import com.trademart.user.UserController;
import com.trademart.user.UserPreferences;
import com.trademart.util.FileUtil;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

@RestController
public class UserRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private UserController userController;
    private PostController postController;
    private MediaController mediaController;

    public UserRestController(SharedResource sharedResource) {
        this.sharedResource = sharedResource;
        this.userController = new UserController(sharedResource);
        this.postController = new PostController(sharedResource);
        this.mediaController = new MediaController(sharedResource);
    }

    @PostMapping("/user/login")
    public ResponseEntity<String> loginMapping(@RequestBody String userData) {
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(userData));
        } catch (JSONException e) {
            Logger.log("received a bad request upon login", LogLevel.WARNING);
            return ResponseEntity
                .badRequest()
                .body(createUserResponse("failed", "no such user exists", null)
                        .toString());
        }
        String responseBody;
        try {
            responseBody = userLoginProcess(json);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok().body(responseBody);
    }

    @PostMapping("/user/signup")
    public ResponseEntity<String> signupMapping(@RequestBody String userData) {
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(userData));
        } catch (JSONException e) {
            return ResponseEntity.badRequest().body("");
        }
        URI location = URI.create("");
        Logger.log("starting user creation process", INFO);
        JSONObject response;
        try {
            response = userCreationProcess(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return badRequestResponse("received a badly formatted request");
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        if (response.getString("status").equals("success")) {
            JSONObject userJson = response.getJSONObject("user_data");
            location = URI.create(new StringBuilder()
                    .append("/user/")
                    .append(userJson.getInt("user_id"))
                    .toString());
            return ResponseEntity.created(location).body(response.toString());
        }
        return ResponseEntity.ok().body(response.toString());
    }

    @PostMapping("/user/preferences/update")
    public ResponseEntity<String> setUserPreferencesMapping(@RequestBody String body){
        UserPreferences preferences;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            preferences = new UserPreferences(json.getInt("user_id"));
            if(json.has("preferred_category")){
                preferences.setPreferredCategory(FeedCategory.parse(json.getString("preferred_category")));
            }
        } catch (JSONException e) {
            return badRequestResponse("unable to set preferences");
        }
        try {
            userController.updateUserPreferences(preferences);
        } catch (SQLException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "preferences updated!")
                .put("data", preferences.parseJson()).toString());
    }

    @PostMapping("/user/preferences")
    public ResponseEntity<String> fetchUserPreferencesMapping(@RequestBody String body){
        int userId;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            userId = json.getInt("user_id");
        } catch (JSONException e) {
            return badRequestResponse("unable to set preferences");
        }
        UserPreferences preferences;
        try {
            preferences = userController.getUserPreferences(userId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "fetched user preferences")
                .put("data", preferences.parseJson()).toString());
    }

    @GetMapping("/user/profile/{user_id}")
    public ResponseEntity<String> fetchUserDataMapping(@PathVariable("user_id") int userID) {
        User user;
        try {
            user = userController.findUserById(userID);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        JSONObject response;
        try {
            response = new JSONObject()
                    .put("username", user.getUsername())
                    .put("user_id", user.getId())
                    .put("email", user.getEmail())
                    .put("verified", user.getVerified())
                    .put("post_count", postController.getUserPostCount(userID));
        } catch (JSONException e) {
            e.printStackTrace();
            return badRequestResponse("unable to post");
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(response.toString());
    }

    @GetMapping("/user/{user_id}/avatar")
    public ResponseEntity<byte[]> updateProfilePictureMapping(@PathVariable("user_id") int userId){
        User user;
        try {
            user = userController.findUserById(userId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        if(user == null){
            return ResponseEntity.notFound().build();
        }
        String profilePicturePath = user.getProfilePicturePath();
        if(profilePicturePath == null || profilePicturePath.equals("")){
            return ResponseEntity.notFound().build();
        }
        byte[] data = mediaController.readFileBytes(new File(profilePicturePath));
        String filename = userController.createProfilePicturePath(mediaController.imagesDir(), userId, "jpg");
        File file = new File(filename);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaController.getMediaTypeEnum(file.getName()));
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(file.getName())
                .build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @PostMapping("/user/{user_id}/avatar/update")
    public ResponseEntity<String> updateProfilePictureMapping(@PathVariable("user_id") int userId, @RequestHeader("Content-Disposition") String dispositionStr, @RequestBody byte[] content){
        User user;
        Logger.log("updating avatar...", INFO);
        try {
            user = userController.findUserById(userId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        if(user == null){
            Logger.log("user not found", INFO);
            return ResponseEntity.notFound().build();
        }
        ContentDisposition disposition = ContentDisposition.parse(dispositionStr);
        String outputFilename = userController
            .createProfilePicturePath(mediaController.imagesDir(), userId, "jpg");

        try {
            Logger.log("writing", INFO);
            mediaController.writeFileNoEncode(outputFilename, content);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        String filename = disposition.getFilename();
        String fileExtension = FileUtil.getExtension(filename);
        if(fileExtension != "jpg"){
            FFmpegUtil.encodeFile(outputFilename, "jpg");
        }
        Logger.log("saving to db...", INFO);
        userController.updateProfilePicture(userId, outputFilename);
        Logger.log("Profile picture of user: " + userId + "has been updated", LogLevel.INFO);
        return ResponseEntity.ok("");
    }

    private String userLoginProcess(JSONObject json) throws InterruptedException, SQLException {
        String username = json.getString("username");

        if (!userController.userExists(username)) {
            return createUserResponse("failed", "no such user exists", new UserBuilder()
                    .setUsername(username)
                    .build()).toString();
        }

        String password = json.getString("password");
        String saltIv = json.getString("salt_iv");
        Decryptor decryptor = new Decryptor(saltIv);
        String decryptedPassword = decryptor.decrypt(password);

        User user = userController.findUserByUsername(username);

        sharedResource.unlock();

        Hasher hasher = new Hasher(user.getPasswordSalt());
        String hashedPassword = hasher.hash(decryptedPassword);

        if (!hashedPassword.equals(user.getPassword())) {
            return createUserResponse("failed", "incorrect password", new UserBuilder()
                    .setUsername(username)
                    .build()).toString();
        }

        return createUserResponse("success", "logged in successfully", user).toString();
    }

    private JSONObject userCreationProcess(JSONObject json) throws JSONException, InterruptedException, SQLException {
        String username = json.getString("username");

        if (userController.userExists(username)) {
            return createUserResponse("failed", "A user with that username already exists", new UserBuilder()
                    .setUsername(username)
                    .build());
        }

        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "users", "user_id");

        String saltIV = json.getString("salt_iv");
        UserBuilder builder = new UserBuilder()
                .setId(id)
                .setUsername(username)
                .setEmail(json.getString("email"))
                .setPassword(json.getString("password"));
        User user = builder.build();

        userController.insertUserToDB(user, saltIV);

        return createUserResponse("success", "account created", user);
    }

    private String createUserCreationResponse(User user) {
        JSONObject json = new JSONObject();
        json.put("id", user.getId());
        json.put("username", user.getUsername());
        json.put("email", user.getEmail());
        json.put("password", user.getPassword());
        return json.toString();
    }

    private JSONObject createUserResponse(String status, String message, User user) {
        JSONObject json = createResponse(status, message);
        JSONObject userJson = new JSONObject();

        if(user != null) {
            if (status.equals("failed")) {
                userJson.put("username", user.getUsername());
            } else if (status.equals("success")) {
                userJson.put("user_id", user.getId())
                    .put("username", user.getUsername())
                    .put("email", user.getEmail())
                    .put("verified", user.getVerified());
            }
        }
        json.put("user_data", userJson);
        return json;
    }
}
