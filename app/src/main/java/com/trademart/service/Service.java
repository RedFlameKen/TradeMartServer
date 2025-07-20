package com.trademart.service;

import java.time.LocalDateTime;

import org.json.JSONObject;

import com.trademart.feed.FeedCategory;

public class Service {

    private int serviceId;
    private String serviceTitle;
    private FeedCategory serviceCategory;
    private int likes;
    private String serviceDescription;
    private LocalDateTime datePosted;
    private double servicePrice;
    private String serviceCurrency;
    private int ownerId;

    public Service(ServiceBuilder builder){
        serviceId = builder.serviceId;
        serviceTitle = builder.serviceTitle;
        serviceCategory = builder.serviceCategory;
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

    public FeedCategory getServiceCategory() {
        return serviceCategory;
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
            .put("service_category", serviceCategory)
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
        private FeedCategory serviceCategory;
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
            serviceCategory = FeedCategory.NONE;
            servicePrice = 0;
            datePosted = null;
        }

        public ServiceBuilder setServiceId(int serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public ServiceBuilder setServiceTitle(String serviceTitle) {
            this.serviceTitle = serviceTitle;
            return this;
        }

        public ServiceBuilder setServiceCategory(FeedCategory serviceCategory) {
            this.serviceCategory = serviceCategory;
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
}
