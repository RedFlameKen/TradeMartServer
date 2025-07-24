package com.trademart.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.json.JSONArray;
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
import com.trademart.media.MediaController;
import com.trademart.service.Service;
import com.trademart.service.ServiceController;
import com.trademart.user.User;
import com.trademart.user.UserController;
import com.trademart.util.FileUtil;

@RestController
public class ServiceRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private ServiceController serviceController;
    private MediaController mediaController;
    private UserController userController;

    public ServiceRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        this.serviceController = new ServiceController(sharedResource);
        this.mediaController = new MediaController(sharedResource);
        this.userController = new UserController(sharedResource);
    }

    @GetMapping("/service/{service_id}")
    public ResponseEntity<String> serveServiceDetailsMapping(@PathVariable("service_id") int serviceId){
        Service service;
        ArrayList<FeedCategory> categories;
        try {
            service = serviceController.findServiceByID(serviceId);
            categories = serviceController.getCategoriesById(serviceId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        if(service == null){
            return ResponseEntity.notFound().build();
        }

        JSONObject json = new JSONObject()
            .put("service_id", service.getServiceId())
            .put("service_title", service.getServiceTitle())
            .put("service_description", service.getServiceDescription())
            .put("service_price", service.getServicePrice())
            .put("service_currency", service.getServiceCurrency())
            .put("date_posted", service.getDatePosted())
            .put("owner_id", service.getOwnerId())
            .put("categories", categories);

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
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(json.toString());
    }

    @GetMapping("/service/list/{user_id}")
    public ResponseEntity<String> fetchServicesByUserIdMapping(@PathVariable("user_id") int userId){
        User user = userController.getUserFromDB(userId);
        if (user == null) {
            return internalServerErrorResponse("no user with the given owner_id was found");
        }
        ArrayList<Service> services;
        try {
             services = serviceController.findServicesByUserId(userId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("an internal server error occured");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse("an internal server error occured");
        }

        JSONObject json = createResponse("success", "fetched services");

        JSONObject data = new JSONObject();
        for (Service service : services) {
            data.append("services", service.parseJson());
        }
        json.put("data", data);
        return ResponseEntity.ok(json.toString());
    }

    @PostMapping("/service/create/{service_id}/media")
    public ResponseEntity<String> uploadServiceMediaMapping(@PathVariable("service_id") int serviceId,
            @RequestHeader("Content-Disposition") String dispositionStr, @RequestBody byte[] data) {

        Service service;
        try {
            service = serviceController.findServiceByID(serviceId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
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
                filepath = FileUtil.removeExtension(filepath).concat(".m3u8");
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
        JSONObject json = null;
        ArrayList<FeedCategory> categories = new ArrayList<>();
        try {
            json = new JSONObject(new JSONTokener(content));
            JSONArray categoriesJson = json.getJSONArray("categories");
            for (int i = 0; i < categoriesJson.length(); i++) {
                categories.add(FeedCategory.valueOf(categoriesJson.getString(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        int ownerId = json.getInt("owner_id");
        User user = userController.getUserFromDB(ownerId);
        if (user == null) {
            return ResponseEntity.internalServerError()
                    .body(createResponse("failed", "no user with the given user_id was found").toString());
        }
        Service service = null;
        try {
            service = publishService(json, ownerId, categories);
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok(service.parseJson().toString());
    }

    private Service publishService(JSONObject json, int ownerId, ArrayList<FeedCategory> categories) throws SQLException, InterruptedException{
        int id = serviceController.generateServiceID();
        Service service = new Service.ServiceBuilder()
            .setServiceId(id)
            .setServiceTitle(json.getString("service_title"))
            .setServiceDescription(json.getString("service_description"))
            .setServicePrice(json.getDouble("service_price"))
            // .setServiceCurrency(json.getString("service_currency"))
            .setDatePosted(LocalDateTime.parse(json.getString("date_posted")))
            .setOwnerId(ownerId)
            .build();
        serviceController.writeServiceToDB(service, categories);
        return service;
    }
    
}
