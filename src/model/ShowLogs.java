package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShowLogs {
    public static List<Logs_aktivity> getUserLogs(Connection conn, int userId) {
        List<Logs_aktivity> logs = new ArrayList<>();

        String sql = "SELECT * FROM logs_activity WHERE user_id = ? ORDER BY timestamp DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Logs_aktivity log = new Logs_aktivity();
                log.setId(rs.getInt("id"));
                log.setUserId(rs.getInt("user_id"));
                log.setUsername(rs.getString("username"));
                log.setStatus(rs.getString("status"));
                log.setTimestamp(rs.getTimestamp("timestamp"));
                log.setActivity(rs.getString("activity"));
                logs.add(log);
            }

        } catch (SQLException e) {
            System.err.println("Gagal mengambil log aktivitas:");
            e.printStackTrace();
        }

        return logs;
    }

}
