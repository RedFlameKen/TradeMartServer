package com.trademart.post;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.db.IDGenerator;
import com.trademart.feed.FeedCategory;
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
        PreparedStatement prep = db.prepareStatement("insert into posts (post_id, title, description, likes, user_id, post_category) values (?,?,?,?,?,?)");
        prep.setInt(1, post.getPostId());
        prep.setString(2, post.getTitle());
        prep.setString(3, post.getDescription());
        prep.setInt(4, post.getLikes());
        prep.setInt(5, post.getUserId());
        prep.setString(6, post.getPostCategory().toString());

        prep.execute();

        sharedResource.unlock();
    }

    public ArrayList<FeedCategory> getCategoriesById(int postId) throws SQLException, InterruptedException{
        ArrayList<FeedCategory> categories = new ArrayList<>();
        String command = "select * from post_categories where post_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, postId);
        ResultSet rs = prep.executeQuery();
        while (rs.next()) {
            categories.add(FeedCategory.parse(rs.getString("category")));
        }
        prep.close();
        sharedResource.unlock();
        return categories;
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

    public void likePost(Post post, int likerId, boolean isLiking) throws InterruptedException, SQLException{
        String command = "update posts set likes=? where post_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, post.getLikes()+(isLiking ? 1 : -1));
        prep.setInt(2, post.getPostId());
        prep.execute();
        sharedResource.unlock();
        registerUserLike(post.getPostId(), likerId, isLiking);
    }

    public void registerUserLike(int postId, int userId, boolean isLiking) throws SQLException, InterruptedException{
        String command = isLiking ?
            "insert into post_likes(user_id, post_id)values(?,?)" :
            "delete from post_likes where user_id=? and post_id=?";
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
