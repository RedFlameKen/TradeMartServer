package com.trademart.util;

import java.time.LocalDateTime;

public class TimeUtil {

    public static LocalDateTime curDateTime(){
        LocalDateTime datetime = LocalDateTime.now();
        return datetime;
    }
}
