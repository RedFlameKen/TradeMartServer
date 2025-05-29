package com.trademart.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public class IDGenerator {

    public static final int ID_RANGE = 100000;

    public static int generateDBID(DatabaseController dbController, String tableName, String idName) {
        int[] ids = null;
        try {
            int rows = dbController.getRowCountDB(tableName);
            ResultSet rs = dbController.execQuery(String.format("select %s from %s", idName, tableName));
            ids = new int[rows];
            int i = 0;
            while(rs.next()){
                ids[i++] = rs.getInt(idName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int id = generateID(ids);
        return id;
    }

    public static int generateID(int[] idList){
        int id = -1;
        int iter = 0;
        do {
            if(iter >= ID_RANGE){
                return -1;
            }
            id = (int) (Math.random() * ID_RANGE);
        } while(id == -1 || idExists(id, idList));
        return id;
    }

    public static boolean idExists(int id, int[] idList){
        for (int i : idList)
            if(id == i)
                return true;
        return false;
    }

}
