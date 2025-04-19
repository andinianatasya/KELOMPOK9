package dao;
import database.db_connect;
import model.User;
import model.AuthLog;
import model.Auth;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    // Method to authenticate a user
    public User authenticate(String username, String plainPassword) throws SQLException {
        String sql = "SELECT * FROM userk9 WHERE username = ?";

        try (Connection conn = db_connect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password");

                    // Verify password using BCrypt
                    if (model.Auth.checkPassword(plainPassword, hashedPassword)) {
                        User user = new User();
                        user.setId(rs.getInt("id"));
                        user.setUsername(rs.getString("username"));
                        user.setPassword(hashedPassword); // Never expose the password outside this class
                        user.setRole(rs.getString("role"));

                        // Log successful login
                        logAuthentication(user.getId(), username, "SUCCESS");

                        return user;
                    }
                }

                // Log failed login attempt
                logAuthentication(0, username, "FAILED");
                return null;
            }
        }
    }

    // Method to register a new user
    public boolean registerUser(String username, String plainPassword) throws SQLException {
        // Check if username already exists
        if (usernameExists(username)) {
            return false;
        }

        String sql = "INSERT INTO userk9 (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = db_connect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash the password before storing
            String hashedPassword = model.Auth.hashPassword(plainPassword);

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, "user"); // Default role for new registrations

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // Check if a username already exists
    private boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM userk9 WHERE username = ?";

        try (Connection conn = db_connect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        }
    }

    // Method to log authentication attempts
    public void logAuthentication(int userId, String username, String status) throws SQLException {
        String sql = "INSERT INTO auth_logs (user_id, username, status, timestamp) VALUES (?, ?, ?, ?)";

        try (Connection conn = db_connect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, status);
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

            stmt.executeUpdate();
        }
    }

    // Insert a predefined admin user
    public boolean setupAdminUser() throws SQLException {
        if (!usernameExists("adminuser")) {
            String sql = "INSERT INTO userk9 (username, password, role) VALUES (?, ?, ?)";

            try (Connection conn = db_connect.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                // Create a default admin with password "admin123"
                String hashedPassword = model.Auth.hashPassword("admin123");

                stmt.setString(1, "adminuser");
                stmt.setString(2, hashedPassword);
                stmt.setString(3, "admin");

                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            }
        }
        return false;
    }
}
