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

    public Service findServiceByID(int serviceId) throws InterruptedException, SQLException{
        String command = "select * from services where service_id=" + serviceId;
        Service service = null;
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        ResultSet rs = prep.executeQuery();
        rs.next();
        service = new Service.ServiceBuilder()
            .setServiceId(rs.getInt("service_id"))
            .setServiceTitle(rs.getString("service_title"))
            .setServiceDescription(rs.getString("service_description"))
            .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
            .setServicePrice(rs.getDouble("service_price"))
            .setOwnerId(rs.getInt("owner_id"))
            .setLikes(rs.getInt("likes"))
            .build();
        sharedResource.unlock();
        return service;
    }

    public ArrayList<FeedCategory> getCategoriesById(int serviceId) throws SQLException, InterruptedException{
        ArrayList<FeedCategory> categories = new ArrayList<>();
        String command = "select * from service_categories where service_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, serviceId);
        ResultSet rs = prep.executeQuery();
        while (rs.next()) {
            categories.add(FeedCategory.parse(rs.getString("category")));
        }
        prep.close();
        sharedResource.unlock();
        return categories;
    }

    public void likeService(Service service, int likerId, boolean isLiking) throws InterruptedException, SQLException{
        String command = "update services set likes=? where service_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, service.getLikes()+(isLiking ? 1 : -1));
        prep.setInt(2, service.getServiceId());
        prep.execute();
        sharedResource.unlock();
        registerUserLike(service.getServiceId(), likerId, isLiking);
    }

    public void registerUserLike(int serviceId, int userId, boolean isLiking) throws SQLException, InterruptedException{
        String command = isLiking ?
            "insert into service_likes(user_id, service_id)values(?,?)" :
            "delete from service_likes where user_id=? and service_id=?";
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
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setServicePrice(rs.getDouble("service_price"))
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
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setServicePrice(rs.getDouble("service_price"))
                .setOwnerId(rs.getInt("owner_id"))
                .setLikes(rs.getInt("likes"))
                .build();
            services.add(service);
        }
        sharedResource.unlock();
        return services;
    }

    public void writeServiceToDB(Service service, ArrayList<FeedCategory> categories) throws SQLException, InterruptedException {
        String command = "insert into services(service_id,service_title,service_description,likes,service_price,service_currency,date_posted, owner_id) values (?,?,?,?,?,?,?,?)";
        sharedResource.lock();

        PreparedStatement prep = dbController.prepareStatement(command);

        prep.setInt(1, service.getServiceId());
        prep.setString(2, service.getServiceTitle());
        prep.setString(3, service.getServiceDescription());
        prep.setInt(4, service.getLikes());
        prep.setDouble(5, service.getServicePrice());
        prep.setString(6, service.getServiceCurrency());
        prep.setTimestamp(7, Timestamp.valueOf(service.getDatePosted()));
        prep.setInt(8, service.getOwnerId());
        prep.execute();

        sharedResource.unlock();
        if(categories.size() == 0){
            categories.add(FeedCategory.NONE);
        }
        for (FeedCategory category : categories) {
            insertCategoryToDB(category, service.getServiceId());
        }
    }

    private void insertCategoryToDB(FeedCategory category, int serviceId) throws SQLException, InterruptedException{
        String command = "insert into service_categories(service_id, category)values(?,?)";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, serviceId);
        prep.setString(2, category.toString());
        prep.execute();
        prep.close();

        sharedResource.unlock();
    }

    public ArrayList<Integer> getServiceMediaIDs(int serviceId) throws SQLException, InterruptedException{
        ArrayList<Integer> ids = new ArrayList<>();
        sharedResource.lock();

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
