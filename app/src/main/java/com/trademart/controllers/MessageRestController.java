package com.trademart.controllers;

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
        Convo convo = messageController.findConvoByID(json.getInt("convo_id"));
        if(convo == null){
            try {
                messageController.initConvo(user1Id, user2Id);
            } catch (InterruptedException | SQLException e) {
                e.printStackTrace();
                return ResponseEntity
                    .badRequest()
                    .body(createResponse("failed", "an error happened in the server")
                            .toString());
            }
        }
        Chat chat = messageController.createChat(json);
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

    @PostMapping("/message")
    public ResponseEntity<String> fetchMessageIdsMapping(@RequestBody String body){
        JSONObject json = null;
        try {
            json = new JSONObject(new JSONTokener(body));
        } catch (JSONException e){
            return createBadRequestResponse("MessageRestController#fetchMessageIdsMapping()", "json was badly formatted");
        }
        int userId = json.getInt("user_id");
        User user = userController.getUserFromDB(userId);
        if(user == null){
            return createBadRequestResponse("MessageRestController#fetchMessageIdsMapping()", "no user with user_id found");
        }
        int convoId = json.getInt("convo_id");
        int receivedCount = json.getInt("received_count");
        ArrayList<Chat> chats = null;
        try {
            chats = messageController.getChatsInConvo(convoId, receivedCount, receivedCount+10);
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
            return ResponseEntity
                .badRequest()
                .body(createResponse("failed", "unabled to fetch chats")
                        .toString());
        }

        JSONObject responseJson = messageController.chatArrayToJSON(chats);

        return ResponseEntity.ok(responseJson.toString());
    }

    private ResponseEntity<String> createBadRequestResponse(String codeLocation, String message){
        Logger.log("Received a bad request to \"/message/send\" " + codeLocation,
                LogLevel.WARNING);
        return ResponseEntity
            .badRequest()
            .body(createResponse("failed", message)
                    .toString());
    }

}
