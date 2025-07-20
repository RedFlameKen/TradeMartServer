package com.trademart.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.db.IDGenerator;
import com.trademart.feed.FeedCategory;

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
                .setServiceCategory(FeedCategory.parse(rs.getString("service_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setOwnerId(rs.getInt("owner_id"))
                .setLikes(rs.getInt("likes"))
                .build();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sharedResource.unlock();
        return service;
    }

    public void likeService(Service service, int likerId) throws InterruptedException, SQLException{
        if(userHasLiked(likerId, service.getServiceId())){
            return;
        }
        String command = "update services set likes=? where service_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, service.getLikes()+1);
        prep.setInt(2, service.getServiceId());
        prep.execute();
        sharedResource.unlock();
        registerUserLike(service.getServiceId(), likerId);
    }

    public void registerUserLike(int serviceId, int userId) throws SQLException, InterruptedException{
        String command = "insert into service_likes(user_id, service_id)values(?,?)";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setInt(2, serviceId);
        prep.execute();
        sharedResource.unlock();
    }

    public boolean userHasLiked(int userId, int serviceId) throws SQLException, InterruptedException{
        String command = "select * from service_likes where user_id=? and service_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setInt(2, serviceId);
        ResultSet rs = prep.executeQuery();
        boolean result = false;
        if(rs.next()){
            result = true;
        }
        sharedResource.unlock();
        return result;
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
                .setServiceCategory(FeedCategory.parse(rs.getString("service_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setOwnerId(rs.getInt("owner_id"))
                .setLikes(rs.getInt("likes"))
                .build();
            services.add(service);
        }
        sharedResource.unlock();
        return services;
    }

    public ArrayList<Service> getAllServices() throws InterruptedException, SQLException{
        sharedResource.lock();
        ArrayList<Service> services = new ArrayList<>();
        String command = "select * from services";
        ResultSet rs = dbController.execQuery(command);
        while(rs.next()){
            Service service = new Service.ServiceBuilder()
                .setServiceId(rs.getInt("service_id"))
                .setServiceTitle(rs.getString("service_title"))
                .setServiceDescription(rs.getString("service_description"))
                .setServiceCategory(FeedCategory.parse(rs.getString("service_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setOwnerId(rs.getInt("owner_id"))
                .setLikes(rs.getInt("likes"))
                .build();
            services.add(service);
        }
        sharedResource.unlock();
        return services;
    }

    public void writeServiceToDB(Service service) throws SQLException, InterruptedException {
        String command = "insert into services(service_id,service_title,service_category,service_description,likes,service_price,service_currency,date_posted, owner_id) values (?,?,?,?,?,?,?,?,?)";
        sharedResource.lock();

        PreparedStatement prep = dbController.prepareStatement(command);

        prep.setInt(1, service.getServiceId());
        prep.setString(2, service.getServiceTitle());
        prep.setString(3, service.getServiceCategory().toString());
        prep.setString(4, service.getServiceDescription());
        prep.setInt(5, service.getLikes());
        prep.setDouble(6, service.getServicePrice());
        prep.setString(7, service.getServiceCurrency());
        prep.setTimestamp(8, Timestamp.valueOf(service.getDatePosted()));
        prep.setInt(9, service.getOwnerId());
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
