package com.advoyage.livestream.testekin.model;

import java.sql.Time;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Schedule {
    public String dateTime;
    public long  time;
    public String name;

    public Schedule(String date, long time, String name) {
        this.dateTime = date;
        this.time = time;
        this.name = name;
    }

    public Schedule() {
    }

    public Map<String,Object> toMap(){
        Map<String,Object> scheduleMap = new HashMap<>();
        scheduleMap.put("dateTime",this.dateTime);
        scheduleMap.put("time",this.time);
        scheduleMap.put("name",this.name);

        return scheduleMap;
    }


}
