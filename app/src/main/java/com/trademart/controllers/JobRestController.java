package com.trademart.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.job.JobController;
import com.trademart.job.JobListing;
import com.trademart.media.MediaController;
import com.trademart.user.User;
import com.trademart.user.UserController;
import com.trademart.util.FileUtil;

@RestController
public class JobRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private JobController jobController;
    private UserController userController;
    private MediaController mediaController;

    public JobRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        jobController = new JobController(sharedResource);
        userController = new UserController(sharedResource);
        mediaController = new MediaController(sharedResource);
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
            job = jobController.findJobByID(jobId);
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

    @GetMapping("/jobs/{job_id}/media")
    public ResponseEntity<String> fetchJobMediaIDsMapping(@PathVariable("job_id") int jobId){
        JSONObject json = new JSONObject();
        try {
            ArrayList<Integer> ids = jobController.getJobMediaIDs(jobId);
            json.put("job_ids", ids);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(json.toString());
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
            e.printStackTrace();
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
        JSONObject entity = job.parseJson();
        return ResponseEntity.ok(createResponse("success", "Job Listing Posted!")
                .put("data", entity).toString());
    }

    @PostMapping("/jobs/create/{job_id}/media")
    public ResponseEntity<String> uploadJobMediaMapping(@PathVariable("job_id") int jobId,
            @RequestHeader("Content-Disposition") String dispositionStr, @RequestBody byte[] data) {

        JobListing job;
        try {
            job = jobController.findJobByID(jobId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("an internal server error occured");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse("an internal server error occured");
        }
        if(job == null){
            return ResponseEntity.notFound().build();
        }

        String filename = ContentDisposition.parse(dispositionStr).getFilename();
        int mediaId = -1;
        try {
            File file = mediaController.writeFile(filename, data);
            String filepath = file.getAbsolutePath();
            String ext = FileUtil.getExtension(file.getName());
            if(ext.equals("mp4")){
                filepath = FileUtil.removeExtension(filepath).concat(".m3u8");
            }
            mediaId = mediaController.insertJobMediaToDB(filepath, job.getEmployerId(), jobId);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return internalServerErrorResponse("unable to upload media");
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("unable to upload media");
        }

        return ResponseEntity.status(HttpStatus.CREATED).header("Location",
                new StringBuilder()
                .append("/media/")
                .append(mediaId)
                .toString())
            .body(createResponse("success", "image uploaded successfully").toString());
    }
}
