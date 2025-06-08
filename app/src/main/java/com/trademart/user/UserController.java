package com.trademart.user;

import static com.trademart.util.Logger.LogLevel.WARNING;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.encryption.Decryptor;
import com.trademart.encryption.Hasher;
import com.trademart.user.User.UserBuilder;
import com.trademart.util.Logger;

public class UserController {

    private SharedResource sharedResource;

    public UserController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
    }

    public boolean userExists(String username) {
        if (findUserByUsername(username) != null) {
            return true;
        }
        return false;
    }

    public User findUserByUsername(String username) {
        DatabaseController dbController = sharedResource.getDatabaseController();
        User user = null;
        try {
            String command = "select * from users where username='" + username + "'";
            if (dbController.getCommandRowCount(command) < 1) {
                return null;
            }
            ResultSet rs = dbController.execQuery(command);
            user = getUserFromResultSet(rs);
        } catch (SQLException e) {
            Logger.log("Unable to get a user from the db", WARNING);
        }
        return user;
    }

    public User getUserFromDB(int userID) {
        User user = null;
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for user fetch", WARNING);
        }

        try {
            ResultSet rs = sharedResource.getDatabaseController()
                    .execQuery("select * from users where user_id=" + userID);
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
        sharedResource.unlock();
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

    public void insertUserToDB(DatabaseController dbController, User user, String saltIV) {
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
    
}
