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
        // Only process if payment is successful
        if (this.amountPaid < calculateTotal()) {
            System.out.println("Tidak dapat memproses transaksi: Pembayaran kurang dari total belanja");
            return;
        }

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
        // Only serialize if transaction is complete
        if (!transactionComplete) {
            System.out.println("Tidak dapat menyimpan transaksi: Transaksi belum selesai");
            return false;
        }

        boolean success = false;

        try {
            // Pastikan username tidak null
            if (this.username == null) {
                try (Connection conn = getConnection()) {
                    this.username = getUsernameByUserId(this.userId);
                }
                if (this.username == null) {
                    throw new SQLException("Username tidak ditemukan untuk userId " + this.userId);
                }
            }

            // Log untuk debugging
            System.out.println("Menyimpan transaksi ke database dengan ID: " + this.transactionId);
            System.out.println("Jumlah item dalam transaksi: " + this.items.size());

            // 1. Simpan data transaksi utama menggunakan koneksi terpisah
            try (Connection conn = getConnection()) {
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
            }

            // 2. Simpan detail transaksi (item-item) menggunakan koneksi terpisah untuk setiap item
            for (CartItem item : items) {
                try (Connection conn = getConnection();
                     PreparedStatement itemStmt = conn.prepareStatement(
                             "INSERT INTO transaction_items (transaction_id, product_code, product_name, quantity, price) " +
                                     "VALUES (?, ?, ?, ?, ?)")) {

                    itemStmt.setString(1, this.transactionId);
                    itemStmt.setString(2, item.getProduct().getKode());
                    itemStmt.setString(3, item.getProduct().getNama());
                    itemStmt.setInt(4, item.getQuantity());
                    itemStmt.setDouble(5, item.getProduct().getHarga());

                    int itemResult = itemStmt.executeUpdate();
                    System.out.println("Item " + item.getProduct().getKode() + " berhasil disimpan: " + itemResult + " baris");
                }
            }

            // 3. Log aktivitas dengan koneksi terpisah
            logActivity("Melakukan checkout transaksi " + this.transactionId +
                    " dengan total " + String.format("Rp%,.2f", this.calculateTotal()).replace('.', ','));

            System.out.println("Semua data transaksi berhasil disimpan");
            success = true;

        } catch (SQLException e) {
            System.err.println("Database error saat menyimpan transaksi: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }

        return success;
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