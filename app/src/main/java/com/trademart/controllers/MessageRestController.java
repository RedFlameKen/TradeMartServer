package com.trademart.controllers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.messaging.Message;
import com.trademart.messaging.MessageCollection;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

@RestController
@RequestMapping("/message")
public class MessageRestController {

    private ArrayList<Message> messages = new ArrayList<>();
    private SharedResource sharedResource;

    public MessageRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
    }

    @PostMapping("/send")
    public String receiveMessage(@RequestBody Message message){
        storeMessage(message);
        return "Message Sent!";
    }

    private void storeMessage(Message message) throws ResponseStatusException {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for storing message", LogLevel.WARNING);
            throw new ResponseStatusException(500, "Internal Server Error", e);
        }
        DatabaseController dbController = sharedResource.getDatabaseController();

        try {
            PreparedStatement prep = dbController.prepareStatement("insert into messages(username, message, time_sent) values (?, ?, ?)");
            prep.setString(1, message.getUsername());
            prep.setString(2, message.getMessage());
            LocalDateTime timeSent = LocalDateTime.parse(message.getTimeSent());
            prep.setTimestamp(3, Timestamp.valueOf(timeSent));

            prep.execute();
        } catch (SQLException e) {
            Logger.log("Unable to store message in database", LogLevel.WARNING);
            e.printStackTrace();
            throw new ResponseStatusException(500, "Internal Server Error", e);
        }

        sharedResource.unlock();
    }

    @GetMapping("/fetch")
    public MessageCollection distributeMessages(){
        return fetchMessages();
    }

    private MessageCollection fetchMessages(){
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            Logger.log("Unable to lock resources for fetching messages", LogLevel.WARNING);
            throw new ResponseStatusException(500, "Internal Server Error", e);
        }
        MessageCollection messageCollection;
        ArrayList<Message> messages = new ArrayList<>();
        DatabaseController dbController = sharedResource.getDatabaseController();
        try {
            ResultSet rs = dbController.execQuery("select * from messages");
            while(rs.next()){
                Message message = new Message();
                message.setUsername(rs.getString("username"));
                message.setMessage(rs.getString("message"));
                message.setTimeSent(rs.getString("time_sent"));
                messages.add(message);
            }
        } catch (SQLException e) {
            Logger.log("Unable to fetch messages!", LogLevel.WARNING);
            throw new ResponseStatusException(500, "Internal Server Error", e);
        }
        messageCollection = new MessageCollection(messages);
        sharedResource.unlock();
        return messageCollection;
    }

}
