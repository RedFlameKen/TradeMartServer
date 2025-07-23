package com.trademart.controllers;

import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.feed.FeedType;
import com.trademart.report.Report;
import com.trademart.report.ReportController;

@RestController
public class ReportRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private ReportController reportController;

    public ReportRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        reportController = new ReportController(sharedResource);
    }

    @PostMapping("/report/create")
    public ResponseEntity<String> createReportMapping(@RequestBody String body){
        Report report = null;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            report = new Report.Builder()
                .setMessage(json.getString("message"))
                .setType(FeedType.parse(json.getString("type")))
                .setUserId(json.getInt("user_id"))
                .setTargetId(json.getInt("target_id"))
                .build();
        } catch (JSONException e) {
            e.printStackTrace();
            return badRequestResponse("received a bad request");
        }
        int id = -1;
        try {
            id = reportController.writeReportToDB(report);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        JSONObject json = new JSONObject()
            .put("report_id", id)
            .put("user_id", report.getUserId())
            .put("target_id", report.getTargetId())
            .put("message", report.getMessage())
            .put("type", report.getType().toString());
        return ResponseEntity.ok(createResponse("success", "report sent")
                .put("data", json)
                .toString());
    }
    
    @GetMapping("/report/fetch/{report_id}")
    public ResponseEntity<String> fetchReportMapping(@PathVariable("report_id") int reportId){
        Report report;
        try {
            report = reportController.findReportById(reportId);
        } catch (SQLException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        if(report == null){
            return notFoundResponse();
        }
        return ResponseEntity.ok(createResponse("success", "fetched report")
                .toString());
    }

}
