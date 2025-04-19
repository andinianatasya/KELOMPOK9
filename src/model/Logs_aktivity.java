package model;

import java.sql.Timestamp;

public class Logs_aktivity {
    private int id;
    private int userId;
    private String username;
    private String status;
    private Timestamp timestamp;
    private String activity;

    public Logs_aktivity() {
    }

    public Logs_aktivity(int id, int userId, String username, String status, Timestamp timestamp) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.timestamp = timestamp;
        this.activity = activity;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }
}
