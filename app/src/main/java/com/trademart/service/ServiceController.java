package com.trademart.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.db.IDGenerator;

public class ServiceController {

    private DatabaseController dbController;
    private SharedResource sharedResource;

    public ServiceController(SharedResource sharedResource){ 
        this.sharedResource = sharedResource;
        this.dbController = sharedResource.getDatabaseController();
    }

    public Service findServiceByID(int serviceId){
        String command = "select * from services where service_id=" + serviceId;
        Service service = null;
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        if(dbController.getCommandRowCount(command) <= 0){
            sharedResource.unlock();
            return null;
        }
        try {
            PreparedStatement prep = dbController.prepareStatement(command);
            ResultSet rs = prep.executeQuery();
            rs.next();
            service = new Service.ServiceBuilder()
                .setServiceId(rs.getInt("service_id"))
                .setServiceTitle(rs.getString("service_title"))
                .setServiceDescription(rs.getString("service_description"))
                .setServiceCategory(ServiceCategory.parse(rs.getString("service_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setOwnerId(rs.getInt("owner_id"))
                .build();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sharedResource.unlock();
        return service;
    }

    public boolean writeServiceToDB(Service service){
        String command = "insert into services(service_id,service_title,service_category,service_description,service_price,service_currency,date_posted, owner_id) values (?,?,?,?,?,?,?,?)";
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        try {
            int serviceId = IDGenerator.generateDBID(dbController, "services", "service_id");
            PreparedStatement prep = dbController.prepareStatement(command);

            prep.setInt(1, serviceId);
            prep.setString(2, service.getServiceTitle());
            prep.setString(3, service.getServiceCategory().toString());
            prep.setString(4, service.getServiceDescription());
            prep.setDouble(5, service.getServicePrice());
            prep.setString(6, service.getServiceCurrency());
            prep.setTimestamp(7, Timestamp.valueOf(service.getDatePosted()));
            prep.setInt(8, service.getOwnerId());
            prep.execute();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        sharedResource.unlock();
        return true;
    }

}
