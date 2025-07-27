package com.trademart.controllers;

import static com.trademart.util.Logger.LogLevel.INFO;

import java.sql.SQLException;
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
import com.trademart.job.JobController;
import com.trademart.job.JobListing;
import com.trademart.job.JobTransaction;
import com.trademart.rating.JobRating;
import com.trademart.rating.RatingController;
import com.trademart.user.User;
import com.trademart.user.UserController;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

@RestController
public class RatingRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private RatingController ratingController;
    private JobController jobController;
    private UserController userController;

    public RatingRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        ratingController = new RatingController(sharedResource);
        jobController = new JobController(sharedResource);
        userController = new UserController(sharedResource);
    }

    @PostMapping("/rate/jobs")
    public ResponseEntity<String> rateJobMapping(@RequestBody String body){
        int jobTransactionId;
        int raterId;
        double rate;
        String comment;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            jobTransactionId = json.getInt("transaction_id");
            rate = json.getDouble("rating");
            raterId = json.getInt("rater_id");
            // comment = json.getString("comment");
        } catch (JSONException e) {
            e.printStackTrace();
            return badRequestResponse("request was bady formatted");
        }

        JobRating jobRating;
        try {
            JobTransaction transaction = jobController.findJobTransactionById(jobTransactionId);
            if(transaction == null){
                Logger.log("received a request to rate a non existent transaction", INFO);
                return badRequestResponse("transaction to be rated does not exist");
            }
            jobRating = ratingController.findJobRatingByTransactionId(jobTransactionId);
            if(jobRating != null){
                Logger.log("A transaction that was already rated received a rating request", INFO);
                return badRequestResponse("This transaction has already been rated");
            }
            jobRating = new JobRating.Builder()
                .setRate(rate)
                .setJobTransactionId(jobTransactionId)
                // .setComment(comment)
                .setRaterId(raterId)
                .build();
            ratingController.writeJobRatingToDB(jobRating);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "rating sent!")
                .put("data", jobRating.parseJSON()).toString());
    }

    @GetMapping("/rate/rated/transaction/{transaction_id}")
    public ResponseEntity<String> checkResponseRated(@PathVariable("transaction_id") int transactionId){
        JSONObject data = new JSONObject();
        try {
            boolean rated = false;
            JobRating rating = ratingController.findJobRatingByTransactionId(transactionId);
            if(rating != null){
                Logger.log("already rated", INFO);
                rated = true;
            }
            data.put("rated", rated);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(data.toString());
    }

    @GetMapping("/rate/user/{user_id}/jobs")
    public ResponseEntity<String> getAllUserJobRatingsMapping(@PathVariable("user_id") int userId){
        JSONObject data = new JSONObject();
        try {
            ArrayList<JobRating> ratings;
            User user = userController.findUserById(userId);
            if(user == null){
                Logger.log("/rate/user/"+ userId + "/jobs : no user found", LogLevel.WARNING);
                return notFoundResponse();
            }
            ratings = ratingController.findJobRatingByRaterId(userId);
            JSONArray arr = new JSONArray();
            for (JobRating rating : ratings) {
                arr.put(rating.parseJSON());
            }
            data.put("ratings", arr);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "fetched rating!")
                .put("data", data).toString());
    }

    @GetMapping("/rate/user/{user_id}/jobs/rating")
    public ResponseEntity<String> getUserRatingMapping(@PathVariable("user_id") int userId){
        JSONObject data = new JSONObject();
        try {
            ArrayList<JobRating> ratings;
            User user = userController.findUserById(userId);
            if(user == null){
                Logger.log("/rate/user/"+ userId + "/jobs : no job found", LogLevel.WARNING);
                return notFoundResponse();
            }
            ratings = ratingController.findJobRatingByEmployeeId(userId);
            double ratingValue = 0;
            int count = 0;
            for (JobRating rating : ratings) {
                ratingValue+= rating.getRate();
                count++;
            }
            double finalValue = 0;;
            if(count > 0){
                finalValue = ratingValue/count;
            }
            Logger.log("ratingValue: " + ratingValue, INFO);
            Logger.log("rating: " + finalValue, INFO);
            data.put("rating", finalValue);
            data.put("rating_count", count);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "fetched user average rating")
                .put("data", data).toString());
    }

    @GetMapping("/rate/jobs/{job_id}")
    public ResponseEntity<String> getAllJobRatingsMapping(@PathVariable("job_id") int jobId){
        JSONObject data = new JSONObject();
        try {
            ArrayList<JobRating> ratings = new ArrayList<>();
            JobListing jobListing = jobController.findJobByID(jobId);
            if(jobListing == null){
                Logger.log("/rate/jobs" + jobId + ": no job found", LogLevel.WARNING);
                return notFoundResponse();
            }
            ArrayList<JobTransaction> transactions = jobController.getAllJobTransactionsByJobId(jobId);
            for (JobTransaction transaction : transactions) {
                JobRating rating = ratingController.findJobRatingByTransactionId(transaction.getTransactionId());
                ratings.add(rating);
            }
            JSONArray arr = new JSONArray();
            for (JobRating rating : ratings) {
                arr.put(rating.parseJSON());
            }
            data.put("ratings", arr);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "rating sent!")
                .put("data", data).toString());
    }


}
