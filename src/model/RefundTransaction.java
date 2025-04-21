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

        transactionComplete = true;
    }

    @Override
    public boolean serializeTransaction() {
        Connection conn = null;
        PreparedStatement transactionStmt = null;
        PreparedStatement itemStmt = null;
        PreparedStatement logStmt = null;
        PreparedStatement updateStmt = null;

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

            transactionStmt = conn.prepareStatement(transactionSql);
            transactionStmt.setString(1, this.transactionId);
            transactionStmt.setInt(2, this.userId);
            transactionStmt.setString(3, this.username);
            transactionStmt.setTimestamp(4, Timestamp.valueOf(this.date));
            transactionStmt.setDouble(5, this.calculateTotal());
            transactionStmt.setDouble(6, this.amountPaid);
            transactionStmt.setDouble(7, this.change);
            transactionStmt.setString(8, this.purchaseTransactionId);
            transactionStmt.setString(9, this.reason);

            int transactionRows = transactionStmt.executeUpdate();
            System.out.println("Berhasil menyimpan transaksi retur: " + transactionRows + " baris");
            transactionStmt.close();

            // Simpan detail transaksi (item-item)
            String itemSql = "INSERT INTO transaction_items " +
                    "(transaction_id, product_code, product_name, quantity, price, returned) " +
                    "VALUES (?, ?, ?, ?, ?, FALSE)";

            itemStmt = conn.prepareStatement(itemSql);
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
            itemStmt.close();

            // Log aktivitas transaksi
            String logSql = "INSERT INTO logs_activity (user_id, username, status, timestamp, activity) " +
                    "VALUES (?, ?, 'RETURN', CURRENT_TIMESTAMP, ?)";

            logStmt = conn.prepareStatement(logSql);
            logStmt.setInt(1, userId);
            logStmt.setString(2, username);
            logStmt.setString(3, "Melakukan transaksi retur dengan ID " + this.transactionId +
                    " untuk pembelian " + purchaseTransactionId + " - " + reason);

            int logRows = logStmt.executeUpdate();
            System.out.println("Log aktivitas return disimpan: " + logRows + " baris");
            logStmt.close();

            // Mark items as returned in the original transaction
            String updateSql = "UPDATE transaction_items SET returned = TRUE " +
                    "WHERE transaction_id = ? AND product_code = ?";

            updateStmt = conn.prepareStatement(updateSql);
            for (CartItem item : items) {
                updateStmt.setString(1, this.purchaseTransactionId);
                updateStmt.setString(2, item.getProduct().getKode());
                updateStmt.addBatch();
            }

            int[] updateResults = updateStmt.executeBatch();
            System.out.println("Berhasil memperbarui " + updateResults.length + " item menjadi returned");
            updateStmt.close();

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
            // Close all statements explicitly
            try {
                if (transactionStmt != null) transactionStmt.close();
                if (itemStmt != null) itemStmt.close();
                if (logStmt != null) logStmt.close();
                if (updateStmt != null) updateStmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statements: " + e.getMessage());
            }

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

    public boolean isTransactionComplete() {
        return transactionComplete;
    }

    public String getAmountPaidFormatted() {
        return String.format("Rp%,.2f", amountPaid).replace('.', ',');
    }
}