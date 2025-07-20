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
import com.trademart.feed.FeedCategory;

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

    public ArrayList<JobListing> getAllJobListingsFromDB() throws InterruptedException, SQLException{
        ArrayList<JobListing> jobs = new ArrayList<>();
        String command = "select * from job_listings";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            jobs.add(new JobListing.Builder()
                .setId(rs.getInt("job_id"))
                .setTitle(rs.getString("job_title"))
                .setDescription(rs.getString("job_description"))
                .setAmount(rs.getDouble("amount"))
                .setCategory(FeedCategory.parse(rs.getString("job_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setEmployerId(rs.getInt("employer_id"))
                .setLikes(rs.getInt("likes"))
                .build());
        }
        sharedResource.unlock();
        return jobs;
    }

    public JobListing findJobByID(int id) throws InterruptedException, SQLException{
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
                .setCategory(FeedCategory.parse(rs.getString("job_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setEmployerId(rs.getInt("employer_id"))
                .setLikes(rs.getInt("likes"))
                .build();
        }
        sharedResource.unlock();
        return job;
    }

    public void likeJob(JobListing job) throws InterruptedException, SQLException{
        String command = "update job_listings set likes=? where job_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, job.getLikes()+1);
        prep.setInt(2, job.getId());
        prep.execute();
        sharedResource.unlock();
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
                .setCategory(FeedCategory.parse(rs.getString("job_category")))
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setEmployerId(employerId)
                .setLikes(rs.getInt("likes"))
                .build());
        }
        sharedResource.unlock();
        return jobs;
    }

    public void writeJobToDb(JobListing job) throws SQLException, InterruptedException{
        sharedResource.lock();

        String command = "insert into job_listings(job_id, job_title, job_description, amount, job_category, likes, date_posted, employer_id)values(?,?,?,?,?,?,?)";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, job.getId());
        prep.setString(2, job.getTitle());
        prep.setString(3, job.getDescription());
        prep.setDouble(4, job.getAmount());
        prep.setInt(5, job.getLikes());
        prep.setString(6, job.getCategory().toString());
        prep.setTimestamp(7, Timestamp.valueOf(job.getDatePosted()));
        prep.setInt(8, job.getEmployerId());
        prep.execute();

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
            .setCategory(FeedCategory.parse(json.getString("job_category")))
            .setDatePosted(LocalDateTime.now())
            .build();
    }

    public ArrayList<Integer> getJobMediaIDs(int jobId) throws SQLException{
        ArrayList<Integer> ids = new ArrayList<>();
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DatabaseController db = sharedResource.getDatabaseController();
        String command = "select job_media.media_id from job_media join media on media.media_id = job_media.media_id where job_media.job_id=? order by date_uploaded";
        PreparedStatement prep = db.prepareStatement(command);
        prep.setInt(1, jobId);
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
