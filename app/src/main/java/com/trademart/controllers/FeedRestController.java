package com.trademart.controllers;

import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.feed.FeedItem;
import com.trademart.feed.FeedType;
import com.trademart.job.JobController;
import com.trademart.job.JobListing;
import com.trademart.media.MediaController;
import com.trademart.post.Post;
import com.trademart.post.PostController;
import com.trademart.service.Service;
import com.trademart.service.ServiceController;
import com.trademart.user.User;
import com.trademart.user.UserController;

@RestController
public class FeedRestController extends RestControllerBase {

    public static final int FEED_SEND_COUNT = 10;

    private SharedResource sharedResource;
    private UserController userController;
    private PostController postController;
    private ServiceController serviceController;
    private JobController jobController;

    private MediaController mediaController;

    public FeedRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        this.serviceController = new ServiceController(sharedResource);
        this.userController = new UserController(sharedResource);
        this.postController = new PostController(sharedResource);
        this.jobController = new JobController(sharedResource);
        this.mediaController = new MediaController(sharedResource);
    }

    @PostMapping("/feed")
    public ResponseEntity<String> serveFeedDetailsMapping(@RequestBody String body){
        JSONObject json;
        ArrayList<FeedItem> loadedFeeds = new ArrayList<>();
        try {
            json = new JSONObject(new JSONTokener(body));
            JSONArray feeds = json.getJSONArray("loaded_feeds");
            for (int i = 0; i < feeds.length(); i++) {
                JSONObject feed = feeds.getJSONObject(i);
                loadedFeeds.add(new FeedItem.Builder()
                        .setId(feed.getInt("id"))
                        // .setTitle(feed.getString("title"))
                        // .setUsername(feed.getString("username"))
                        // .setOwnerId(feed.getInt("id"))
                        .setType(FeedType.parse(feed.getString("type")))
                        // .setLikes(feed.getInt("likes"))
                        .build());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return badRequestResponse("received a bad request");
        }
        JSONObject data = new JSONObject();
        try {
            ArrayList<FeedItem> feeds = generateFeeds(loadedFeeds);
            for (FeedItem feedItem : feeds) {
                data.append("feeds", feedItem.parseJSON());
            }
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "fetched feed")
                .put("data", data).toString());
    }

    private ArrayList<FeedItem> generateFeeds(ArrayList<FeedItem> loadedFeeds) throws InterruptedException, SQLException{
        ArrayList<FeedItem> feeds = new ArrayList<>();
        ArrayList<Post> posts = postController.getAllPostsFromDB();
        ArrayList<Service> services = serviceController.getAllServices();
        ArrayList<JobListing> jobs = jobController.getAllJobListingsFromDB();

        for (int i = 0; i < FEED_SEND_COUNT; i++) {
            switch (decideFeedType()) {
                case JOB_LISTING:
                    feeds.add(decideJobListingFeed(jobs, loadedFeeds));
                    break;
                case POST:
                    feeds.add(decidePostFeed(posts, loadedFeeds));
                    break;
                case SERVICE:
                    feeds.add(decideServiceFeed(services, loadedFeeds));
                    break;
                default:
                    break;
            }
        }
        return feeds;
    }

    private FeedItem decideServiceFeed(ArrayList<Service> services, ArrayList<FeedItem> loadedFeeds) throws SQLException{
        Service selected;
        do{
            int rand = randomNumber(0, services.size());
            selected = services.get(rand);
        } while(isLoaded(selected.getServiceId(), FeedType.SERVICE, loadedFeeds));
        User user = userController.getUserFromDB(selected.getOwnerId());
        return new FeedItem.Builder()
            .setId(selected.getServiceId())
            .setTitle(selected.getServiceTitle())
            .setUsername(user.getUsername())
            .setOwnerId(user.getId())
            .setType(FeedType.SERVICE)
            .setMediaIds(serviceController.getServiceMediaIDs(selected.getServiceId()))
            .build();
    }

    private FeedItem decideJobListingFeed(ArrayList<JobListing> jobs, ArrayList<FeedItem> loadedFeeds) throws SQLException{
        JobListing selected;
        do{
            int rand = randomNumber(0, jobs.size());
            selected = jobs.get(rand);
        } while(isLoaded(selected.getId(), FeedType.JOB_LISTING, loadedFeeds));
        User user = userController.getUserFromDB(selected.getEmployerId());
        return new FeedItem.Builder()
            .setId(selected.getId())
            .setTitle(selected.getTitle())
            .setUsername(user.getUsername())
            .setOwnerId(user.getId())
            .setType(FeedType.JOB_LISTING)
            .setMediaIds(jobController.getJobMediaIDs(selected.getId()))
            .build();
    }

    private FeedItem decidePostFeed(ArrayList<Post> posts, ArrayList<FeedItem> loadedFeeds) throws SQLException{
        Post selected;
        do{
            int rand = randomNumber(0, posts.size());
            selected = posts.get(rand);
        } while(isLoaded(selected.getPostId(), FeedType.POST, loadedFeeds));
        User user = userController.getUserFromDB(selected.getUserId());
        return new FeedItem.Builder()
            .setId(selected.getPostId())
            .setTitle(selected.getTitle())
            .setUsername(user.getUsername())
            .setOwnerId(user.getId())
            .setType(FeedType.POST)
            .setMediaIds(postController.getPostMediaIDs(selected.getPostId()))
            .build();
    }

    private boolean isLoaded(int id, FeedType type, ArrayList<FeedItem> feeds){
        for (FeedItem feedItem : feeds)
            if(feedItem.getType() == type && feedItem.getId() == id)
                return true;
        return false;
    }

    private int randomNumber(int start, int end){
        return (int) ((Math.random() * end) + start);
    }

    private FeedType decideFeedType(){
        switch (randomNumber(1, 3)) {
            case 1:
                return FeedType.POST;
            case 2:
                return FeedType.SERVICE;
            case 3:
                return FeedType.JOB_LISTING;
            default:
                return FeedType.SERVICE;
        }
    }

    
}
