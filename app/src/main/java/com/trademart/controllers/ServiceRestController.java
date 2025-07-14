package com.trademart.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

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
import com.trademart.media.MediaController;
import com.trademart.service.Service;
import com.trademart.service.ServiceController;
import com.trademart.util.FileUtil;

@RestController
public class ServiceRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private ServiceController serviceController;
    private MediaController mediaController;

    public ServiceRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        this.serviceController = new ServiceController(sharedResource);
        this.mediaController = new MediaController(sharedResource);
    }

    @GetMapping("/service/{service_id}")
    public ResponseEntity<String> serveServiceDetailsMapping(@PathVariable("service_id") int serviceId){
        Service service = serviceController.findServiceByID(serviceId);
        if(service == null){
            return ResponseEntity.notFound().build();
        }

        JSONObject json = new JSONObject()
            .put("service_id", service.getServiceId())
            .put("service_title", service.getServiceTitle())
            .put("service_description", service.getServiceDescription())
            .put("service_category", service.getServiceCategory())
            .put("service_price", service.getServicePrice())
            .put("service_currency", service.getServiceCurrency())
            .put("date_posted", service.getDatePosted())
            .put("owner_id", service.getOwnerId());

        return ResponseEntity.ok(json.toString());
    }

    @GetMapping("/service/{service_id}/media")
    public ResponseEntity<String> fetchServiceMediaIDsMapping(@PathVariable("service_id") int serviceId){
        JSONObject json = new JSONObject();
        try {
            ArrayList<Integer> ids = serviceController.getServiceMediaIDs(serviceId);
            json.put("service_ids", ids);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(json.toString());
    }

    @PostMapping("/service/create/{service_id}/media")
    public ResponseEntity<String> uploadServiceMediaMapping(@PathVariable("service_id") int serviceId,
            @RequestHeader("Content-Disposition") String dispositionStr, byte[] data) {

        Service service = serviceController.findServiceByID(serviceId);
        if(service == null){
            return ResponseEntity.notFound().build();
        }

        String filename = ContentDisposition.parse(dispositionStr).getFilename();
        int mediaId = -1;
        try {
            File file = mediaController.writeFile(filename, data);
            String filepath = file.getAbsolutePath();
            String ext = FileUtil.getExtension(file.getName());
            if(ext.equals("mp4")){
                filepath = FileUtil.removeExtension(filepath).concat("m3u8");
            }
            mediaId = mediaController.insertServiceMediaToDB(filepath, service.getOwnerId(), serviceId);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).header("Location",
                new StringBuilder()
                .append("/media/")
                .append(mediaId)
                .toString())
            .body(createResponse("success", "image uploaded successfully").toString());
    }

    @PostMapping("/service/create")
    public ResponseEntity<String> createServiceDetailsMapping(@RequestBody String content){
        JSONObject json = new JSONObject(new JSONTokener(content));
        // Service service = serviceController.findServiceByID(json.getInt("service_id"));
        // if(service != null){
        //     return ResponseEntity.ok(createResponse("failed", "a service with the specified id already exists").toString());
        // }

        Service service = new Service.ServiceBuilder()
            .setServiceTitle(json.getString("service_title"))
            .setServiceDescription(json.getString("service_description"))
            // .setServiceCategory(ServiceCategory.parse(json.getString("service_category")))
            .setServicePrice(json.getDouble("service_price"))
            // .setServiceCurrency(json.getString("service_currency"))
            .setDatePosted(LocalDateTime.parse(json.getString("date_posted")))
            .setOwnerId(json.getInt("owner_id"))
            .build();
        
        if(!serviceController.writeServiceToDB(service)){
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(createResponse("success", "service successfully created").toString());
    }
    
}
