package com.trademart.controllers;

import static com.trademart.util.Logger.LogLevel.WARNING;

import java.net.URI;
import java.util.ArrayList;

import org.json.JSONArray;
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
import com.trademart.db.IDGenerator;
import com.trademart.encryption.Decryptor;
import com.trademart.encryption.Hasher;
import com.trademart.post.PostController;
import com.trademart.user.User;
import com.trademart.user.User.UserBuilder;
import com.trademart.user.UserController;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

@RestController
public class UserRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private UserController userController;
    private PostController postController;

    public UserRestController(SharedResource sharedResource) {
        this.sharedResource = sharedResource;
        this.userController = new UserController(sharedResource);
        this.postController = new PostController(sharedResource);
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
        String responseBody = userLoginProcess(json);
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
        JSONObject response = userCreationProcess(json);
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

    @GetMapping("/user/profile/{user_id}")
    public ResponseEntity<String> fetchUserData(@PathVariable("user_id") int userID) {
        User user = userController.getUserFromDB(userID);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        JSONObject response = new JSONObject()
                .put("username", user.getUsername())
                .put("user_id", user.getId())
                .put("email", user.getEmail())
                .put("verified", user.getVerified())
                .put("post_count", postController.getUserPostCount(userID));
        return ResponseEntity.ok(response.toString());
    }

    private String userLoginProcess(JSONObject json) {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for user creation", WARNING);
        }
        String username = json.getString("username");

        if (!userController.userExists(username)) {
            sharedResource.unlock();
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

    private JSONObject userCreationProcess(JSONObject json) throws JSONException {
        String username = json.getString("username");
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for user creation", WARNING);
        }

        if (userController.userExists(username)) {
            sharedResource.unlock();
            return createUserResponse("failed", "A user with that username already exists", new UserBuilder()
                    .setUsername(username)
                    .build());
        }

        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "users", "user_id");

        sharedResource.unlock();

        String saltIV = json.getString("salt_iv");
        UserBuilder builder = new UserBuilder()
                .setId(id)
                .setUsername(username)
                .setEmail(json.getString("email"))
                .setPassword(json.getString("password"));
        User user = builder.build();

        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for user creation", WARNING);
        }

        userController.insertUserToDB(sharedResource.getDatabaseController(), user, saltIV);

        sharedResource.unlock();
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
