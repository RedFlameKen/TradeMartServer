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

    public int generateJobTransactionID() {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "job_transactions", "id");
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
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setEmployerId(rs.getInt("employer_id"))
                .setLikes(rs.getInt("likes"))
                .build());
        }
        sharedResource.unlock();
        return jobs;
    }

    public ArrayList<FeedCategory> getCategoriesById(int jobId) throws SQLException, InterruptedException{
        ArrayList<FeedCategory> categories = new ArrayList<>();
        String command = "select * from job_categories where job_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, jobId);
        ResultSet rs = prep.executeQuery();
        while (rs.next()) {
            categories.add(FeedCategory.parse(rs.getString("category")));
        }
        prep.close();
        sharedResource.unlock();
        return categories;
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
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setEmployerId(rs.getInt("employer_id"))
                .setLikes(rs.getInt("likes"))
                .build();
        }
        sharedResource.unlock();
        return job;
    }

    public void likeJob(JobListing job, int likerId, boolean isLiking) throws InterruptedException, SQLException{
        String command = "update job_listings set likes=? where job_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, job.getLikes()+(isLiking ? 1 : -1));
        prep.setInt(2, job.getId());
        prep.execute();
        sharedResource.unlock();
        registerUserLike(job.getId(), likerId, isLiking);
    }

    public void registerUserLike(int jobId, int userId, boolean isLiking) throws SQLException, InterruptedException{
        String command = isLiking ?
            "insert into job_likes(user_id, job_id)values(?,?)" :
            "delete from job_likes where user_id=? and job_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setInt(2, jobId);
        prep.execute();
        sharedResource.unlock();
    }

    public boolean userHasLiked(int userId, int jobId) throws SQLException, InterruptedException{
        String command = "select * from job_likes where user_id=? and job_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setInt(2, jobId);
        ResultSet rs = prep.executeQuery();
        boolean result = false;
        if(rs.next()){
            result = true;
        }
        sharedResource.unlock();
        return result;
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
                .setDatePosted(rs.getTimestamp("date_posted").toLocalDateTime())
                .setEmployerId(employerId)
                .setLikes(rs.getInt("likes"))
                .build());
        }
        sharedResource.unlock();
        return jobs;
    }

    public void writeJobToDb(JobListing job, ArrayList<FeedCategory> categories) throws SQLException, InterruptedException{
        sharedResource.lock();

        String command = "insert into job_listings(job_id, job_title, job_description, amount, likes, date_posted, employer_id)values(?,?,?,?,?,?,?)";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, job.getId());
        prep.setString(2, job.getTitle());
        prep.setString(3, job.getDescription());
        prep.setDouble(4, job.getAmount());
        prep.setInt(5, job.getLikes());
        prep.setTimestamp(6, Timestamp.valueOf(job.getDatePosted()));
        prep.setInt(7, job.getEmployerId());
        prep.execute();

        sharedResource.unlock();
        if(categories.size() == 0){
            categories.add(FeedCategory.NONE);
        }
        for (FeedCategory category : categories) {
            insertCategoryToDB(category, job.getId());
        }
    }

    private void insertCategoryToDB(FeedCategory category, int jobId) throws SQLException, InterruptedException{
        String command = "insert into job_categories(job_id, category)values(?,?)";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, jobId);
        prep.setString(2, category.toString());
        prep.execute();
        prep.close();

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

    public void writeJobTransactionToDB(JobTransaction transaction) throws InterruptedException, SQLException{
        int generatedId = generateJobTransactionID();
        String command = "insert into job_transactions(id, job_id, employee_id, employer_id, date_published, completed)values(?,?,?,?,?,?)";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, generatedId);
        prep.setInt(2, transaction.getJobId());
        prep.setInt(3, transaction.getEmployeeId());
        prep.setInt(4, transaction.getEmployerId());
        prep.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
        prep.setBoolean(6, transaction.isCompleted());
        prep.execute();

        sharedResource.unlock();
    }

    public JobTransaction findJobTransactionById(int transactionId) throws InterruptedException, SQLException{
        JobTransaction transaction = null;
        String command = "select * from job_transactions where id=? order by date_published desc";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, transactionId);
        ResultSet rs = prep.executeQuery();
        if(rs.next()){
            JobTransaction.Builder builder = new JobTransaction.Builder()
                .setTransactionId(rs.getInt("id"))
                .setJobId(rs.getInt("job_id"))
                .setEmployeeId(rs.getInt("employee_id"))
                .setEmployerId(rs.getInt("employer_id"))
                .setCompleted(rs.getBoolean("completed"));
            Timestamp dateStarted = rs.getTimestamp("date_started");
            if(rs.wasNull())
                builder.setDateStarted(null);
            else 
                builder.setDateStarted(dateStarted.toLocalDateTime());
            Timestamp dateFinished = rs.getTimestamp("date_finished");
            if(rs.wasNull())
                builder.setDateFinished(null);
            else 
                builder.setDateFinished(dateFinished.toLocalDateTime());
            transaction = builder.build();
        }
        sharedResource.unlock();
        return transaction;
    }

    public ArrayList<JobTransaction> getAllJobTransactionsByJobId(int jobId) throws InterruptedException, SQLException{
        ArrayList<JobTransaction> transactions = new ArrayList<>();
        String command = "select * from job_transactions where job_id=? order by date_published desc";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, jobId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            JobTransaction.Builder builder = new JobTransaction.Builder();
            Timestamp dateStarted = rs.getTimestamp("date_started");
            if(!rs.wasNull()){
                builder.setDateStarted(dateStarted.toLocalDateTime());
            }
            Timestamp dateFinished = rs.getTimestamp("date_finished");
            if(!rs.wasNull()){
                builder.setDateFinished(dateFinished.toLocalDateTime());
            }
            transactions.add(builder
                .setTransactionId(rs.getInt("id"))
                .setJobId(rs.getInt("job_id"))
                .setEmployeeId(rs.getInt("employee_id"))
                .setEmployerId(rs.getInt("employer_id"))
                .setCompleted(rs.getBoolean("completed"))
                .build());
        }
        sharedResource.unlock();
        return transactions;
    }

    public ArrayList<JobTransaction> getAllJobTransactionsByEmployeeId(int employeeId) throws InterruptedException, SQLException{
        ArrayList<JobTransaction> transactions = new ArrayList<>();
        String command = "select * from job_transactions where employee_id=? order by date_published desc";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, employeeId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            JobTransaction.Builder builder = new JobTransaction.Builder();
            Timestamp dateStarted = rs.getTimestamp("date_started");
            if(!rs.wasNull()){
                builder.setDateStarted(dateStarted.toLocalDateTime());
            }
            Timestamp dateFinished = rs.getTimestamp("date_finished");
            if(!rs.wasNull()){
                builder.setDateFinished(dateFinished.toLocalDateTime());
            }
            transactions.add(builder
                .setTransactionId(rs.getInt("id"))
                .setJobId(rs.getInt("job_id"))
                .setEmployeeId(rs.getInt("employee_id"))
                .setEmployerId(rs.getInt("employer_id"))
                .setCompleted(rs.getBoolean("completed"))
                .build());
        }
        sharedResource.unlock();
        return transactions;
    }

    public ArrayList<JobTransaction> getAllJobTransactionsByEmployerId(int employerId) throws InterruptedException, SQLException{
        ArrayList<JobTransaction> transactions = new ArrayList<>();
        String command = "select * from job_transactions where employer_id=? order by date_published desc";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, employerId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            JobTransaction.Builder builder = new JobTransaction.Builder();
            Timestamp dateStarted = rs.getTimestamp("date_started");
            if(!rs.wasNull()){
                builder.setDateStarted(dateStarted.toLocalDateTime());
            }
            Timestamp dateFinished = rs.getTimestamp("date_finished");
            if(!rs.wasNull()){
                builder.setDateFinished(dateFinished.toLocalDateTime());
            }
            transactions.add(builder
                .setTransactionId(rs.getInt("id"))
                .setJobId(rs.getInt("job_id"))
                .setEmployeeId(rs.getInt("employee_id"))
                .setEmployerId(rs.getInt("employer_id"))
                .setCompleted(rs.getBoolean("completed"))
                .build());
        }
        sharedResource.unlock();
        return transactions;
    }

    public void startJob(JobTransaction transaction) throws InterruptedException, SQLException{
        String command = "update job_transactions set date_started=? where id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
        prep.setInt(2, transaction.getTransactionId());
        prep.execute();
        sharedResource.unlock();
    }

    public void markJobTransactionCompleted(int transactionId) throws InterruptedException, SQLException{
        String command = "update job_transactions set completed=?, date_finished=? where id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setBoolean(1, true);
        prep.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
        prep.setInt(3, transactionId);
        prep.execute();
        sharedResource.unlock();
    }

    public ArrayList<JobTransaction> getUserCompletedJobs(int userId) throws InterruptedException, SQLException{
        ArrayList<JobTransaction> jobs = new ArrayList<>();
        String command = "select * from job_transactions where employee_id=? and completed=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setBoolean(2, true);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            JobTransaction.Builder builder = new JobTransaction.Builder();
            Timestamp dateStarted = rs.getTimestamp("date_started");
            if(!rs.wasNull()){
                builder.setDateStarted(dateStarted.toLocalDateTime());
            }
            Timestamp dateFinished = rs.getTimestamp("date_finished");
            if(!rs.wasNull()){
                builder.setDateFinished(dateFinished.toLocalDateTime());
            }
            jobs.add(new JobTransaction.Builder()
                    .setTransactionId(rs.getInt("id"))
                    .setJobId(rs.getInt("job_id"))
                    .setEmployeeId(userId)
                    .setEmployerId(rs.getInt("employer_id"))
                    .setCompleted(rs.getBoolean("completed"))
                    .build());
        }
        sharedResource.unlock();
        return jobs;
    }

    public ArrayList<JobTransaction> getCompletedJobTransactions(int userId) throws InterruptedException, SQLException{
        ArrayList<JobTransaction> jobs = new ArrayList<>();
        String command = "select * from job_transactions where employee_id=? and completed=? or employer_id=? and completed=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setBoolean(2, true);
        prep.setInt(3, userId);
        prep.setBoolean(4, true);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            JobTransaction.Builder builder = new JobTransaction.Builder();
            Timestamp dateStarted = rs.getTimestamp("date_started");
            if(!rs.wasNull()){
                builder.setDateStarted(dateStarted.toLocalDateTime());
            }
            Timestamp dateFinished = rs.getTimestamp("date_finished");
            if(!rs.wasNull()){
                builder.setDateFinished(dateFinished.toLocalDateTime());
            }
            jobs.add(builder
                    .setTransactionId(rs.getInt("id"))
                    .setJobId(rs.getInt("job_id"))
                    .setEmployeeId(rs.getInt("employee_id"))
                    .setEmployerId(rs.getInt("employer_id"))
                    .setCompleted(rs.getBoolean("completed"))
                    .build());
        }
        sharedResource.unlock();
        return jobs;
    }

    public ArrayList<JobTransaction> getActiveJobTransactions(int userId) throws InterruptedException, SQLException{
        ArrayList<JobTransaction> jobs = new ArrayList<>();
        String command = "select * from job_transactions where employee_id=? and completed=? or employer_id=? and completed=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setBoolean(2, false);
        prep.setInt(3, userId);
        prep.setBoolean(4, false);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            JobTransaction.Builder builder = new JobTransaction.Builder();
            Timestamp dateStarted = rs.getTimestamp("date_started");
            boolean startedNull = false;
            boolean finishedNull = false;
            if(!rs.wasNull()){
                startedNull = true;
                builder.setDateStarted(dateStarted.toLocalDateTime());
            }
            Timestamp dateFinished = rs.getTimestamp("date_finished");
            if(!rs.wasNull()){
                finishedNull = true;
                builder.setDateFinished(dateFinished.toLocalDateTime());
            }
            if(!(startedNull && !finishedNull)){
                continue;
            }
            jobs.add(builder
                    .setTransactionId(rs.getInt("id"))
                    .setJobId(rs.getInt("job_id"))
                    .setEmployeeId(rs.getInt("employee_id"))
                    .setEmployerId(rs.getInt("employer_id"))
                    .setCompleted(rs.getBoolean("completed"))
                    .build());
        }
        sharedResource.unlock();
        return jobs;
    }
}
