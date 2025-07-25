package com.trademart.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.json.JSONArray;
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
import com.trademart.feed.FeedCategory;
import com.trademart.job.JobController;
import com.trademart.job.JobListing;
import com.trademart.job.JobTransaction;
import com.trademart.media.MediaController;
import com.trademart.user.User;
import com.trademart.user.UserController;
import com.trademart.util.FileUtil;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

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
        User employer;
        try {
            employer = userController.getUserFromDB(employerId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
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
        ArrayList<FeedCategory> categories;
        try {
            job = jobController.findJobByID(jobId);
            categories = jobController.getCategoriesById(jobId);
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
                .put("data", job.parseJson()
                    .put("categories", categories)).toString());
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
        ArrayList<FeedCategory> categories = new ArrayList<>();
        try {
            json = new JSONObject(new JSONTokener(body));
            Logger.log("received /jobs/create json: " + json.toString(), LogLevel.INFO);
            employer = userController.getUserFromDB(json.getInt("employer_id"));
            if(employer == null){
                return notFoundResponse();
            }
            JSONArray categoriesJson = json.getJSONArray("categories");
            for (int i = 0; i < categoriesJson.length(); i++) {
                categories.add(FeedCategory.valueOf(categoriesJson.getString(i)));
            }
        } catch (JSONException e){
            e.printStackTrace();
            return badRequestResponse("request was badly formatted");
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        JobListing job = jobController.createJobFromJSON(json);
        try {
            jobController.writeJobToDb(job, categories);
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

    @PostMapping("/jobs/{job_id}/liked")
    public ResponseEntity<String> checkIfUserLikedPost(@PathVariable("job_id") int jobId, @RequestBody String body){
        int userId = -1;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            userId = json.getInt("user_id");
        } catch (JSONException e) {
            return badRequestResponse("client sent a bad request");
        }
        boolean hasLiked = false;
        try {
            hasLiked = jobController.userHasLiked(userId, jobId);
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "request sent")
                .put("data", new JSONObject()
                    .put("has_liked", hasLiked)).toString());
    }

    @PostMapping("/jobs/{job_id}/apply")
    public ResponseEntity<String> applyForJobMapping(@PathVariable("job_id") int jobId, @RequestBody String body){
        int employeeId;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            employeeId = json.getInt("employee_id");
        } catch (JSONException e){
            e.printStackTrace();
            return badRequestResponse("invalid request");
        }

        JobTransaction transaction;
        try {
            JobListing job = jobController.findJobByID(jobId);
            transaction = new JobTransaction.Builder()
                .setJobId(jobId)
                .setEmployeeId(employeeId)
                .setEmployerId(job.getEmployerId())
                .build();
            jobController.writeJobTransactionToDB(transaction);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }

        return ResponseEntity.ok(createResponse("success", "appliation sent!")
                .put("data", transaction.parseJSON()).toString());
    }

    @PostMapping("/jobs/hire")
    public ResponseEntity<String> acceptApplicationMapping(@RequestBody String body){
        int transactionId;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            transactionId = json.getInt("transaction_id");
        } catch (JSONException e){
            e.printStackTrace();
            return badRequestResponse("invalid request");
        }

        JobTransaction transaction;
        try {
            transaction = jobController.findJobTransactionById(transactionId);
            if(transaction == null){
                return notFoundResponse();
            }
            jobController.startJob(transaction);
            transaction = jobController.findJobTransactionById(transactionId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "application accepted")
                .put("data", transaction.parseJSON()).toString());
    }

    @PostMapping("/jobs/complete")
    public ResponseEntity<String> completeJobMapping(@RequestBody String body){
        int transactionId;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            transactionId = json.getInt("transaction_id");
        } catch (JSONException e){
            e.printStackTrace();
            return badRequestResponse("invalid request");
        }
        JobTransaction transaction;
        try {
            transaction = jobController.findJobTransactionById(transactionId);
            if(transaction == null){
                return notFoundResponse();
            }
            jobController.markJobTransactionCompleted(transactionId);
            transaction = jobController.findJobTransactionById(transactionId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        // TODO: something about the payment messages
        return ResponseEntity.ok(createResponse("success", "job completed!")
                .put("data", transaction.parseJSON()).toString());
    }

    @PostMapping("/jobs/applications")
    public ResponseEntity<String> fetchApplicationsMapping(@RequestBody String body){
        int employeeId;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            employeeId = json.getInt("employeeId");
        } catch (JSONException e){
            e.printStackTrace();
            return badRequestResponse("invalid request");
        }
        JSONObject response = new JSONObject();
        try {
            JSONArray appsJson = new JSONArray();
            ArrayList<JobTransaction> applications = jobController.getAllJobTransactionsByEmployeeId(employeeId);
            for (JobTransaction application : applications) {
                appsJson.put(application.parseJSON());
            }
            response.put("applications", appsJson);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "fetched applicatiosn")
                .put("data", response).toString());
    }


    @PostMapping("/jobs/hirings")
    public ResponseEntity<String> fetchReceivedApplicationsMapping(@RequestBody String body){
        int employerId;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            employerId = json.getInt("employer_id");
        } catch (JSONException e){
            e.printStackTrace();
            return badRequestResponse("invalid request");
        }
        JSONObject response = new JSONObject();
        try {
            JSONArray appsJson = new JSONArray();
            ArrayList<JobTransaction> hirings = jobController.getAllJobTransactionsByEmployerId(employerId);
            for (JobTransaction hiring : hirings) {
                appsJson.put(hiring.parseJSON());
            }
            response.put("hirings", appsJson);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "fetched hirings")
                .put("data", response).toString());
    }

    @GetMapping("/jobs/user/{user_id}/completedjobs")
    public ResponseEntity<String> fetchUserCompletedJobs(@PathVariable("user_id") int userId){
        JSONObject data = new JSONObject();
        try {
             ArrayList<JobTransaction> completedJobs = jobController.getUserCompletedJobs(userId);
             JSONArray json = new JSONArray();
             for (JobTransaction jobTransaction : completedJobs) {
                 json.put(jobTransaction.parseJSON());
             }
             data.put("completed_jobs", json);

        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "fetched completed jobs")
                .put("data", data).toString());
    }

}
