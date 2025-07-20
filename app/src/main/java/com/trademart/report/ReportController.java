package com.trademart.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.db.IDGenerator;
import com.trademart.feed.FeedType;

public class ReportController {

    private SharedResource sharedResource;
    private DatabaseController dbController;

    public ReportController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        dbController = sharedResource.getDatabaseController();
    }

    public int generateReportID(){
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "reports", "report_id");
        sharedResource.unlock();
        return id;
    }

    public Report findReportById(int reportId) throws SQLException, InterruptedException{
        Report report = null;
        String command = "select * from reports where report_id=?";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, reportId);
        ResultSet rs = prep.executeQuery();
        if(rs.next()){
            report = new Report.Builder()
                .setReportId(rs.getInt("report_id"))
                .setMessage(rs.getString("message"))
                .setUserId(rs.getInt("user_id"))
                .setTargetId(rs.getInt("target_id"))
                .setType(FeedType.parse(rs.getString("type")))
                .build();
        }
        sharedResource.unlock();
        return report;
    }

    public int writeReportToDB(Report report) throws InterruptedException, SQLException{
        String command = "insert into reports(report_id, message, user_id, type, target_id)values(?,?,?,?,?)";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        int id = generateReportID();
        prep.setInt(1, id);
        prep.setString(2, report.getMessage());
        prep.setInt(3, report.getUserId());
        prep.setString(4, report.getType().toString());
        prep.setInt(5, report.getTargetId());
        prep.execute();
        sharedResource.unlock();
        return id;

    }
    
}
