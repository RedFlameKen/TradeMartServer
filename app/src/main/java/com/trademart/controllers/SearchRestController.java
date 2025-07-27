package com.trademart.controllers;

import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.job.JobController;
import com.trademart.job.JobListing;
import com.trademart.post.Post;
import com.trademart.post.PostController;
import com.trademart.search.MediaSearchItem;
import com.trademart.search.SearchController;
import com.trademart.search.SearchItem;
import com.trademart.service.Service;
import com.trademart.service.ServiceController;
import com.trademart.user.User;
import com.trademart.user.UserController;

@RestController
public class SearchRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private UserController userController;
    private ServiceController serviceController;
    private JobController jobController;
    private PostController postController;

    public SearchRestController(SharedResource sharedResource) {
        this.sharedResource = sharedResource;
        userController = new UserController(sharedResource);
        serviceController = new ServiceController(sharedResource);
        jobController = new JobController(sharedResource);
        postController = new PostController(sharedResource);
    }

    @GetMapping("/search/user")
    public ResponseEntity<String> searchUsersMapping(@RequestParam(required = false, name = "query") String query){
        if(query == null || query.equals("")){
            return ResponseEntity.ok(createResponse("success", "no search")
                    .put("data", new JSONObject()).toString());
        }
        ArrayList<User> users;
        try {
             users = userController.getAllUsersFromDB();
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        SearchController<SearchItem> searchController = new SearchController<>();
        ArrayList<SearchItem> items = searchController.filter(query, userSearchItems(users));
        JSONObject responseJson = searchController.searchItemsToJSON(items);
        return ResponseEntity.ok(createResponse("success", "fetched results")
                .put("data", responseJson).toString());
    }

    @GetMapping("/search/service")
    public ResponseEntity<String> searchServiceMapping(@RequestParam(required = false, name = "query") String query){
        SearchController<MediaSearchItem> searchController = new SearchController<>();
        if(query == null || query.equals("")){
            return ResponseEntity.ok(createResponse("success", "no search")
                    .put("data", new JSONObject()).toString());
        }
        ArrayList<Service> services;
        try {
             services = serviceController.getAllServices();
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }

        ArrayList<MediaSearchItem> items;
        try {
            items = searchController.filter(query, serviceSearchItems(services));
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        JSONObject responseJson = searchController.searchItemsToJSON(items);
        return ResponseEntity.ok(createResponse("success", "fetched results")
                .put("data", responseJson).toString());
    }

    @GetMapping("/search/job")
    public ResponseEntity<String> searchJobMapping(@RequestParam(required = false, name = "query") String query){
        SearchController<MediaSearchItem> searchController = new SearchController<>();
        if(query == null || query.equals("")){
            return ResponseEntity.ok(createResponse("success", "no search")
                    .put("data", new JSONObject()).toString());
        }
        ArrayList<JobListing> jobs;
        try {
             jobs = jobController.getAllJobListingsFromDB();
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }

        ArrayList<MediaSearchItem> items;
        try {
            items = searchController.filter(query, jobSearchItems(jobs));
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        JSONObject responseJson = searchController.searchItemsToJSON(items);
        return ResponseEntity.ok(createResponse("success", "fetched results")
                .put("data", responseJson).toString());
    }

    @GetMapping("/search/post")
    public ResponseEntity<String> searchPostMapping(@RequestParam(required = false, name = "query") String query){
        SearchController<MediaSearchItem> searchController = new SearchController<>();
        if(query == null || query.equals("")){
            return ResponseEntity.ok(createResponse("success", "no search")
                    .put("data", new JSONObject()).toString());
        }
        ArrayList<Post> posts;
        try {
             posts = postController.getAllPostsFromDB();
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }

        ArrayList<MediaSearchItem> items;
        try {
            items = searchController.filter(query, postSearchItems(posts));
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        JSONObject responseJson = searchController.searchItemsToJSON(items);
        return ResponseEntity.ok(createResponse("success", "fetched results")
                .put("data", responseJson).toString());
    }

    private ArrayList<SearchItem> userSearchItems(ArrayList<User> users){
        ArrayList<SearchItem> items = new ArrayList<>();
        for (User user : users) {
            items.add(new SearchItem(user));
        }
        return items;
    }

    private ArrayList<MediaSearchItem> serviceSearchItems(ArrayList<Service> services) throws SQLException, InterruptedException{
        ArrayList<MediaSearchItem> items = new ArrayList<>();
        for (Service service : services) {
            ArrayList<Integer> mediaIds = serviceController.getServiceMediaIDs(service.getServiceId());
            User user = userController.findUserById(service.getOwnerId());
            items.add(new MediaSearchItem(service, mediaIds, user));
        }
        return items;
    }

    private ArrayList<MediaSearchItem> jobSearchItems(ArrayList<JobListing> jobs) throws SQLException, InterruptedException{
        ArrayList<MediaSearchItem> items = new ArrayList<>();
        for (JobListing job : jobs) {
            ArrayList<Integer> mediaIds = jobController.getJobMediaIDs(job.getId());
            User user = userController.findUserById(job.getEmployerId());
            items.add(new MediaSearchItem(job, mediaIds, user));
        }
        return items;
    }

    private ArrayList<MediaSearchItem> postSearchItems(ArrayList<Post> posts) throws SQLException, InterruptedException{
        ArrayList<MediaSearchItem> items = new ArrayList<>();
        for (Post post : posts) {
            ArrayList<Integer> mediaIds = postController.getPostMediaIDs(post.getPostId());
            User user = userController.findUserById(post.getUserId());
            items.add(new MediaSearchItem(post, mediaIds, user));
        }
        return items;
    }

}
