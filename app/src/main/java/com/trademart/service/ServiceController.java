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

    public Service findServiceByID(int jobId){
        String command = "select * from job_postings where job_id=" + jobId;
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
                .setJobId(rs.getInt("job_id"))
                .setJobTitle(rs.getString("job_title"))
                .setJobDescription(rs.getString("job_description"))
                .setJobType(JobType.parse(rs.getString("job_type")))
                .setJobCategory(JobCategory.parse(rs.getString("job_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setUserId(rs.getInt("user_id"))
                .build();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sharedResource.unlock();
        return service;
    }

    public boolean writeServiceToDB(Service service){
        String command = "insert into job_postings(job_id,job_title,job_type,job_category,job_description,date_posted, user_id) values (?,?,?,?,?,?,?)";
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        try {
            int jobId = IDGenerator.generateDBID(dbController, "job_postings", "job_id");
            PreparedStatement prep = dbController.prepareStatement(command);

            prep.setInt(1, jobId);
            prep.setString(2, service.getJobTitle());
            prep.setString(3, service.getJobType().toString());
            prep.setString(4, service.getJobCategory().toString());
            prep.setString(5, service.getJobDescription());
            prep.setTimestamp(6, Timestamp.valueOf(service.getDatePosted()));
            prep.setInt(7, service.getUserId());
            prep.execute();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        sharedResource.unlock();
        return true;
    }

}
