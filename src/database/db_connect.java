package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class db_connect {
    private static final String URL = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres";
    private static final String USER = "postgres.jnmxqxmrgwmmupkozavo";
    private static final String PASSWORD = "kelompok9";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("✅ Koneksi berhasil!");
        } catch (SQLException e) {
            System.out.println("❌ Koneksi gagal: " + e.getMessage());
        }
    }
}
