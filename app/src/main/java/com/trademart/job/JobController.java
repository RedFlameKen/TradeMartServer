package com.trademart.job;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.db.IDGenerator;

public class JobController {

    private SharedResource sharedResource;
    private DatabaseController dbController;

    public JobController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        dbController = sharedResource.getDatabaseController();
    }

    public int generateJobID() {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "job_listings", "job_id");
        sharedResource.unlock();
        return id;
    }


    public JobListing findJobById(int id) throws InterruptedException, SQLException{
        sharedResource.lock();

        String command = "select * from job_listings where job_id=?";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, id);
        ResultSet rs = prep.executeQuery();

        JobListing job = null;
        if(rs.next()) {
            job = new JobListing.Builder()
                .setId(id)
                .setTitle(rs.getString("job_title"))
                .setDescription(rs.getString("job_description"))
                .setAmount(rs.getDouble("amount"))
                .setCategory(JobCategory.parse(rs.getString("job_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setEmployerId(rs.getInt("employer_id"))
                .build();
        }
        sharedResource.unlock();
        return job;
    }

    public ArrayList<JobListing> findJobsByEmployerId(int employerId) throws InterruptedException, SQLException{
        ArrayList<JobListing> jobs = new ArrayList<>();
        sharedResource.lock();

        String command = "select * from job_listings where employer_id=?";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, employerId);
        ResultSet rs = prep.executeQuery();

        while(rs.next()) {
            jobs.add(new JobListing.Builder()
                .setId(rs.getInt("job_id"))
                .setTitle(rs.getString("job_title"))
                .setDescription(rs.getString("job_description"))
                .setAmount(rs.getDouble("amount"))
                .setCategory(JobCategory.parse(rs.getString("job_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setEmployerId(employerId)
                .build());
        }
        sharedResource.unlock();
        return jobs;
    }

    public void writeJobToDb(JobListing job) throws SQLException, InterruptedException{
        sharedResource.lock();

        String command = "insert into job_listings(job_id, job_title, job_description, amount, job_category, date_posted, employer_id)values(?,?,?,?,?,?,?)";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, job.getId());
        prep.setString(2, job.getTitle());
        prep.setString(3, job.getDescription());
        prep.setDouble(4, job.getAmount());
        prep.setString(5, job.getCategory().toString());
        prep.setTimestamp(6, Timestamp.valueOf(job.getDatePosted()));
        prep.setInt(7, job.getEmployerId());

        sharedResource.unlock();
    }

    public JSONObject jobArrayToJSON(ArrayList<JobListing> jobs){
        JSONObject json = new JSONObject();
        for (JobListing job : jobs) {
            json.append("jobs", job.parseJson());
        }
        return json;
    }

    public JobListing createJobFromJSON(JSONObject json) throws JSONException{
        return new JobListing.Builder()
            .setId(generateJobID())
            .setEmployerId(json.getInt("employer_id"))
            .setTitle(json.getString("job_title"))
            .setDescription(json.getString("job_description"))
            .setAmount(json.getDouble("amount"))
            .setCategory(JobCategory.parse(json.getString("job_category")))
            .setDatePosted(LocalDateTime.now())
            .build();
    }

}
