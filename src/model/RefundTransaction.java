package model;

import java.sql.Connection;
import java.sql.Timestamp;
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
    private boolean transactionComplete = false;

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
        // Untuk refund, amount paid adalah jumlah yang dikembalikan ke pelanggan
        this.amountPaid = calculateTotal();
        this.change = 0; // Tidak ada kembalian untuk refund

        System.out.println("Transaksi retur dengan ID " + this.transactionId + " telah diproses");
        System.out.println("Total Pengembalian: " + String.format("Rp%,.2f", calculateTotal()).replace('.', ','));

        // Tambahkan log aktivitas
        logActivity("Melakukan transaksi retur dengan ID " + this.transactionId +
                " untuk pembelian " + purchaseTransactionId);

        transactionComplete = true;
    }

    @Override
    public boolean serializeTransaction() {
        Connection conn = null;
        try {
            conn = getConnection();

            // Mulai transaksi database
            conn.setAutoCommit(false);

            // Log untuk debugging
            System.out.println("Menyimpan transaksi retur ke database dengan ID: " + this.transactionId);

            // Simpan data transaksi utama
            String transactionSql = "INSERT INTO transactions " +
                    "(transaction_id, user_id, username, transaction_date, total_amount, amount_paid, change_amount, type, reference_transaction_id, reason) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'REFUND', ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(transactionSql)) {
                stmt.setString(1, this.transactionId);
                stmt.setInt(2, this.userId);
                stmt.setString(3, this.username);
                stmt.setTimestamp(4, Timestamp.valueOf(this.date));
                stmt.setDouble(5, this.calculateTotal());
                stmt.setDouble(6, this.amountPaid);
                stmt.setDouble(7, this.change);
                stmt.setString(8, this.purchaseTransactionId);
                stmt.setString(9, this.reason);

                int transactionRows = stmt.executeUpdate();
                System.out.println("Berhasil menyimpan transaksi retur: " + transactionRows + " baris");
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
                    System.out.println("Berhasil menyimpan " + itemResults.length + " item transaksi retur");
                }
            }

            // Log aktivitas transaksi
            logActivityWithConnection(conn, "Melakukan transaksi retur dengan ID " + this.transactionId +
                    " untuk pembelian " + purchaseTransactionId);

            // Commit transaksi
            conn.commit();
            System.out.println("Transaksi retur database berhasil dicommit");
            return true;

        } catch (SQLException e) {
            // Rollback jika terjadi error
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Transaksi retur database di-rollback karena error");
                } catch (SQLException ex) {
                    System.err.println("Gagal melakukan rollback: " + ex.getMessage());
                }
            }

            System.err.println("Database error saat menyimpan transaksi retur: " + e.getMessage());
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
        String sql = "INSERT INTO logs_activity (user_id, username, status, timestamp, activity) " +
                "VALUES (?, ?, 'REFUND', CURRENT_TIMESTAMP, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, activityDescription);
            int rows = stmt.executeUpdate();
            System.out.println("Log aktivitas retur disimpan: " + rows + " baris");
        }
    }

    public boolean isTransactionComplete() {
        return transactionComplete;
    }

    public String getAmountPaidFormatted() {
        return String.format("Rp%,.2f", amountPaid).replace('.', ',');
    }
}