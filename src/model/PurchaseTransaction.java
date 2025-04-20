package model;

import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;

public class PurchaseTransaction extends Transaction implements Payable {
    private List<CartItem> items;
    private int userId;
    private String username;
    private boolean transactionComplete = false;

    public PurchaseTransaction(int userId, String username) {
        super();
        this.items = new ArrayList<>();
        this.userId = userId;
        this.username = username;
    }

    public void addItem(CartItem item) {
        this.items.add(item);
    }

    public List<CartItem> getItems() {
        return items;
    }

    @Override
    public double calculateTotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getProduct().getHarga() * item.getQuantity();
        }
        return total;
    }

    public boolean processPayment(double amountPaid) {
        double total = calculateTotal();

        // Validate payment amount
        if (amountPaid < total) {
            return false;
        }

        this.amountPaid = amountPaid;
        this.calculateChange(total);
        return true;
    }

    @Override
    public void processTransaction() {
        System.out.println("Transaksi dengan ID " + this.transactionId + " telah diproses");
        System.out.println("Total Pembelian: " + String.format("Rp%,.2f", calculateTotal()).replace('.', ','));
        System.out.println("Pembayaran: " + String.format("Rp%,.2f", amountPaid).replace('.', ','));
        System.out.println("Kembalian: " + String.format("Rp%,.2f", change).replace('.', ','));

        // Tambahkan log aktivitas
        logActivity("Melakukan transaksi pembelian dengan ID " + this.transactionId);
        transactionComplete = true;
    }

    private String getUsernameByUserId(int userId) {
        Connection conn = null;
        String username = null;
        try {
            conn = getConnection();
            String query = "SELECT username FROM userk9 WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        username = rs.getString("username");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching username: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
        return username;
    }


    @Override
    public boolean serializeTransaction() {
        Connection conn = null;
        try {
            conn = getConnection();

            // Pastikan username tidak null
            if (this.username == null) {
                this.username = getUsernameByUserId(this.userId);
                if (this.username == null) {
                    throw new SQLException("Username tidak ditemukan untuk userId " + this.userId);
                }
            }

            // Mulai transaksi database
            conn.setAutoCommit(false);

            // Log untuk debugging
            System.out.println("Menyimpan transaksi ke database dengan ID: " + this.transactionId);

            // Simpan data transaksi utama
            String transactionSql = "INSERT INTO transactions " +
                    "(transaction_id, user_id, username, transaction_date, total_amount, amount_paid, change_amount, type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'PURCHASE')";

            try (PreparedStatement stmt = conn.prepareStatement(transactionSql)) {
                stmt.setString(1, this.transactionId);
                stmt.setInt(2, this.userId);
                stmt.setString(3, this.username);
                stmt.setTimestamp(4, Timestamp.valueOf(this.date));
                stmt.setDouble(5, this.calculateTotal());
                stmt.setDouble(6, this.amountPaid);
                stmt.setDouble(7, this.change);

                int transactionRows = stmt.executeUpdate();
                System.out.println("Berhasil menyimpan transaksi: " + transactionRows + " baris");
            }

            // Simpan detail transaksi (item-item)
            String itemSql = "INSERT INTO transaction_items " +
                    "(transaction_id, product_code, product_name, quantity, price) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                int batchCount = 0;

                for (CartItem item : items) {
                    itemStmt.setString(1, this.transactionId);
                    itemStmt.setString(2, item.getProduct().getKode());
                    itemStmt.setString(3, item.getProduct().getNama());
                    itemStmt.setInt(4, item.getQuantity());
                    itemStmt.setDouble(5, item.getProduct().getHarga());
                    itemStmt.addBatch();
                    batchCount++;
                }

                if (batchCount > 0) {
                    int[] itemResults = itemStmt.executeBatch();
                    System.out.println("Berhasil menyimpan " + itemResults.length + " item transaksi");
                }
            }

            // Log aktivitas transaksi
            logActivityWithConnection(conn, "Melakukan checkout transaksi " + this.transactionId +
                    " dengan total " + String.format("Rp%,.2f", this.calculateTotal()).replace('.', ','));

            // Commit transaksi
            conn.commit();
            System.out.println("Transaksi database berhasil dicommit");
            return true;

        } catch (SQLException e) {
            // Rollback jika terjadi error
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Transaksi database di-rollback karena error");
                } catch (SQLException ex) {
                    System.err.println("Gagal melakukan rollback: " + ex.getMessage());
                }
            }

            System.err.println("Database error saat menyimpan transaksi: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            return false;

        } finally {
            // Tutup koneksi
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                    System.out.println("Koneksi database ditutup");
                } catch (SQLException e) {
                    System.err.println("Gagal menutup koneksi: " + e.getMessage());
                }
            }
        }
    }

    private void logActivity(String activityDescription) {
        try (Connection conn = getConnection()) {
            logActivityWithConnection(conn, activityDescription);
        } catch (SQLException e) {
            System.err.println("Gagal mencatat aktivitas: " + e.getMessage());
        }
    }

    private void logActivityWithConnection(Connection conn, String activityDescription) throws SQLException {

        if (this.username == null) {
            this.username = getUsernameByUserId(this.userId);
            if (this.username == null) {
                throw new SQLException("Username tidak ditemukan untuk userId " + this.userId + " saat mencatat log");
            }
        }

        String sql = "INSERT INTO logs_activity (user_id, username, status, timestamp, activity) " +
                "VALUES (?, ?, 'PURCHASE', CURRENT_TIMESTAMP, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, activityDescription);
            int rows = stmt.executeUpdate();
            System.out.println("Log aktivitas disimpan: " + rows + " baris");
        }
    }

    public boolean isTransactionComplete() {
        return transactionComplete;
    }

    public void setTransactionComplete(boolean transactionComplete) {
        this.transactionComplete = transactionComplete;
    }

    public String getAmountPaidFormatted() {
        return String.format("Rp%,.2f", amountPaid).replace('.', ',');
    }

    public String getChangeFormatted() {
        return String.format("Rp%,.2f", change).replace('.', ',');
    }
}