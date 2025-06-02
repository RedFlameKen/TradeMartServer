package com.trademart.controllers;

import static com.trademart.util.Logger.LogLevel.WARNING;

import java.net.URI;
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
import com.trademart.db.IDGenerator;
import com.trademart.encryption.Decryptor;
import com.trademart.encryption.Hasher;
import com.trademart.user.User;
import com.trademart.user.User.UserBuilder;
import com.trademart.util.Logger;

@RestController
public class UserController {

    private SharedResource sharedResource;

    public UserController(SharedResource sharedResource) {
        this.sharedResource = sharedResource;
    }

    @PostMapping("/user/login")
    public ResponseEntity<String> loginMapping(@RequestBody String userData) {
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(userData));
        } catch (JSONException e) {
            return ResponseEntity.badRequest().body("");
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
        if(response.getString("status").equals("sucess")){
            location = URI.create(new StringBuilder()
                    .append("/user/")
                    .append(response.getString("user_id"))
                    .toString());
        }
        return ResponseEntity.created(location).body(response.toString());
    }

    @GetMapping("/user/{userID}")
    public String fetchUserData(@PathVariable("userID") int userID) {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for user fetch", WARNING);
        }

        User user = getUserFromDB(userID);

        sharedResource.unlock();

        JSONObject response = new JSONObject()
            .put("username", user.getUsername())
            .put("user_id", user.getId())
            .put("email", user.getEmail())
            .put("verified", user.getVerified());

        return response.toString();
    }

    private String userLoginProcess(JSONObject json) {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for user creation", WARNING);
        }
        String username = json.getString("username");

        if(!userExists(username)){
            sharedResource.unlock();
            return createResponse("failed", "no such user exists", new UserBuilder()
                    .setUsername(username)
                    .build()).toString();
        }

        String password = json.getString("password");
        String saltIv = json.getString("salt_iv");
        Decryptor decryptor = new Decryptor(saltIv);
        String decryptedPassword = decryptor.decrypt(password);

        User user = findUserByUsername(username);

        sharedResource.unlock();

        Hasher hasher = new Hasher(user.getPasswordSalt());
        String hashedPassword = hasher.hash(decryptedPassword);

        if(!hashedPassword.equals(user.getPassword())){
            return createResponse("failed", "incorrect password", new UserBuilder()
                    .setUsername(username)
                    .build()).toString();
        }

        return createResponse("success", "logged in successfully", user).toString();
    }

    private JSONObject userCreationProcess(JSONObject json) throws JSONException {
        String username = json.getString("username");
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for user creation", WARNING);
        }

        if(userExists(username)){
            sharedResource.unlock();
            return createResponse("failed", "A user with that username already exists", new UserBuilder()
                    .setUsername(username)
                    .build());
        }

        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "users", "user_id");

        sharedResource.unlock();

        String saltIV = json.getString("salt_iv");
        UserBuilder builder = new UserBuilder()
                .setId(id)
                .setUsername(json.getString("username"))
                .setEmail(json.getString("email"))
                .setPassword(json.getString("password"));
        User user = builder.build();

        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for user creation", WARNING);
        }

        insertUserToDB(sharedResource.getDatabaseController(), user, saltIV);

        sharedResource.unlock();
        return createResponse("success", "account created", user);
    }

    private void insertUserToDB(DatabaseController dbController, User user, String saltIV) {
        String cmd = "insert into users(user_id, username, email, password, password_salt, verified) values (?, ?, ?, ?, ?, ?)";
        String decryptedPassword = new Decryptor(saltIV).decrypt(user.getPassword());
        Hasher hasher = new Hasher();
        String hashSalt = hasher.getSalt();
        String hashedPassword = hasher.hash(decryptedPassword);
        try {
            PreparedStatement prep = dbController.prepareStatement(cmd);
            prep.setInt(1, user.getId());
            prep.setString(2, user.getUsername());
            prep.setString(3, user.getEmail());
            prep.setString(4, hashedPassword);
            prep.setString(5, hashSalt);
            prep.setBoolean(6, false);
            prep.execute();
        } catch (SQLException e) {
            Logger.log("Unable to insert user to db", WARNING);
        }
    }

    private String createUserCreationResponse(User user) {
        JSONObject json = new JSONObject();
        json.put("id", user.getId());
        json.put("username", user.getUsername());
        json.put("email", user.getEmail());
        json.put("password", user.getPassword());
        return json.toString();
    }

    private JSONObject createResponse(String status, String message, User user){
        JSONObject json = new JSONObject()
            .put("status", status)
            .put("message", message);
        JSONObject userJson = new JSONObject();
        if(status.equals("failed")){
            userJson.put("username", user.getUsername());
        } else {
            userJson.put("user_id", user.getId())
                .put("username", user.getUsername())
                .put("email", user.getEmail())
                .put("verified", user.getVerified());
        }
        json.put("user_data", userJson);
        return json;
    }

    private boolean userExists(String username){
        if(findUserByUsername(username) != null){
            return true;
        }
        return false;
    }

    private User findUserByUsername(String username) {
        DatabaseController dbController = sharedResource.getDatabaseController();
        User user = null;
        try {
            String command = "select * from users where username='" + username + "'";
            if(dbController.getCommandRowCount(command) < 1){
                return null;
            }
            ResultSet rs = dbController.execQuery(command);
            user = getUserFromResultSet(rs);
        } catch (SQLException e) {
            Logger.log("Unable to get a user from the db", WARNING);
        }
        return user;
    }

    private User getUserFromDB(int userID) {
        User user = null;
        try {
            ResultSet rs = sharedResource.getDatabaseController().execQuery("select * from users where user_id=" + userID);
            rs.next();
            user = new UserBuilder()
                .setId(rs.getInt("user_id"))
                .setUsername(rs.getString("username"))
                .setEmail(rs.getString("email"))
                .setVerified(rs.getBoolean("verified"))
                .build();
        } catch (SQLException e) {
            Logger.log("Unable to get a user from the db", WARNING);
            e.printStackTrace();
        }
        return user;
    }

    private User getUserFromResultSet(ResultSet rs) throws SQLException {
        rs.next();
        return new UserBuilder()
            .setId(rs.getInt("user_id"))
            .setUsername(rs.getString("username"))
            .setEmail(rs.getString("email"))
            .setPassword(rs.getString("password"))
            .setPasswordSalt(rs.getString("password_salt"))
            .setVerified(rs.getBoolean("verified"))
            .build();
    }

}
