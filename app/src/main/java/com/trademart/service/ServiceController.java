package com.trademart.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

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

    public int generateServiceID(){
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "services", "service_id");
        sharedResource.unlock();
        return id;
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

    public ArrayList<Service> findServicesByUserId(int userId) throws InterruptedException, SQLException{
        ArrayList<Service> services = new ArrayList<>();
        String command = "select * from services where owner_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            Service service = new Service.ServiceBuilder()
                .setServiceId(rs.getInt("service_id"))
                .setServiceTitle(rs.getString("service_title"))
                .setServiceDescription(rs.getString("service_description"))
                .setServiceCategory(ServiceCategory.parse(rs.getString("service_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setOwnerId(rs.getInt("owner_id"))
                .build();
            services.add(service);
        }
        sharedResource.unlock();
        return services;
    }

    public ArrayList<Service> getAllServices(){
        ArrayList<Service> services = new ArrayList<>();
        String command = "select * from services";
        try {
            ResultSet rs = dbController.execQuery(command);
            while(rs.next()){
                Service service = new Service.ServiceBuilder()
                    .setServiceId(rs.getInt("service_id"))
                    .setServiceTitle(rs.getString("service_title"))
                    .setServiceDescription(rs.getString("service_description"))
                    .setServiceCategory(ServiceCategory.parse(rs.getString("service_category")))
                    .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                    .setOwnerId(rs.getInt("owner_id"))
                    .build();
                services.add(service);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return services;
    }

    public void writeServiceToDB(Service service) throws SQLException, InterruptedException {
        String command = "insert into services(service_id,service_title,service_category,service_description,service_price,service_currency,date_posted, owner_id) values (?,?,?,?,?,?,?,?)";
        sharedResource.lock();

        PreparedStatement prep = dbController.prepareStatement(command);

        prep.setInt(1, service.getServiceId());
        prep.setString(2, service.getServiceTitle());
        prep.setString(3, service.getServiceCategory().toString());
        prep.setString(4, service.getServiceDescription());
        prep.setDouble(5, service.getServicePrice());
        prep.setString(6, service.getServiceCurrency());
        prep.setTimestamp(7, Timestamp.valueOf(service.getDatePosted()));
        prep.setInt(8, service.getOwnerId());
        prep.execute();

        sharedResource.unlock();
    }

    public ArrayList<Integer> getServiceMediaIDs(int serviceId) throws SQLException{
        ArrayList<Integer> ids = new ArrayList<>();
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DatabaseController db = sharedResource.getDatabaseController();
        String command = "select service_media.media_id from service_media join media on media.media_id = service_media.media_id where service_media.service_id=? order by date_uploaded";
        PreparedStatement prep = db.prepareStatement(command);
        prep.setInt(1, serviceId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            ids.add(rs.getInt("media_id"));
        }
        rs.close();
        prep.close();
        sharedResource.unlock();
        return ids;
    }
}
