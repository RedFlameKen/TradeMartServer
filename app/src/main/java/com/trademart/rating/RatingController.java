package com.trademart.rating;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.db.IDGenerator;
import com.trademart.job.JobListing;

public class RatingController {

    private SharedResource sharedResource;
    private DatabaseController dbController;

    public RatingController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        dbController = sharedResource.getDatabaseController();
    }
    
    public int generateRatingID() {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "job_ratings", "id");
        sharedResource.unlock();
        return id;
    }

    public JobRating findJobRatingById(int ratingId) throws InterruptedException, SQLException{
        JobRating rating = null;
        String command = "select * from job_ratings where rating_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, ratingId);
        ResultSet rs = prep.executeQuery();
        if(rs.next()) {
            rating = new JobRating.Builder()
                .setId(rs.getInt("id"))
                .setJobTransactionId(rs.getInt("job_transaction_id"))
                .setRaterId(rs.getInt("rater_id"))
                .setComment(rs.getString("comment"))
                .setRate(rs.getDouble("rating"))
                .build();
        }
        prep.close();
        sharedResource.unlock();
        return rating;
    }

    public JobRating findJobRatingByTransactionId(int transactionId) throws InterruptedException, SQLException{
        JobRating rating = null;
        String command = "select * from job_ratings where job_transaction_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, transactionId);
        ResultSet rs = prep.executeQuery();
        if(rs.next()) {
            rating = new JobRating.Builder()
                .setId(rs.getInt("id"))
                .setJobTransactionId(rs.getInt("job_transaction_id"))
                .setRaterId(rs.getInt("rater_id"))
                .setComment(rs.getString("comment"))
                .setRate(rs.getDouble("rating"))
                .build();
        }
        prep.close();
        sharedResource.unlock();
        return rating;
    }

    public ArrayList<JobRating> findJobRatingsByJobTransactionId(int transactionId) throws InterruptedException, SQLException{
        ArrayList<JobRating> ratings = new ArrayList<>();
        String command = "select * from job_ratings where job_transaction_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, transactionId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()) {
            ratings.add(new JobRating.Builder()
                .setId(rs.getInt("id"))
                .setJobTransactionId(rs.getInt("job_transaction_id"))
                .setRaterId(rs.getInt("rater_id"))
                .setComment(rs.getString("comment"))
                .setRate(rs.getDouble("rating"))
                .build());
        }
        prep.close();
        sharedResource.unlock();
        return ratings;
    }
//;

    public ArrayList<JobRating> findJobRatingByEmployeeId(int employeeId) throws InterruptedException, SQLException{
        ArrayList<JobRating> ratings = new ArrayList<>();
        String command = "select * from job_ratings join job_transactions on job_ratings.job_transaction_id = job_transactions.id where employee_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, employeeId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()) {
            ratings.add(new JobRating.Builder()
                .setId(rs.getInt("id"))
                .setJobTransactionId(rs.getInt("job_transaction_id"))
                .setRaterId(rs.getInt("rater_id"))
                .setComment(rs.getString("comment"))
                .setRate(rs.getDouble("rating"))
                .build());
        }
        prep.close();
        sharedResource.unlock();
        return ratings;
    }

    public ArrayList<JobRating> findJobRatingByRaterId(int raterId) throws InterruptedException, SQLException{
        ArrayList<JobRating> ratings = new ArrayList<>();
        String command = "select * from job_ratings where rater_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, raterId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()) {
            ratings.add(new JobRating.Builder()
                .setId(rs.getInt("id"))
                .setJobTransactionId(rs.getInt("job_transaction_id"))
                .setRaterId(rs.getInt("rater_id"))
                .setComment(rs.getString("comment"))
                .setRate(rs.getDouble("rating"))
                .build());
        }
        prep.close();
        sharedResource.unlock();
        return ratings;
    }

    public void writeJobRatingToDB(JobRating rating) throws InterruptedException, SQLException{
        String command = "insert into job_ratings(id,rating,comment,job_transaction_id,rater_id)values(?,?,?,?,?)";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, generateRatingID());
        prep.setDouble(2, rating.getRate());
        prep.setString(3, rating.getComment());
        prep.setInt(4, rating.getJobTransactionId());
        prep.setInt(5, rating.getRaterId());
        prep.execute();
        prep.close();
        sharedResource.unlock();
    }

}
