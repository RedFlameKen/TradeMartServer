package com.trademart.db;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Component;

import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

@Component
public class DatabaseController {

    public static final String DB_URL_FORMAT = "jdbc:mysql://%s:%d/";

    private String dbName;
    private String dbUsername;
    private String dbPassword;
    private String dbAddress;
    private int dbPort;

    private Connection connection;
    private boolean isConnected;

    public DatabaseController(){
        initDB();
    }

    public static int loadMySQLDriver(){
        try {
            Class.forName("com.mysql.jc.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            Logger.log("Could not load the JDBC Driver!", LogLevel.CRITICAL);
            return -1;
        }
        return 0;
    }

    public int connect(){
        if(isConnected){
            Logger.log("Attempted to connect DatabaseController to db when it's already connected", LogLevel.WARNING);
            return 0;
        }
        try {
            connection = DriverManager.getConnection(String.format(DB_URL_FORMAT, dbAddress, dbPort), dbUsername, dbPassword);
        } catch (SQLException e) {
            Logger.log("Unable to connect to MySQL server!", LogLevel.CRITICAL);
            return -1;
        }
        try {
            initDatabase();
        } catch (SQLException e) {
            Logger.log("Unable to initialize the Database", LogLevel.CRITICAL);
            return -1;
        }
        try {
            useDatabase();
        } catch (SQLException e) {
            Logger.log("Unable use the database, " + dbName, LogLevel.CRITICAL);
        }
        try {
            initTables();
        } catch (SQLException e) {
            Logger.log("Unable to initialize tables", LogLevel.CRITICAL);
            return -1;
        }
        isConnected = true;
        Logger.log("Successfully connected to MySQL Server!", LogLevel.INFO);
        return 0;
    }

    public int getRowCountDB(String tableName){
        int rows = 0;
        try {
            ResultSet rs = execQuery(String.format("select COUNT(*) from %s", tableName));
            if(rs.next())
                rows = rs.getRow();
        } catch (SQLException e) {
            Logger.log("Unable to count rows from db", LogLevel.WARNING);
            e.printStackTrace();
        }
        return rows;
    }

    public void exec(String command) throws SQLException{
        Statement stmt = connection.createStatement();
        stmt.execute(command);
    }

    public ResultSet execQuery(String command) throws SQLException{
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(command);
        return rs;
    }

    public PreparedStatement prepareStatement(String command) throws SQLException{
        PreparedStatement prep = connection.prepareStatement(command);
        return prep;
    }

    public void execCaught(String command) {
        Statement stmt;
        try {
            stmt = connection.createStatement();
            stmt.execute(command);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet execQueryCaught(String command) {
        Statement stmt;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery(command);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }

    private int initDB(){
        try {
            readConfig();
        } catch (FileNotFoundException e) {
            Logger.log("Could not find config file!", LogLevel.CRITICAL);
            return -1;
        } catch (JSONException e) {
            Logger.log("There is a problem with the config file!", LogLevel.CRITICAL);
            return -1;
        }
        return 0;
    }

    private void initDatabase() throws SQLException{
        Statement stmt = connection.createStatement();
        stmt.execute("create database if not exists " + dbName);
    }

    private void initTables() throws SQLException{
        Statement stmt = connection.createStatement();
        stmt.execute("create table if not exists messages(username varchar(255), message varchar(4096), time_sent datetime)");
    }

    private void useDatabase() throws SQLException{
        Statement stmt = connection.createStatement();
        stmt.execute("use " + dbName);
    }

    private void readConfig() throws FileNotFoundException, JSONException {
        FileReader reader = new FileReader(".dbconfig.json");
        JSONObject configJson = new JSONObject(new JSONTokener(reader));
        dbUsername = configJson.getString("username");
        dbPassword = configJson.getString("password");
        dbAddress = configJson.getString("address");
        dbPort = configJson.getInt("port");
        dbName = configJson.getString("db_name");
        
    }

}
