package com.trademart.user;

import static com.trademart.util.Logger.LogLevel.WARNING;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.encryption.Decryptor;
import com.trademart.encryption.Hasher;
import com.trademart.feed.FeedCategory;
import com.trademart.user.User.UserBuilder;
import com.trademart.util.Logger;

public class UserController {

    private SharedResource sharedResource;
    private DatabaseController dbController;

    public UserController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        dbController = sharedResource.getDatabaseController();
    }

    public boolean userExists(String username) {
        if (findUserByUsername(username) != null) {
            return true;
        }
        return false;
    }

    public User findUserByUsername(String username) {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        sharedResource.unlock();
        return user;
    }

    public UserPreferences getUserPreferences(int userId) throws InterruptedException, SQLException{
        UserPreferences preferences = null;
        String command = "select * from user_preferences where user_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        ResultSet rs = prep.executeQuery();
        if(rs.next()){
            preferences = new UserPreferences(userId);
            preferences.setPreferredCategory(FeedCategory.parse(rs.getString("preferred_category")));
        }
        sharedResource.unlock();
        return preferences;
    }

    public ArrayList<User> getAllUsersFromDB() throws InterruptedException, SQLException{
        ArrayList<User> users = new ArrayList<>();
        String command = "select * from users";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            users.add(new UserBuilder()
                .setId(rs.getInt("user_id"))
                .setUsername(rs.getString("username"))
                .setEmail(rs.getString("email"))
                .setPassword(rs.getString("password"))
                .setPasswordSalt(rs.getString("password_salt"))
                .setVerified(rs.getBoolean("verified"))
                .setProfilePicturePath(rs.getString("profile_picture_path"))
                .build());
        }
        sharedResource.unlock();
        return users;
    }

    // Logger.log("Unable to lock resources for user fetch", WARNING);
    public User findUserById(int userId) throws InterruptedException, SQLException {
        User user = null;
        sharedResource.lock();
        String command = "select * from users where user_id=?";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        ResultSet rs = prep.executeQuery();
        if(rs.next()){
            user = new UserBuilder()
                .setId(rs.getInt("user_id"))
                .setUsername(rs.getString("username"))
                .setEmail(rs.getString("email"))
                .setVerified(rs.getBoolean("verified"))
                .setProfilePicturePath(rs.getString("profile_picture_path"))
                .build();
        }
        sharedResource.unlock();
        return user;
    }

    private User getUserFromResultSet(ResultSet rs) throws SQLException {
        rs.next();
        String profilePicturePath = rs.getString("profile_picture_path");
        if(rs.wasNull()){
            profilePicturePath = "";
        }
        return new UserBuilder()
                .setId(rs.getInt("user_id"))
                .setUsername(rs.getString("username"))
                .setEmail(rs.getString("email"))
                .setPassword(rs.getString("password"))
                .setPasswordSalt(rs.getString("password_salt"))
                .setVerified(rs.getBoolean("verified"))
                .setProfilePicturePath(profilePicturePath)
                .build();
    }

    public String createProfilePicturePath(String parent, int userId, String extension){
        return new StringBuilder()
            .append(parent)
            .append("/pfp_")
            .append(userId)
            .append(".")
            .append(extension)
            .toString();
    }

    public void updateProfilePicture(int userId, String path){
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String command = "update users set profile_picture_path=? where user_id=?";

        try {
            PreparedStatement prep = dbController.prepareStatement(command);
            prep.setString(1, path);
            prep.setInt(2, userId);
            prep.execute();
            prep.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        sharedResource.unlock();
    }

    public void insertUserToDB(User user, String saltIV) {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
            prep.close();

            writeUserPreferencesToDb(user.getId());
        } catch (SQLException e) {
            Logger.log("Unable to insert user to db", WARNING);
        }
        sharedResource.unlock();
    }

    public void updateUserPreferences(UserPreferences preferences) throws SQLException, InterruptedException{
        String command = "update user_preferences set preferred_category=? where user_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setString(1, preferences.getPreferredCategory().toString());
        prep.setInt(2, preferences.getUserId());
        prep.execute();
        prep.close();
        sharedResource.unlock();
    }

    private void writeUserPreferencesToDb(int userId) throws SQLException{
        String command = "insert into user_preferences(user_id, preferred_category)values(?,?)";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setString(2, FeedCategory.NONE.toString());
        prep.execute();
        prep.close();
    }
    
}
