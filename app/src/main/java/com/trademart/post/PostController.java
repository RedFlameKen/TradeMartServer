package com.trademart.post;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.db.IDGenerator;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

public class PostController {

    private SharedResource sharedResource;
    private DatabaseController dbController;

    public PostController(SharedResource sharedResource) {
        this.sharedResource = sharedResource;
        dbController = sharedResource.getDatabaseController();
    }

    public void insertPostToDB(Post post) throws SQLException{
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DatabaseController db = sharedResource.getDatabaseController();
        PreparedStatement prep = db.prepareStatement("insert into posts (post_id, user_id, title, description, post_category, likes) values (?, ?, ?, ?, ?, ?)");
        prep.setInt(1, post.getPostId());
        prep.setInt(2, post.getUserId());
        prep.setString(3, post.getTitle());
        prep.setString(4, post.getDescription());
        prep.setString(5, post.getPostCategory().toString());
        prep.setInt(6, post.getLikes());

        prep.execute();

        sharedResource.unlock();
    }
    
    public ArrayList<Integer> getPostMediaIDs(int postId) throws SQLException{
        ArrayList<Integer> ids = new ArrayList<>();
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DatabaseController db = sharedResource.getDatabaseController();
        String command = "select post_media.media_id from post_media join media on media.media_id = post_media.media_id where post_media.post_id=? order by date_uploaded";
        PreparedStatement prep = db.prepareStatement(command);
        prep.setInt(1, postId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            ids.add(rs.getInt("media_id"));
        }
        rs.close();
        prep.close();
        sharedResource.unlock();
        return ids;
    }

    public int generatePostID(){
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "posts", "post_id");
        sharedResource.unlock();
        return id;
    }

    public void likePost(Post post, int likerId) throws InterruptedException, SQLException{
        if(userHasLiked(likerId, post.getPostId())){
            return;
        }
        String command = "update posts set likes=? where post_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, post.getLikes()+1);
        prep.setInt(2, post.getPostId());
        prep.execute();
        sharedResource.unlock();
        registerUserLike(post.getPostId(), likerId);
    }

    public void registerUserLike(int postId, int userId) throws SQLException, InterruptedException{
        String command = "insert into post_likes(user_id, post_id)values(?,?)";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setInt(2, postId);
        prep.execute();
        sharedResource.unlock();

    }

    public boolean userHasLiked(int userId, int postId) throws SQLException, InterruptedException{
        String command = "select * from post_likes where user_id=? and post_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setInt(2, postId);
        ResultSet rs = prep.executeQuery();
        boolean result = false;
        if(rs.next()){
            result = true;
        }
        sharedResource.unlock();
        return result;
    }

    public ArrayList<Integer> getLikingUsersById(int postId) throws SQLException, InterruptedException{
        ArrayList<Integer> ids = new ArrayList<>();
        String command = "select * from post_likes where post_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, postId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            ids.add(rs.getInt("user_id"));
        }
        sharedResource.unlock();
        return ids;
   }

    public ArrayList<Post> getAllPostsFromDB() throws InterruptedException, SQLException{
        ArrayList<Post> posts = new ArrayList<>();
        sharedResource.lock();
        String command = "select * from posts";
        PreparedStatement prep = dbController.prepareStatement(command);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            posts.add(new Post.PostBuilder()
                    .setPostId(rs.getInt("post_id"))
                    .setUserId(rs.getInt("user_id"))
                    .setTitle(rs.getString("title"))
                    .setDescription(rs.getString("description"))
                    .setLikes(rs.getInt("likes"))
                    .build());
        }
        sharedResource.unlock();
        return posts;
    }

    public Post findPostByID(int postId) {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Post post = null;
        try {
            PreparedStatement prep = sharedResource.getDatabaseController()
                .prepareStatement("select * from posts where post_id=?");
            prep.setInt(1, postId);
            ResultSet rs = prep.executeQuery();
            rs.next();
            post = new Post.PostBuilder()
                .setPostId(postId)
                .setUserId(rs.getInt("user_id"))
                .setTitle(rs.getString("title"))
                .setDescription(rs.getString("description"))
                .setLikes(rs.getInt("likes"))
                .build();
        } catch (SQLException e) {
            Logger.log("Unable to find a post with the id " + postId, LogLevel.WARNING);
        }

        sharedResource.unlock();
        return post;
    }

    public int getUserPostCount(int userId){
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int count = 0;
        try {
            String command = "select COUNT(*) from posts where user_id=?";
            PreparedStatement prep = sharedResource.getDatabaseController().prepareStatement(command);
            prep.setInt(1, userId);
            ResultSet rs = prep.executeQuery();
            rs.next();
            count = rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sharedResource.unlock();
        return count;
    }

    public ArrayList<Integer> getPostIDsByUserID(int userId){
        ArrayList<Integer> postIds = new ArrayList<>();
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            String command = "select post_id from posts where user_id=?";
            PreparedStatement prep = sharedResource.getDatabaseController().prepareStatement(command);
            prep.setInt(1, userId);
            ResultSet rs = prep.executeQuery();
            while(rs.next()){
                postIds.add(rs.getInt("post_id"));
            }
        } catch (SQLException e) {
        }
        sharedResource.unlock();
        return postIds;
    }

}
