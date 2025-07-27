package com.trademart.controllers;

import static com.trademart.util.Logger.LogLevel.INFO;

import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONArray;
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
import com.trademart.messaging.Convo;
import com.trademart.messaging.MessageController;
import com.trademart.messaging.chat.Chat;
import com.trademart.messaging.chat.ChatType;
import com.trademart.messaging.chat.MediaChat;
import com.trademart.messaging.chat.MessageChat;
import com.trademart.messaging.chat.PaymentChat;
import com.trademart.payment.JobPayment;
import com.trademart.payment.Payment;
import com.trademart.payment.PaymentController;
import com.trademart.payment.PaymentType;
import com.trademart.payment.ServicePayment;
import com.trademart.service.Service;
import com.trademart.service.ServiceController;
import com.trademart.user.User;
import com.trademart.user.UserController;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

@RestController
public class MessageRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private UserController userController;
    private MessageController messageController;
    private PaymentController paymentController;
    private ServiceController serviceController;
    private JobController jobController;

    public MessageRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        userController = new UserController(sharedResource);
        messageController = new MessageController(sharedResource);
        paymentController = new PaymentController(sharedResource);
        serviceController = new ServiceController(sharedResource);
        jobController = new JobController(sharedResource);
    }

    @PostMapping("/message/send")
    public ResponseEntity<String> sendChatMapping(@RequestBody String body){
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(body));
        } catch (JSONException e){
            return createBadRequestResponse("MessageRestController#sendChatMapping()", "json was badly formatted");
        }
        int user1Id = json.getInt("user1_id");
        int user2Id = json.getInt("user2_id");
        User user1;
        User user2;
        try {
            user1 = userController.findUserById(user1Id);
            user2 = userController.findUserById(user2Id);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        if(user1 == null){
            return createBadRequestResponse("MessageRestController#sendChatMapping()", "no user with user1_id found");
        }
        if(user2 == null){
            return createBadRequestResponse("MessageRestController#sendChatMapping()", "no user with user2_id found");
        }
        Convo convo;
        try {
            convo = messageController.findConvoByUserIds(user1Id, user2Id);
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(createResponse("failed", "unable to send chat").toString());
        }
        if(convo == null){
            try {
                convo = messageController.initConvo(user1Id, user2Id);
            } catch (InterruptedException | SQLException e) {
                e.printStackTrace();
                return ResponseEntity
                    .badRequest()
                    .body(createResponse("failed", "an error happened in the server")
                            .toString());
            }
        }
        Chat chat;
        try {
            chat = messageController.createChat(json, convo.getConvoId());
            messageController.writeChatToDB(chat);
        } catch (JSONException e) {
            return createBadRequestResponse("MessageRestController#sendChatMapping", "the sent json was badly formatted");
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
                return ResponseEntity
                    .badRequest()
                    .body(createResponse("failed", "unable to send the message")
                            .toString());
        }
        JSONObject chatJson = null;
        try {
            switch (chat.getType()) {
                case MEDIA:
                    chatJson = ((MediaChat)chat).parseJson();
                    break;
                case MESSAGE:
                    chatJson = ((MessageChat)chat).parseJson();
                    break;
                case PAYMENT:
                    chatJson = expoundPaymentChatJson((PaymentChat)chat)
                        .put("type", ChatType.PAYMENT.toString());
                    break;

            }
        } catch (InterruptedException e){
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e){
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "successfully sent the message")
                .put("data", chatJson)
                .toString());
    }

    @PostMapping("/message/fetch")
    public ResponseEntity<String> fetchMessagesMapping(@RequestBody String body){
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(body));
        } catch (JSONException e){
            e.printStackTrace();
            return createBadRequestResponse("MessageRestController#fetchMessagesMapping()", "json was badly formatted");
        }
        int user1Id = json.getInt("user1_id");
        int user2Id = json.getInt("user2_id");
        User user1;
        User user2;
        try {
            user1 = userController.findUserById(user1Id);
            user2 = userController.findUserById(user2Id);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        if(user1 == null){
            return createBadRequestResponse("MessageRestController#fetchMessagesMapping()", "no user with user1_id found");
        }
        if(user2 == null){
            return createBadRequestResponse("MessageRestController#fetchMessagesMapping()", "no user with user2_id found");
        }
        JSONObject responseJson = null;
        try {
            Convo convo = messageController.findConvoByUserIds(user1Id, user2Id);
            if(convo == null){
                convo = messageController.initConvo(user1Id, user2Id);
                // return createBadRequestResponse("MessageRestController#fetchMessagesMapping()", "no convo found with user1_id and user2_id found");
            }
            ArrayList<Chat> chats = messageController.getAllChatsInConvo(convo.getConvoId());
            responseJson = chatArrayToJSON(chats);
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
            return ResponseEntity
                .badRequest()
                .body(createResponse("failed", "unabled to fetch chats")
                        .toString());
        }


        return ResponseEntity
                .ok(createResponse("success", "successfully fetched chats")
                        .put("data", responseJson).toString());
    }

    @PostMapping("/message/convos")
    public ResponseEntity<String> fetchConvoIdByUsersMapping(@RequestBody String body){
        int user1Id = -1;
        int user2Id = -1;
        try {
            JSONObject json = new JSONObject(new JSONTokener(body));
            user1Id = json.getInt("user1_id");
            user2Id = json.getInt("user2_id");
        } catch (JSONException e){
            return createBadRequestResponse("MessageRestController#fetchMessageIdsMapping()", "json was badly formatted");
        }
        User user1;
        User user2;
        try {
            user1 = userController.findUserById(user1Id);
            user2 = userController.findUserById(user2Id);
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        if(user1 == null){
            return createBadRequestResponse("MessageRestController#fetchMessagesMapping()", "no user with user1_id found");
        }
        if(user2 == null){
            return createBadRequestResponse("MessageRestController#fetchMessagesMapping()", "no user with user2_id found");
        }
        Convo convo;
        try {
            convo = messageController.findConvoByUserIds(user1Id, user2Id);
            if(convo == null){
                convo = messageController.initConvo(user1Id, user2Id);
            }
        } catch (InterruptedException e) {
            sharedResource.unlock();
            e.printStackTrace();
            return internalServerErrorResponse();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerErrorResponse();
        }
        return ResponseEntity.ok(createResponse("success", "conversation found")
                .put("data", new JSONObject().put("convo_id", convo.getConvoId()))
                .toString());
    }

    @PostMapping("/message/convos/user")
    public ResponseEntity<String> fetchUserConvosMapping(@RequestBody String body){
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(body));
        } catch (JSONException e){
            return createBadRequestResponse("MessageRestController#fetchMessageIdsMapping()", "json was badly formatted");
        }
        int userId = json.getInt("user_id");
        JSONObject convosJson = new JSONObject();
        try {
            JSONArray convosArr = new JSONArray();
            ArrayList<Convo> convos = messageController.findConvosByUserId(userId);
            for (Convo convo : convos) {
                User user1 = userController.findUserById(convo.getUser1Id());
                User user2 = userController.findUserById(convo.getUser2Id());
                int secondUserId = userId == user1.getId() ? user2.getId() : user1.getId();
                String username = userId == user1.getId() ? user2.getUsername() : user1.getUsername();
                Chat lastChat = messageController.getLastChat(convo.getConvoId());
                if(lastChat == null){
                    Logger.log("null chat between " + user1.getUsername() + " and " + user2.getUsername(), INFO);
                    continue;
                }
                JSONObject chatJson = null;
                if(lastChat instanceof MediaChat){
                    chatJson = ((MediaChat)lastChat).parseJson();
                } else if(lastChat instanceof MessageChat){
                    chatJson = ((MessageChat)lastChat).parseJson();
                } else if(lastChat instanceof PaymentChat){
                    chatJson = ((PaymentChat)lastChat).parseJson();
                } 
                JSONObject convoJson = new JSONObject()
                    .put("user_id", secondUserId)
                    .put("username", username)
                    .put("convo_id", convo.getConvoId())
                    .put("last_chat", chatJson);

                convosArr.put(convoJson);
            }
            convosJson.put("convos", convosArr);
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(createResponse("failed", "unable to load conversations").toString());
        }

        JSONObject responseJson = createResponse("success", "successfully fetched convos")
            .put("data", convosJson);
        return ResponseEntity.ok(responseJson.toString());
    }

    private ResponseEntity<String> createBadRequestResponse(String codeLocation, String message){
        Logger.log("Received a bad request to " + codeLocation,
                LogLevel.WARNING);
        return ResponseEntity
            .badRequest()
            .body(createResponse("failed", message)
                    .toString());
    }

    public JSONObject chatArrayToJSON(ArrayList<Chat> chats) throws InterruptedException, SQLException{
        JSONObject json = new JSONObject();
        JSONArray arr = new JSONArray();
        for (Chat chat : chats) {
            JSONObject chatJson = new JSONObject()
                .put("chat_id", chat.getChatId())
                .put("time_sent", chat.getTimeSent())
                .put("sender_id", chat.getSenderId())
                .put("convo_id", chat.getConvoId())
                .put("type", chat.getType());
            switch (chat.getType()) {
                case MEDIA:
                    chatJson.put("media_id", ((MediaChat)chat).getMediaId());
                    break;
                case MESSAGE:
                    chatJson.put("message", ((MessageChat)chat).getMessage());
                    break;
                case PAYMENT:
                    chatJson = expoundPaymentChatJson((PaymentChat)chat);
                    break;
            }
            arr.put(chatJson);
            // json.append("chats", chatJson);
        }
        json.put("chats", arr);
        return json;
    }

    private JSONObject expoundPaymentChatJson(PaymentChat chat) throws InterruptedException, SQLException{
        JSONObject chatJson = chat.parseJson();
        Payment payment = paymentController.findPaymentById(chat.getPaymentId());
        chatJson.put("payment_id", ((PaymentChat)chat).getPaymentId())
            .put("amount", payment.getAmount())
            .put("is_confirmed", payment.isConfirmed());

        if(payment instanceof ServicePayment){
            Service service = serviceController.findServiceByID(
                    ((ServicePayment)payment).getServiceId());
            chatJson.put("payment_reason", service.getServiceTitle())
                .put("payment_type", PaymentType.SERVICE.toString());
        }
        if(payment instanceof JobPayment){
            JobListing job = jobController.findJobByID(((JobPayment)payment).getJobId());
            chatJson.put("payment_reason", job.getTitle())
                .put("payment_type", PaymentType.JOB.toString());
        }
        return chatJson;
    }

}
