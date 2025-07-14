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
import com.trademart.service.ServiceCategory;
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
