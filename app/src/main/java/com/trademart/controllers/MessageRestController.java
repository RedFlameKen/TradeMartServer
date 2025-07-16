package com.trademart.controllers;

import static com.trademart.util.Logger.LogLevel.INFO;

import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.messaging.Convo;
import com.trademart.messaging.MessageController;
import com.trademart.messaging.chat.Chat;
import com.trademart.messaging.chat.MediaChat;
import com.trademart.messaging.chat.MessageChat;
import com.trademart.messaging.chat.PaymentChat;
import com.trademart.user.User;
import com.trademart.user.UserController;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

@RestController
public class MessageRestController extends RestControllerBase {

    private SharedResource sharedResource;
    private UserController userController;
    private MessageController messageController;

    public MessageRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        userController = new UserController(sharedResource);
        messageController = new MessageController(sharedResource);
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
        User user1 = userController.getUserFromDB(user1Id);
        if(user1 == null){
            return createBadRequestResponse("MessageRestController#sendChatMapping()", "no user with user1_id found");
        }
        User user2 = userController.getUserFromDB(user2Id);
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
        Chat chat = messageController.createChat(json, convo.getConvoId());
        try {
            messageController.writeChatToDB(chat);
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
                return ResponseEntity
                    .badRequest()
                    .body(createResponse("failed", "unable to send the message")
                            .toString());
        }
        return ResponseEntity.ok(createResponse("success", "successfully sent the message").toString());
    }

    @PostMapping("/message/fetch")
    public ResponseEntity<String> fetchMessagesMapping(@RequestBody String body){
        Logger.log("fetching...", INFO);
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(body));
        } catch (JSONException e){
            return createBadRequestResponse("MessageRestController#fetchMessageIdsMapping()", "json was badly formatted");
        }
        int user1Id = json.getInt("user1_id");
        int user2Id = json.getInt("user2_id");
        User user1 = userController.getUserFromDB(user1Id);
        if(user1 == null){
            return createBadRequestResponse("MessageRestController#sendChatMapping()", "no user with user1_id found");
        }
        User user2 = userController.getUserFromDB(user2Id);
        if(user2 == null){
            return createBadRequestResponse("MessageRestController#sendChatMapping()", "no user with user2_id found");
        }
        ArrayList<Chat> chats = null;
        try {
            Logger.log("getting convo", INFO);
            Convo convo = messageController.findConvoByUserIds(user1Id, user2Id);
            if(convo == null){
                return createBadRequestResponse("MessageRestController#sendChatMapping()", "no convo found with user1_id and user2_id found");
            }
            int receivedCount = json.getInt("received_count");
            chats = messageController.getChatsInConvo(convo.getConvoId(), receivedCount, receivedCount+10);
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
            return ResponseEntity
                .badRequest()
                .body(createResponse("failed", "unabled to fetch chats")
                        .toString());
        }

        JSONObject responseJson = messageController.chatArrayToJSON(chats);

        Logger.log("sending response...", INFO);
        return ResponseEntity
                .ok(createResponse("success", "successfully fetched chats")
                        .put("data", responseJson).toString());
    }

    @PostMapping("/message/convos")
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
            ArrayList<Convo> convos = messageController.findConvosByUserId(userId);
            for (Convo convo : convos) {
                User user1 = userController.getUserFromDB(convo.getUser1Id());
                User user2 = userController.getUserFromDB(convo.getUser2Id());
                int secondUserId = userId == user1.getId() ? user2.getId() : user1.getId();
                String username = userId == user1.getId() ? user2.getUsername() : user1.getUsername();
                Chat lastChat = messageController.getLastChat(convo.getConvoId());
                JSONObject chatJson = null;
                switch (lastChat.getType()) {
                    case MEDIA:
                        chatJson = ((MediaChat)lastChat).parseJson();
                        break;
                    case MESSAGE:
                        chatJson = ((MessageChat)lastChat).parseJson();
                        break;
                    case PAYMENT:
                        chatJson = ((PaymentChat)lastChat).parseJson();
                        break;
                }
                JSONObject convoJson = new JSONObject()
                    .put("user_id", secondUserId)
                    .put("username", username)
                    .put("convo_id", convo.getConvoId())
                    .put("last_chat", chatJson);

                convosJson.append("convos", convoJson);
            }
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

}
