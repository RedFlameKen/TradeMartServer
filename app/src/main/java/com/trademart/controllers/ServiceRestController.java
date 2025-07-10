package com.trademart.controllers;

import java.time.LocalDateTime;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.service.JobCategory;
import com.trademart.service.JobType;
import com.trademart.service.Service;
import com.trademart.service.ServiceController;

@RestController
public class ServiceRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private ServiceController serviceController;

    public ServiceRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        this.serviceController = new ServiceController(sharedResource);
    }

    @GetMapping("/service/{job_id}")
    public ResponseEntity<String> serveServiceDetailsMapping(@PathVariable("job_id") int jobId){
        Service service = serviceController.findServiceByID(jobId);
        if(service == null){
            return ResponseEntity.notFound().build();
        }

        JSONObject json = new JSONObject()
            .put("job_id", service.getJobId())
            .put("job_title", service.getJobTitle())
            .put("job_description", service.getJobDescription())
            .put("job_type", service.getJobType())
            .put("job_category", service.getJobCategory())
            .put("date_posted", service.getDatePosted())
            .put("user_id", service.getUserId());

        return ResponseEntity.ok(json.toString());
    }

    @PostMapping("/service/create")
    public ResponseEntity<String> createServiceDetailsMapping(@RequestBody String content){
        JSONObject json = new JSONObject(new JSONTokener(content));
        Service service = serviceController.findServiceByID(json.getInt("job_id"));
        if(service != null){
            return ResponseEntity.ok(createResponse("failed", "a service with the specified id already exists").toString());
        }

        service = new Service.ServiceBuilder()
            .setJobId(json.getInt("job_id"))
            .setJobTitle(json.getString("job_title"))
            .setJobDescription(json.getString("job_description"))
            .setJobType(JobType.parse(json.getString("job_type")))
            .setJobCategory(JobCategory.parse(json.getString("job_category")))
            .setDatePosted(LocalDateTime.parse(json.getString("date_posted")))
            .setUserId(json.getInt("user_id"))
            .build();
        
        if(!serviceController.writeServiceToDB(service)){
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(createResponse("success", "service successfully created").toString());
    }
    
}
