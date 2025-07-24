package com.trademart.service;

import java.time.LocalDateTime;

import org.json.JSONObject;

import com.trademart.search.SearchIndexable;

public class Service implements SearchIndexable {

    private int serviceId;
    private String serviceTitle;
    private int likes;
    private String serviceDescription;
    private LocalDateTime datePosted;
    private double servicePrice;
    private String serviceCurrency;
    private int ownerId;

    public Service(ServiceBuilder builder){
        serviceId = builder.serviceId;
        serviceTitle = builder.serviceTitle;
        likes = builder.likes;
        serviceDescription = builder.serviceDescription;
        servicePrice = builder.servicePrice;
        serviceCurrency = builder.serviceCurrency;
        datePosted = builder.datePosted;
        ownerId = builder.ownerId;
    }

    public int getServiceId() {
        return serviceId;
    }

    public String getServiceTitle() {
        return serviceTitle;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public double getServicePrice() {
        return servicePrice;
    }

    public String getServiceCurrency() {
        return serviceCurrency;
    }

    public LocalDateTime getDatePosted() {
        return datePosted;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getLikes() {
        return likes;
    }

    public JSONObject parseJson(){
        return new JSONObject()
            .put("service_id", serviceId)
            .put("service_title", serviceTitle)
            .put("service_description", serviceDescription)
            .put("date_posted", datePosted)
            .put("service_price", servicePrice)
            .put("service_currency", serviceCurrency)
            .put("owner_id", ownerId);
    }

    public static class ServiceBuilder {

        private int serviceId;
        private String serviceTitle;
        private int likes;
        private String serviceDescription;
        private LocalDateTime datePosted;
        private double servicePrice;
        private String serviceCurrency;
        private int ownerId;

        public ServiceBuilder(){
            serviceId = ownerId = -1;
            serviceTitle = serviceDescription = "";
            likes = 0;
            serviceCurrency = "PHP";
            servicePrice = 0;
            datePosted = LocalDateTime.now();
        }

        public ServiceBuilder setServiceId(int serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public ServiceBuilder setServiceTitle(String serviceTitle) {
            this.serviceTitle = serviceTitle;
            return this;
        }

        public ServiceBuilder setServiceDescription(String serviceDescription) {
            this.serviceDescription = serviceDescription;
            return this;
        }

        public ServiceBuilder setDatePosted(LocalDateTime datePosted) {
            this.datePosted = datePosted;
            return this;
        }

        public ServiceBuilder setOwnerId(int ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public ServiceBuilder setServicePrice(double servicePrice) {
            this.servicePrice = servicePrice;
            return this;
        }

        public ServiceBuilder setServiceCurrency(String serviceCurrency) {
            if(serviceCurrency != null)
                this.serviceCurrency = serviceCurrency;
            return this;
        }

        public ServiceBuilder setLikes(int likes) {
            this.likes = likes;
            return this;
        }

        public Service build(){
            return new Service(this);
        }

    }

    @Override
    public int getIndexId() {
        return serviceId;
    }

    @Override
    public String getKeyTerm() {
        return serviceTitle;
    }

    @Override
    public JSONObject getIndexJson(double relPoints) {
        return new JSONObject()
            .put("result", serviceTitle)
            .put("id", serviceId)
            .put("relevance", relPoints)
            .put("entity", parseJson());
    }
}
