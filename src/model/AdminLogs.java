package model;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class AdminLogs {
    public static void logActivity(Connection conn, int userId, String username, String status, String activity) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO logs_activity (user_id, username, status, timestamp, activity) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, status);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setString(5, activity);

            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Gagal mencatat aktivitas admin:");
            e.printStackTrace();
        }
    }
}
