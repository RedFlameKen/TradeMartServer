package com.trademart.payment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.db.IDGenerator;

public class PaymentController {

    private SharedResource sharedResource;
    private DatabaseController dbController;

    public PaymentController(SharedResource sharedResource) {
        this.sharedResource = sharedResource;
        dbController = sharedResource.getDatabaseController();
    }

    // TODO: Method for generating a json response of a payment containing complete information about service and job payments

    public int generatePaymentID() {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "payments", "payment_id");
        sharedResource.unlock();
        return id;
    }

    public Payment findPaymentById(int id) throws InterruptedException, SQLException {
        sharedResource.lock();
        ServicePayment servicePayment = findServicePaymentById(id);
        if(servicePayment != null){
            sharedResource.unlock();
            return servicePayment;
        }
        JobPayment jobPayment = findJobPaymentById(id);
        if(jobPayment != null){
            sharedResource.unlock();
            return jobPayment;
        }
        return null;
    }

    public Payment findPaymentById(int id, PaymentType type) throws InterruptedException, SQLException {
        Payment payment = null;
        sharedResource.lock();
        switch (type) {
            case JOB:
                payment = findJobPaymentById(id);
                break;
            case SERVICE:
                payment = findServicePaymentById(id);
                break;
        }
        sharedResource.unlock();
        return payment;
    }

    private ServicePayment findServicePaymentById(int id) throws InterruptedException, SQLException {
        String command = "select * from payments join service_payment on payments.payment_id = service_payment.payment_id where payments.payment_id=?";
        PreparedStatement prep = dbController.prepareStatement(command);

        prep.setInt(1, id);
        ResultSet rs = prep.executeQuery();
        if (rs.next()) {
            return new ServicePayment.Builder()
                    .setPaymentId(id)
                    .setType(PaymentType.SERVICE)
                    .setAmount(rs.getInt("amount"))
                    .setConfirmed(rs.getBoolean("is_confirmed"))
                    .setSenderId(rs.getInt("sender_id"))
                    .setReceiverId(rs.getInt("receiver_id"))
                    .setServiceId(rs.getInt("service_id"))
                    .build();
        }

        return null;
    }

    private JobPayment findJobPaymentById(int id) throws InterruptedException, SQLException {
        String command = "select * from payments join job_payment on payments.payment_id = job_payment.payment_id where payment_id=?";
        PreparedStatement prep = dbController.prepareStatement(command);

        prep.setInt(1, id);
        ResultSet rs = prep.executeQuery();
        if (rs.next()) {
            return new JobPayment.Builder()
                    .setPaymentId(id)
                    .setType(PaymentType.JOB)
                    .setAmount(rs.getInt("amount"))
                    .setConfirmed(rs.getBoolean("is_confirmed"))
                    .setSenderId(rs.getInt("sender_id"))
                    .setReceiverId(rs.getInt("receiver_id"))
                    .setJobId(rs.getInt("job_id"))
                    .build();
        }

        return null;
    }

    public Payment createPayment(JSONObject json, PaymentType type) throws JSONException {
        Payment.Builder builder = new Payment.Builder()
                .setPaymentId(generatePaymentID())
                .setType(type)
                .setAmount(json.getDouble("amount"))
                .setConfirmed(json.getBoolean("is_confirmed"))
                .setSenderId(json.getInt("sender_id"))
                .setReceiverId(json.getInt("receiver_id"));

        switch (type) {
            case JOB:
                return createJobPayment(builder, json);
            case SERVICE:
                return createServicePayment(builder, json);
        }
        return null;
    }

    public ServicePayment createServicePayment(JSONObject json){
        return new ServicePayment.Builder()
                .setPaymentId(generatePaymentID())
                .setType(PaymentType.SERVICE)
                .setAmount(json.getDouble("amount"))
                .setConfirmed(false)
                .setSenderId(json.getInt("sender_id"))
                .setReceiverId(json.getInt("receiver_id"))
                .setServiceId(json.getInt("service_id"))
                .build();
    }

    public JobPayment createJobPayment(JSONObject json){
        return new JobPayment.Builder()
                .setPaymentId(generatePaymentID())
                .setType(PaymentType.JOB)
                .setAmount(json.getDouble("amount"))
                .setConfirmed(json.getBoolean("is_confirmed"))
                .setSenderId(json.getInt("sender_id"))
                .setReceiverId(json.getInt("receiver_id"))
                .setJobId(json.getInt("job_id"))
                .build();
    }

    public ServicePayment createServicePayment(Payment.Builder builder, JSONObject json) throws JSONException {
        ServicePayment.Builder nBuilder = ServicePayment.Builder.of(builder)
                .setServiceId(json.getInt("service_id"));
        return nBuilder.build();
    }

    public JobPayment createJobPayment(Payment.Builder builder, JSONObject json) throws JSONException {
        JobPayment.Builder nBuilder = JobPayment.Builder.of(builder)
                .setJobId(json.getInt("service_id"));
        return nBuilder.build();
    }

    public void writePaymentToDB(Payment payment) throws InterruptedException, SQLException {
        String command = "insert into payments(payment_id,type,amount,is_confirmed,sender_id,receiver_id)values(?,?,?,?,?,?)";
        sharedResource.lock();
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, payment.getPaymentId());
        prep.setString(2, payment.getType().toString());
        prep.setDouble(3, payment.getAmount());
        prep.setBoolean(4, payment.isConfirmed());
        prep.setInt(5, payment.getSenderId());
        prep.setInt(6, payment.getReceiverId());
        prep.execute();
        prep.close();

        if (payment instanceof ServicePayment) {
            writeServicePaymentToDB((ServicePayment) payment);
        }
        if (payment instanceof JobPayment) {
            writeJobPaymentToDB((JobPayment) payment);
        }

        sharedResource.unlock();
    }

    public void confirmPayment(int paymentId) throws InterruptedException, SQLException{
        String command = "update payments set is_confirmed=1 where payment_id=?";
        sharedResource.lock();

        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, paymentId);
        prep.execute();
        prep.close();

        sharedResource.unlock();
    }

    private void writeServicePaymentToDB(ServicePayment payment) throws SQLException {
        String command = "insert into service_payment(payment_id,service_id)values(?,?)";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, payment.getPaymentId());
        prep.setInt(2, payment.getServiceId());
        prep.execute();
        prep.close();
    }

    private void writeJobPaymentToDB(JobPayment payment) throws SQLException {
        String command = "insert into job_payment(payment_id,job_id)values(?,?)";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, payment.getPaymentId());
        prep.setInt(2, payment.getJobId());
        prep.execute();
        prep.close();
    }

}
