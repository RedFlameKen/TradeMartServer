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

    @PostMapping("/user/signup")
    public ResponseEntity<String> createUser(@RequestBody String userData){
        JSONObject json = null;
        try {
        json = new JSONObject(new JSONTokener(userData));
        } catch (JSONException e){
            return ResponseEntity.badRequest().body("");
        }
        User createdUser = userCreationProcess(json);
        URI location = URI.create(new StringBuilder()
                .append("/user/")
                .append(createdUser.getId())
                .toString());
        String response = createUserCreationResponse(createdUser);
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/user/{userID}")
    public User fetchUserData(@PathVariable("userID") int userID){
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for user fetch", WARNING);
        }

        User user = getUserFromDB(sharedResource.getDatabaseController(), userID);

        sharedResource.unlock();

        return user;
    }

    private User userCreationProcess(JSONObject json){
        String saltIV = json.getString("salt_iv");

        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for user creation", WARNING);
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "users", "user_id");

        sharedResource.unlock();

        UserBuilder builder = new UserBuilder()
            .setId(id)
            .setName(json.getString("name"))
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
        return user;
    }

    private void insertUserToDB(DatabaseController dbController, User user, String saltIV){
        String cmd = "insert into users(user_id, name, email, password, password_salt) values (?, ?, ?, ?, ?)";
        String decryptedPassword = new Decryptor(saltIV).decrypt(user.getPassword());
        Hasher hasher = new Hasher();
        String hashSalt = hasher.getSalt();
        String hashedPassword = hasher.hash(decryptedPassword);
        try {
            PreparedStatement prep = dbController.prepareStatement(cmd);
            prep.setInt(1, user.getId());
            prep.setString(2, user.getName());
            prep.setString(3, user.getEmail());
            prep.setString(4, hashedPassword);
            prep.setString(5, hashSalt);
            prep.execute();
        } catch (SQLException e) {
            Logger.log("Unable to insert user to db", WARNING);
        }
    }

    private String createUserCreationResponse(User user){
        JSONObject json = new JSONObject();
        json.put("id", user.getId());
        json.put("name", user.getName());
        json.put("email", user.getEmail());
        json.put("password", user.getPassword());
        return json.toString();
    }

    private User getUserFromDB(DatabaseController dbController, int userID){
        User user = null;
        try {
            ResultSet rs = dbController.execQuery("select * from users where user_id="+userID);
            rs.next();
            UserBuilder builder = new UserBuilder()
                .setId(rs.getInt("user_id"))
                .setName(rs.getString("name"))
                .setEmail(rs.getString("email"));
            user = builder.build();
        } catch (SQLException e) {
            Logger.log("Unable to get a user from the db", WARNING);
        }
        return user;
    }

}
