package com.trademart.controllers;

import static com.trademart.util.Logger.LogLevel.INFO;

import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.job.JobController;
import com.trademart.job.JobListing;
import com.trademart.payment.JobPayment;
import com.trademart.payment.Payment;
import com.trademart.payment.PaymentController;
import com.trademart.payment.PaymentType;
import com.trademart.payment.ServicePayment;
import com.trademart.service.Service;
import com.trademart.service.ServiceController;
import com.trademart.util.Logger;

@RestController
public class PaymentRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private PaymentController paymentController;
    private ServiceController serviceController;
    private JobController jobController;

    public PaymentRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        serviceController = new ServiceController(sharedResource);
        paymentController = new PaymentController(sharedResource);
        jobController = new JobController(sharedResource);
    }

    @PostMapping("/payment/fetch")
    public ResponseEntity<String> getPaymentByIdMapping(@RequestBody String body){
        JSONObject json = null;
        Payment payment = null;
        int paymentId = -1;
        PaymentType paymentType = null;
        try {
            json = new JSONObject(new JSONTokener(body));
            paymentId = json.getInt("payment_id");
            paymentType = PaymentType.parse(json.getString("type"));
        } catch (JSONException e){
            return badRequestResponse("request was badly formatted");
        }
        try {
            payment = paymentController.findPaymentById(paymentId, paymentType);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("unable to get payment details");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse("unable to get payment details");
        }
        if(payment == null){
            return ResponseEntity.ok(createResponse("success", "payment not found").toString());
        }
        JSONObject responseJson = null;
        try {

            if(payment instanceof ServicePayment){
                Service service = serviceController.findServiceByID(((ServicePayment)payment).getServiceId());
                responseJson = createServicePaymentResponseEntity((ServicePayment)payment, service);
            }
            if(payment instanceof JobPayment){
                JobListing job = jobController.findJobByID(((JobPayment)payment).getJobId());
                responseJson = createJobPaymentResponseEntity((JobPayment)payment, job);
            }
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("unable to get payment details");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse("unable to get payment details");
        }

        return ResponseEntity.ok(createResponse("success", "found the payment")
                .put("entity", responseJson).toString());
    }

    @PostMapping("/payment/confirm")
    public ResponseEntity<String> confirmPaymentMapping(@RequestBody String body){
        JSONObject json = null;
        int paymentId = -1;
        PaymentType paymentType;
        try {
            json = new JSONObject(new JSONTokener(body));
            paymentId = json.getInt("payment_id");
            paymentType = PaymentType.parse(json.getString("type"));
        } catch (JSONException e){
            return badRequestResponse("request was badly formatted");
        }
        Payment payment;
        try {
            payment = paymentController.findPaymentById(paymentId, paymentType);
            if(payment == null){
                return ResponseEntity.ok(createResponse("failed", "no such payment exists").toString());
            }
            paymentController.confirmPayment(paymentId);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("failed to confirm payment");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse("failed to confirm payment");
        }
        return ResponseEntity.ok(createResponse("success", "the payment is confirmed")
                .put("payment", payment.parseJson()).toString());
    }

    @PostMapping("/payment/create/service")
    public ResponseEntity<String> createServicePaymentMapping(@RequestBody String body){
        JSONObject json = null;
        ServicePayment payment = null;
        try {
            json = new JSONObject(new JSONTokener(body));
            payment = paymentController.createServicePayment(json);
        } catch (JSONException e){
            return badRequestResponse("request was badly formatted");
        }
        JSONObject responseJson = null;
        try {
            paymentController.writePaymentToDB(payment);
            Service service = serviceController.findServiceByID(payment.getServiceId());
            if(service == null) return notFoundResponse();
            responseJson = createServicePaymentResponseEntity(payment, service);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("unable to make a payment");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse("unable to make a payment");
        }
        return ResponseEntity.ok(createResponse("success", "created the service payment")
                .put("entity", responseJson).toString());
    }

    @PostMapping("/payment/create/job")
    public ResponseEntity<String> createJobPaymentMapping(@RequestBody String body){
        JobPayment payment = null;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            payment = paymentController.createJobPayment(json);
        } catch (JSONException e){
            e.printStackTrace();
            return badRequestResponse("request was badly formatted");
        }
        JSONObject responseJson = null;
        try {
            paymentController.writePaymentToDB(payment);
            JobListing job = jobController.findJobByID(payment.getJobId());
            if(job == null) {
                Logger.log("not found response for /payment/create/job", INFO);
                return notFoundResponse();
            };
            responseJson = createJobPaymentResponseEntity(payment, job);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse("unable to make a payment");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse("unable to make a payment");
        }
        return ResponseEntity.ok(createResponse("success", "created the job payment")
                .put("entity", responseJson).toString());
    }


    private JSONObject createServicePaymentResponseEntity(ServicePayment payment, Service service){
        JSONObject serviceJson = service.parseJson();
        JSONObject paymentJson = payment.parseJson();
        paymentJson.put("paying_for", serviceJson);
        return paymentJson;
    }

    private JSONObject createJobPaymentResponseEntity(JobPayment payment, JobListing job){
        JSONObject jobJson = job.parseJson();
        JSONObject paymentJson = payment.parseJson();
        paymentJson.put("paying_for", jobJson);
        return paymentJson;
    }

}
