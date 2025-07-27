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
import com.trademart.feed.FeedCategory;
import com.trademart.feed.FeedItem;
import com.trademart.feed.FeedType;
import com.trademart.job.JobController;
import com.trademart.job.JobListing;
import com.trademart.media.MediaController;
import com.trademart.media.MediaType;
import com.trademart.post.Post;
import com.trademart.post.PostController;
import com.trademart.service.Service;
import com.trademart.service.ServiceController;
import com.trademart.user.User;
import com.trademart.user.UserController;
import com.trademart.user.UserPreferences;

@RestController
public class FeedRestController extends RestControllerBase {

    public static final int FEED_SEND_COUNT = 10;
    public static final int ALGO_PREFERRED_CATEGORY_MODIFIER = 5;
    public static final int HESITATION_LIMIT = 10;

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
        int userId = -1;
        try {
            json = new JSONObject(new JSONTokener(body));
            userId = json.getInt("user_id");
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
            ArrayList<FeedItem> feeds = generateFeeds(loadedFeeds, userId);
            for (FeedItem feedItem : feeds) {
                data.append("feeds", feedItem.parseJSON()
                        .put("user_liked", userHasLiked(userId, feedItem)));
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

    private boolean userHasLiked(int userId, FeedItem feed) throws SQLException, InterruptedException{
        switch (feed.getType()) {
            case JOB_LISTING:
                return jobController.userHasLiked(userId, feed.getId());
            case POST:
                return postController.userHasLiked(userId, feed.getId());
            case SERVICE:
                return serviceController.userHasLiked(userId, feed.getId());
            default:
                return false;
        }
    }

    @PostMapping("/feed/like")
    public ResponseEntity<String> likeFeedMapping(@RequestBody String body){
        JSONObject json;
        FeedItem feedItem;
        int userId;
        try {
            json = new JSONObject(new JSONTokener(body));
            userId = json.getInt("user_id");
            JSONObject feedJson = json.getJSONObject("data");
            feedItem = new FeedItem.Builder()
                .setId(feedJson.getInt("id"))
                // .setOwnerId(feedJson.getInt("owner_id"))
                .setType(FeedType.parse(feedJson.getString("type")))
                .build();
        } catch (JSONException e) {
            e.printStackTrace();
            return badRequestResponse("received a bad request");
        }
        FeedItem updated = null;
        boolean userLiked = false;
        try {
            switch (feedItem.getType()) {
                case JOB_LISTING:
                    updated = likeJobListing(userId, feedItem.getId());
                    userLiked = jobController.userHasLiked(userId, feedItem.getId());
                    break;
                case POST:
                    updated = likePost(userId, feedItem.getId());
                    userLiked = postController.userHasLiked(userId, feedItem.getId());
                    break;
                case SERVICE:
                    updated = likeService(userId, feedItem.getId());
                    userLiked = serviceController.userHasLiked(userId, feedItem.getId());
                    break;
                default:
                    break;
            }
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "feed liked")
                .put("data", new JSONObject()
                    .put("feed_updated", updated.parseJSON()
                        .put("user_liked", userLiked)))
                .toString());
    }

    private FeedItem likePost(int userId, int postId) throws InterruptedException, SQLException{
        User user = userController.findUserById(userId);
        Post post = postController.findPostByID(postId);
        boolean isLiking = !postController.userHasLiked(userId, postId);
        postController.likePost(post, userId, isLiking);
        return new FeedItem.Builder()
            .setId(post.getPostId())
            .setUsername(user.getUsername())
            .setOwnerId(user.getId())
            .setTitle(post.getTitle())
            .setLikes(post.getLikes() + (isLiking ? 1 : -1))
            .setType(FeedType.SERVICE)
            .build();
    }

    private FeedItem likeService(int userId, int serviceId) throws InterruptedException, SQLException{
        User user = userController.findUserById(userId);
        Service service = serviceController.findServiceByID(serviceId);
        boolean isLiking = !serviceController.userHasLiked(userId, serviceId);
        serviceController.likeService(service, userId, isLiking);
        return new FeedItem.Builder()
            .setId(service.getServiceId())
            .setUsername(user.getUsername())
            .setOwnerId(user.getId())
            .setTitle(service.getServiceTitle())
            .setLikes(service.getLikes() + (isLiking ? 1 : -1))
            .setType(FeedType.SERVICE)
            .build();
    }

    private FeedItem likeJobListing(int userId, int jobId) throws InterruptedException, SQLException{
        User user = userController.findUserById(userId);
        JobListing job = jobController.findJobByID(jobId);
        boolean isLiking = !jobController.userHasLiked(userId, jobId);
        jobController.likeJob(job, userId, isLiking);
        return new FeedItem.Builder()
            .setId(job.getId())
            .setUsername(user.getUsername())
            .setOwnerId(user.getId())
            .setTitle(job.getTitle())
            .setLikes(job.getLikes() + (isLiking ? 1 : -1))
            .setType(FeedType.JOB_LISTING)
            .build();
    }

    private ArrayList<FeedItem> generateFeeds(ArrayList<FeedItem> loadedFeeds, int userId) throws InterruptedException, SQLException{
        ArrayList<FeedItem> feeds = new ArrayList<>();
        ArrayList<Post> posts = postController.getAllPostsFromDB();
        ArrayList<Service> services = serviceController.getAllServices();
        ArrayList<JobListing> jobs = jobController.getAllJobListingsFromDB();
        for (int i = 0; i < FEED_SEND_COUNT; i++) {
            FeedItem item = null;
            FeedCategory categoryFilter = decideCategory(userId);
            do {
                switch (decideFeedType()) {
                    case JOB_LISTING:
                        item = decideJobListingFeed(jobs, loadedFeeds, categoryFilter);
                        break;
                    case POST:
                        item = decidePostFeed(posts, loadedFeeds, categoryFilter);
                        break;
                    case SERVICE:
                        item = decideServiceFeed(services, loadedFeeds, categoryFilter);
                        break;
                    default:
                        break;
                }
            } while(hasId(feeds, item.getId()));
            feeds.add(item);
        }
        return feeds;
    }

    private boolean hasId(ArrayList<FeedItem> feeds, int id){
        for (FeedItem feedItem : feeds)
            if(feedItem.getId() == id)
                return true;
        return false;
    }

    private FeedCategory decideCategory(int userId) throws InterruptedException, SQLException{
        UserPreferences pref = userController.getUserPreferences(userId);
        FeedCategory preferredCategory = pref.getPreferredCategory();
        if(preferredCategory == FeedCategory.NONE){
            return FeedCategory.NONE;
        }
        ArrayList<FeedCategory> categories = new ArrayList<>();
        for (int i = 0; i < ALGO_PREFERRED_CATEGORY_MODIFIER; i++) {
            categories.add(preferredCategory);
        }
        for(FeedCategory cat : FeedCategory.values()){
            categories.add(cat);
        }
        int select = randomNumber(0, categories.size());
        return categories.get(select);
    }

    private FeedItem decideServiceFeed(ArrayList<Service> services, ArrayList<FeedItem> loadedFeeds, FeedCategory categoryFilter) throws SQLException, InterruptedException{
        Service selected;
        ArrayList<FeedCategory> categories;
        int hesitation = 0;
        do{
            int rand = randomNumber(0, services.size());
            selected = services.get(rand);
            categories = serviceController.getCategoriesById(selected.getServiceId());
            hesitation++;
            if(hesitation == HESITATION_LIMIT){
                break;
            }
        } while (isLoaded(selected.getServiceId(), FeedType.SERVICE, loadedFeeds) ||
                !categoryFilterPasses(categories, categoryFilter));
        User user = userController.findUserById(selected.getOwnerId());
        ArrayList<Integer> mediaIds = serviceController.getServiceMediaIDs(selected.getServiceId());
        return new FeedItem.Builder()
            .setId(selected.getServiceId())
            .setTitle(selected.getServiceTitle())
            .setUsername(user.getUsername())
            .setLikes(selected.getLikes())
            .setOwnerId(user.getId())
            .setType(FeedType.SERVICE)
            .setMediaIds(mediaIds)
            .setMediaTypes(getMediaTypes(mediaIds))
            .build();
    }

    private ArrayList<MediaType> getMediaTypes(ArrayList<Integer> mediaIds) throws SQLException, InterruptedException{
        ArrayList<MediaType> types = new ArrayList<>();
        for (int id : mediaIds) {
            types.add(mediaController.getMediaTypeFromMediaId(id));
        }
        return types;
    }

    private FeedItem decideJobListingFeed(ArrayList<JobListing> jobs, ArrayList<FeedItem> loadedFeeds, FeedCategory categoryFilter) throws SQLException, InterruptedException{
        JobListing selected;
        ArrayList<FeedCategory> categories;
        int hesitation = 0;
        do{
            int rand = randomNumber(0, jobs.size());
            selected = jobs.get(rand);
            categories = jobController.getCategoriesById(selected.getId());
            hesitation++;
            if(hesitation == HESITATION_LIMIT){
                break;
            }
        } while(isLoaded(selected.getId(), FeedType.JOB_LISTING, loadedFeeds) ||
                !categoryFilterPasses(categories, categoryFilter));
        User user = userController.findUserById(selected.getEmployerId());
        ArrayList<Integer> mediaIds = jobController.getJobMediaIDs(selected.getId());
        return new FeedItem.Builder()
            .setId(selected.getId())
            .setTitle(selected.getTitle())
            .setUsername(user.getUsername())
            .setOwnerId(user.getId())
            .setLikes(selected.getLikes())
            .setType(FeedType.JOB_LISTING)
            .setMediaIds(mediaIds)
            .setMediaTypes(getMediaTypes(mediaIds))
            .build();
    }

    private FeedItem decidePostFeed(ArrayList<Post> posts, ArrayList<FeedItem> loadedFeeds, FeedCategory categoryFilter) throws SQLException, InterruptedException{
        Post selected;
        ArrayList<FeedCategory> categories;
        int hesitation = 0;
        do{
            int rand = randomNumber(0, posts.size());
            selected = posts.get(rand);
            categories = jobController.getCategoriesById(selected.getPostId());
            hesitation++;
            if(hesitation == HESITATION_LIMIT){
                break;
            }
        } while(isLoaded(selected.getPostId(), FeedType.POST, loadedFeeds) ||
                !categoryFilterPasses(categories, categoryFilter));
        ArrayList<Integer> mediaIds = postController.getPostMediaIDs(selected.getPostId());
        User user = userController.findUserById(selected.getUserId());
        return new FeedItem.Builder()
            .setId(selected.getPostId())
            .setTitle(selected.getTitle())
            .setUsername(user.getUsername())
            .setOwnerId(user.getId())
            .setLikes(selected.getLikes())
            .setType(FeedType.POST)
            .setMediaIds(mediaIds)
            .setMediaTypes(getMediaTypes(mediaIds))
            .build();
    }

    private boolean categoryFilterPasses(ArrayList<FeedCategory> categories, FeedCategory categoryFilter){
        if(categoryFilter == FeedCategory.NONE || categories.contains(categoryFilter)){
            return true;
        }
        return false;
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
