package com.example.springstarter;

import java.util.TimeZone;

public class TimeZoneTest {

    static void main() {
        // Inside the PostgreSQL JDBC driver (PgConnection.java)
        // Simplified version of what happens:
                String clientTimezone = TimeZone.getDefault().getID();  // → "Asia/Saigon"
        // This gets sent as a connection parameter: TimeZone=Asia/Saigon
        IO.println(clientTimezone);
        System.out.println((Integer) Integer.MAX_VALUE + 1);
    }

}
