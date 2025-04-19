package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RefundTransaction extends Transaction implements Payable {
    private List<CartItem> items;
    private String purchaseTransactionId;
    private int userId;
    private String username;
    private String reason;

    public RefundTransaction(String purchaseTransactionId, int userId, String username, String reason) {
        super();
        this.items = new ArrayList<>();
        this.purchaseTransactionId = purchaseTransactionId;
        this.userId = userId;
        this.username = username;
        this.reason = reason;
    }

    public void addItem(CartItem item) {
        this.items.add(item);
    }

    public List<CartItem> getItems() {
        return items;
    }

    public String getPurchaseTransactionId() {
        return purchaseTransactionId;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public double calculateTotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getProduct().getHarga() * item.getQuantity();
        }
        return total;
    }

    @Override
    public void processTransaction() {
        System.out.println("Transaksi retur dengan ID " + this.transactionId + " telah diproses");
        // Tambahkan log aktivitas
        logActivity("Melakukan transaksi retur dengan ID " + this.transactionId +
                " untuk pembelian " + purchaseTransactionId);
    }

    @Override
    public void serializeTransaction() {
        // Simpan transaksi ke database
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres",
                "postgres.jnmxqxmrgwmmupkozavo",
                "kelompok9")) {
            // Simpan data transaksi utama (asumsi ada tabel transactions)
            String sql = "INSERT INTO transactions (transaction_id, user_id, username, transaction_date, total_amount, type, reference_transaction_id, reason) " +
                    "VALUES (?, ?, ?, ?, ?, 'REFUND', ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, this.transactionId);
                stmt.setInt(2, this.userId);
                stmt.setString(3, this.username);
                stmt.setTimestamp(4, java.sql.Timestamp.valueOf(this.date));
                stmt.setDouble(5, this.calculateTotal());
                stmt.setString(6, this.purchaseTransactionId);
                stmt.setString(7, this.reason);
                stmt.executeUpdate();
            }

            // Simpan detail transaksi (asumsi ada tabel transaction_items)
            String detailSql = "INSERT INTO transaction_items (transaction_id, product_code, product_name, quantity, price) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement detailStmt = conn.prepareStatement(detailSql)) {
                for (CartItem item : items) {
                    detailStmt.setString(1, this.transactionId);
                    detailStmt.setString(2, item.getProduct().getKode());
                    detailStmt.setString(3, item.getProduct().getNama());
                    detailStmt.setInt(4, item.getQuantity());
                    detailStmt.setDouble(5, item.getProduct().getHarga());
                    detailStmt.addBatch();
                }
                detailStmt.executeBatch();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void logActivity(String activityDescription) {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres",
                "postgres.jnmxqxmrgwmmupkozavo",
                "kelompok9")) {
            String sql = "INSERT INTO logs_activity (user_id, username, status, timestamp, activity) " +
                    "VALUES (?, ?, 'REFUND', CURRENT_TIMESTAMP, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, username);
                stmt.setString(3, activityDescription);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}