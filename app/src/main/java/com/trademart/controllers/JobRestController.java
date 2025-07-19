package com.trademart.controllers;

import java.sql.SQLException;
import java.util.ArrayList;

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
import com.trademart.user.User;
import com.trademart.user.UserController;

@RestController
public class JobRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private JobController jobController;
    private UserController userController;

    public JobRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        jobController = new JobController(sharedResource);
        userController = new UserController(sharedResource);
    }
    
    @GetMapping("/jobs/employer/{employer_id}")
    public ResponseEntity<String> fetchJobsMapping(@PathVariable("employer_id") int employerId){
        User employer = userController.getUserFromDB(employerId);
        if(employer == null){
            return notFoundResponse();
        }
        ArrayList<JobListing> jobs;
        try {
            jobs = jobController.findJobsByEmployerId(employerId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("unable to fetch job listings");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse("unable to fetch job listings");
        }
        return ResponseEntity.ok(createResponse("success", "fetched jobs")
                .put("data", jobController.jobArrayToJSON(jobs)).toString());
    }

    @GetMapping("/jobs/find/{job_id}")
    public ResponseEntity<String> getJobMapping(@PathVariable("job_id") int jobId){
        JobListing job = null;
        try {
            job = jobController.findJobById(jobId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("an internal server error occured");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse("an internal server error occured");
        }
        if(job == null){
            return notFoundResponse();
        }
        return ResponseEntity.ok(createResponse("success", "Job Listing found!")
                .put("data", job.parseJson()).toString());
    }

    @PostMapping("/jobs/create")
    public ResponseEntity<String> createJobMapping(@RequestBody String body){
        JSONObject json = null;
        User employer = null;
        try {
            json = new JSONObject(new JSONTokener(body));
            employer = userController.getUserFromDB(json.getInt("employer_id"));
            if(employer == null){
                return notFoundResponse();
            }
        } catch (JSONException e){
            return badRequestResponse("request was badly formatted");
        }
        JobListing job = jobController.createJobFromJSON(json);
        try {
            jobController.writeJobToDb(job);
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse("unable to post Job Listing!");
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("unable to post Job Listing!");
        }
        JSONObject entity = new JSONObject();
        return ResponseEntity.ok(createResponse("success", "Job Listing Posted!")
                .put("data", entity).toString());
    }

}
