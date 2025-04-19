package model;

public class User {
    private int id;
    private String username;
    private String password;
    private String role;
    private String nama;

    public User() {
    }

    public User(int id, String username, String password, String role, String nama) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.nama = nama;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }
}
