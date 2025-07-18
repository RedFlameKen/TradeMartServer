package com.trademart.messaging;


import static com.trademart.util.Logger.LogLevel.INFO;

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
import com.trademart.messaging.chat.Chat;
import com.trademart.messaging.chat.ChatType;
import com.trademart.messaging.chat.MediaChat;
import com.trademart.messaging.chat.MessageChat;
import com.trademart.messaging.chat.PaymentChat;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

public class MessageController {

    private SharedResource sharedResource;
    private DatabaseController dbController;

    public MessageController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        dbController = sharedResource.getDatabaseController();
    }

    public int generateChatID(){
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "chats", "chat_id");
        sharedResource.unlock();
        return id;
    }

    public int generateConvoID(){
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "convos", "convo_id");
        sharedResource.unlock();
        return id;
    }

    public Chat createChat(JSONObject json, int convoId)throws JSONException{
        ChatType type = ChatType.parse(json.getString("type"));
        Logger.log("type: " + type.toString(), INFO);
        Chat.Builder builder = new Chat.Builder()
            .setChatId(generateChatID())
            .setTimeSent(LocalDateTime.now())
            .setType(type)
            .setSenderId(json.getInt("sender_id"))
            .setConvoId(convoId);
        switch (type) {
            case MEDIA:
                return createMediaChat(builder, json);
            case MESSAGE:
                return createMessageChat(builder, json);
            case PAYMENT:
                return createPaymentChat(builder, json);
            default:
                return null;
        }
    }

    private MessageChat createMessageChat(Chat.Builder builder, JSONObject json)throws JSONException{
        MessageChat.Builder nBuilder = MessageChat.Builder.of(builder)
            .setMessage(json.getString("message"));
        return nBuilder.build();
    }

    private PaymentChat createPaymentChat(Chat.Builder builder, JSONObject json)throws JSONException{
        PaymentChat.Builder nBuilder = PaymentChat.Builder.of(builder)
            .setPaymentId(json.getInt("payment_id"));
        return nBuilder.build();
    }

    private MediaChat createMediaChat(Chat.Builder builder, JSONObject json)throws JSONException{
        MediaChat.Builder nBuilder = MediaChat.Builder.of(builder)
            .setMediaId(json.getInt("media_id"));
        return nBuilder.build();
    }

    public void writeChatToDB(Chat chat) throws InterruptedException, SQLException{
        sharedResource.lock();

        String command = "insert into chats(chat_id,type,time_sent,sender_id,convo_id)values(?,?,?,?,?)";

        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, chat.getChatId());
        prep.setString(2, chat.getType().toString());
        prep.setTimestamp(3, Timestamp.valueOf(chat.getTimeSent()));
        prep.setInt(4, chat.getSenderId());
        prep.setInt(5, chat.getConvoId());
        prep.execute();

        if(chat instanceof MessageChat){
            writeMessageChatToDB((MessageChat) chat);
        }
        if(chat instanceof PaymentChat){
            writePaymentChatToDB((PaymentChat) chat);
        }
        if(chat instanceof MediaChat){
            writeMediaChatToDB((MediaChat) chat);
        }

        sharedResource.unlock();
    }

    public void writePaymentChatToDB(PaymentChat chat) throws SQLException {
        String command = "insert into payment_chat(chat_id,payment_id)values(?,?)";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, chat.getChatId());
        prep.setInt(2, chat.getPaymentId());
        prep.execute();
    }
    
    public void writeMediaChatToDB(MediaChat chat) throws SQLException {
        String command = "insert into media_chat(chat_id,media_id)values(?,?)";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, chat.getChatId());
        prep.setInt(2, chat.getMediaId());
        prep.execute();
    }
    
    public void writeMessageChatToDB(MessageChat chat) throws SQLException {
        String command = "insert into message_chat(chat_id,message)values(?,?)";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, chat.getChatId());
        prep.setString(2, chat.getMessage());
        prep.execute();
    }
    
    private int getConvoRowCount(int convoId){
        return dbController.getCommandRowCount("select * from convos where convo_id="+convoId);
    }

    private boolean hasConvo(int user1Id, int user2Id) throws InterruptedException, SQLException{
        String command = "select * from convos where user1_id=? and user2_id=? or user1_id=? and user2_id=?";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, user1Id);
        prep.setInt(2, user2Id);
        prep.setInt(3, user2Id);
        prep.setInt(4, user1Id);
        ResultSet rs = prep.executeQuery();
        boolean result = false;
        if(rs.isBeforeFirst())
            if(rs.next()) result = true;
        return result;
    }


    public Convo findConvoByID(int convoId) {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(getConvoRowCount(convoId) < 1){
            return null;
        }
        Convo convo = null;
        try {
            PreparedStatement prep = sharedResource.getDatabaseController()
                .prepareStatement("select * from convos where convo_id=?");
            prep.setInt(1, convoId);
            ResultSet rs = prep.executeQuery();
            rs.next();
            convo = new Convo.Builder()
                .setConvoId(convoId)
                .setUser1Id(rs.getInt("user1_id"))
                .setUser2Id(rs.getInt("user2_id"))
                .build();
        } catch (SQLException e) {
            Logger.log("Unable to find a convo with the id " + convoId, LogLevel.WARNING);
        }

        sharedResource.unlock();
        return convo;
    }

    public Convo findConvoByUserIds(int user1Id, int user2Id) throws InterruptedException, SQLException{
        sharedResource.lock();

        if(!hasConvo(user1Id, user2Id)) {
            sharedResource.unlock();
            return null;
        }
        Convo convo = null;
        PreparedStatement prep = sharedResource.getDatabaseController()
            .prepareStatement("select * from convos where user1_id=? and user2_id=? or user1_id=? and user2_id=?");
        prep.setInt(1, user1Id);
        prep.setInt(2, user2Id);
        prep.setInt(3, user2Id);
        prep.setInt(4, user1Id);
        ResultSet rs = prep.executeQuery();
        rs.next();
        convo = new Convo.Builder()
            .setConvoId(rs.getInt("convo_id"))
            .setUser1Id(user1Id)
            .setUser2Id(user2Id)
            .build();

        sharedResource.unlock();
        return convo;

    }

    public ArrayList<Convo> findConvosByUserId(int userId) throws InterruptedException, SQLException{
        ArrayList<Convo> convos = new ArrayList<>();
        sharedResource.lock();
        String command = "select * from convos where user1_id=? or user2_id=?";

        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, userId);
        prep.setInt(2, userId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            convos.add(new Convo.Builder()
                    .setConvoId(rs.getInt("convo_id"))
                    .setUser1Id(rs.getInt("user1_id"))
                    .setUser2Id(rs.getInt("user2_id"))
                    .build());
        }

        sharedResource.unlock();
        return convos;
    }

    public Convo initConvo(int user1Id, int user2Id) throws InterruptedException, SQLException{
        int id = generateConvoID();
        Convo convo = new Convo.Builder()
            .setConvoId(id)
            .setUser1Id(user1Id)
            .setUser2Id(user2Id)
            .build();

        writeConvoToDB(convo);
        return convo;
    }

    private void writeConvoToDB(Convo convo) throws InterruptedException, SQLException{
        sharedResource.lock();

        String command = "insert into convos(convo_id, user1_id, user2_id)values(?,?,?)";

        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, convo.getConvoId());
        prep.setInt(2, convo.getUser1Id());
        prep.setInt(3, convo.getUser2Id());

        prep.execute();

        sharedResource.unlock();
    }

    public ArrayList<Integer> getChatIdsInConvo(int convoId) throws InterruptedException, SQLException{
        ArrayList<Integer> ids = new ArrayList<>();
        sharedResource.lock();

        String command = "select * from chats where convo_id=? order by time_sent desc";

        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, convoId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            ids.add(rs.getInt("chat_id"));
        }
        prep.close();

        sharedResource.unlock();
        return ids;
    }

    public ArrayList<Chat> getChatsInConvo(int convoId) throws InterruptedException, SQLException{
        ArrayList<Chat> chats = new ArrayList<>();
        sharedResource.lock();

        String command = "select * from chats where convo_id=? order by time_sent desc";

        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, convoId);
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            int id = rs.getInt("chat_id");
            ChatType type = ChatType.parse(rs.getString("type"));
            chats.add(getChatById(id, type));
        }
        prep.close();

        sharedResource.unlock();
        return chats;
    }

    public Chat getLastChat(int convoId) throws InterruptedException, SQLException{
        Chat chat = null;
        sharedResource.lock();
        String command = "select * from chats where convo_id=? order by time_sent desc";
        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, convoId);
        ResultSet rs = prep.executeQuery();
        if(rs.next()){
            int id = rs.getInt("chat_id");
            ChatType type = ChatType.parse(rs.getString("type"));
            chat = getChatById(id, type);
        }
        sharedResource.unlock();
        return chat;
    }

    public ArrayList<Chat> getChatsInConvo(int convoId, int startIndex, int endIndex) throws InterruptedException, SQLException{
        ArrayList<Chat> chats = new ArrayList<>();
        sharedResource.lock();

        String command = "select * from chats where convo_id=? order by time_sent desc";

        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, convoId);
        int i = 0;
        ResultSet rs = prep.executeQuery();
        while(rs.next()){
            if(i < startIndex) continue;
            if(i >= endIndex) break;
            int id = rs.getInt("chat_id");
            ChatType type = ChatType.parse(rs.getString("type"));
            chats.add(getChatById(id, type));
            i++;
        }
        prep.close();

        sharedResource.unlock();
        return chats;
    }


    private Chat getChatById(int id, ChatType chatType) throws SQLException{
        Chat chat = null;
        switch (chatType) {
            case MEDIA:
                chat = getMediaChatById(id);
                break;
            case MESSAGE:
                chat = getMessageChatById(id);
                break;
            case PAYMENT:
                chat = getPaymentChatById(id);
                break;
        }
        return chat;

    }

    private MessageChat getMessageChatById(int id) throws SQLException{
        String command = "select * from chats join message_chat on chats.chat_id = message_chat.chat_id where chats.chat_id=?";

        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, id);
        ResultSet rs = prep.executeQuery();
        MessageChat chat = null;
        if(rs.next()){
            chat = new MessageChat.Builder()
                .setMessage(rs.getString("message"))
                .setChatId(id)
                .setType(ChatType.MESSAGE)
                .setSenderId(rs.getInt("sender_id"))
                .setConvoId(rs.getInt("convo_id"))
                .setTimeSent(rs.getTimestamp("time_sent").toLocalDateTime())
                .build();
            prep.execute();
            prep.close();
        }

        return chat;
    }

    private MediaChat getMediaChatById(int id) throws SQLException{
        String command = "select * from chats join media_chat on chats.chat_id = media_chat.chat_id where chats.chat_id=?";

        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, id);
        ResultSet rs = prep.executeQuery();
        MediaChat chat = null;
        if(rs.next()){
            chat = new MediaChat.Builder()
                .setMediaId(rs.getInt("media_id"))
                .setChatId(id)
                .setSenderId(rs.getInt("sender_id"))
                .setConvoId(rs.getInt("convo_id"))
                .setType(ChatType.MEDIA)
                .setTimeSent(rs.getTimestamp("time_sent").toLocalDateTime())
                .build();
            prep.execute();
            prep.close();
        }

        return chat;
    }

    private PaymentChat getPaymentChatById(int id) throws SQLException {
        String command = "select * from chats join payment_chat on chats.chat_id = payment_chat.chat_id where chats.chat_id=?";

        PreparedStatement prep = dbController.prepareStatement(command);
        prep.setInt(1, id);
        ResultSet rs = prep.executeQuery();
        PaymentChat chat = null;
        if(rs.next()){
            chat = new PaymentChat.Builder()
                .setPaymentId(rs.getInt("payment_id"))
                .setChatId(id)
                .setType(ChatType.PAYMENT)
                .setSenderId(rs.getInt("sender_id"))
                .setConvoId(rs.getInt("convo_id"))
                .setTimeSent(rs.getTimestamp("time_sent").toLocalDateTime())
                .build();
            prep.execute();
            prep.close();
        }
        return chat;
    }
}
