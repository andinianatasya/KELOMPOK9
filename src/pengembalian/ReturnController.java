package pengembalian;

import model.*;
import java.sql.*;
import java.util.*;
import java.time.LocalDate;
import java.net.URL;

public class ReturnController {

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres",
                "postgres.jnmxqxmrgwmmupkozavo",
                "kelompok9"
        );
    }

    // Mencari transaksi berdasarkan ID transaksi
    public Map<String, Object> findTransactionById(String transactionId) {
        Map<String, Object> result = new HashMap<>();
        result.put("transaction", null);
        result.put("items", new ArrayList<CartItem>());

        try (Connection conn = getConnection()) {
            // Query untuk data transaksi
            String sql = "SELECT * FROM transactions WHERE transaction_id = ? AND type = 'PURCHASE'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, transactionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> transaction = new HashMap<>();
                        transaction.put("transaction_id", rs.getString("transaction_id"));
                        transaction.put("user_id", rs.getInt("user_id"));
                        transaction.put("username", rs.getString("username"));
                        transaction.put("transaction_date", rs.getTimestamp("transaction_date"));
                        transaction.put("total_amount", rs.getDouble("total_amount"));

                        // Query untuk item transaksi
                        String itemSql = "SELECT * FROM transaction_items WHERE transaction_id = ?";
                        try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                            itemStmt.setString(1, transactionId);
                            try (ResultSet itemRs = itemStmt.executeQuery()) {
                                List<Map<String, Object>> items = new ArrayList<>();
                                while (itemRs.next()) {
                                    Map<String, Object> item = new HashMap<>();
                                    item.put("product_code", itemRs.getString("product_code"));
                                    item.put("product_name", itemRs.getString("product_name"));
                                    item.put("quantity", itemRs.getInt("quantity"));
                                    item.put("price", itemRs.getDouble("price"));
                                    items.add(item);
                                }
                                result.put("items", items);
                            }
                        }

                        result.put("transaction", transaction);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding transaction: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    // Proses untuk melakukan return produk
    public boolean processReturn(String originalTransactionId, int userId, String username,
                                 String reason, List<ReturnItem> itemsToReturn) {
        if (itemsToReturn.isEmpty()) {
            System.err.println("Tidak ada item untuk dikembalikan");
            return false;
        }

        try {
            // Buat objek RefundTransaction
            RefundTransaction refund = new RefundTransaction(
                    originalTransactionId, userId, username, reason
            );

            // Validasi item yang akan dikembalikan
            Map<String, Object> originalTransaction = findTransactionById(originalTransactionId);
            if (originalTransaction.get("transaction") == null) {
                System.err.println("Transaksi original tidak ditemukan");
                return false;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> originalItems = (List<Map<String, Object>>) originalTransaction.get("items");
            Map<String, Object> productMap = new HashMap<>();

            // Buat map untuk mempermudah validasi
            for (Map<String, Object> item : originalItems) {
                productMap.put((String)item.get("product_code"), item);
            }

            // Validasi dan tambahkan item ke refund
            for (ReturnItem returnItem : itemsToReturn) {
                String productCode = returnItem.getProductCode();

                if (!productMap.containsKey(productCode)) {
                    System.err.println("Produk " + productCode + " tidak ditemukan dalam transaksi asli");
                    return false;
                }

                Map<String, Object> originalItem = (Map<String, Object>) productMap.get(productCode);
                int originalQuantity = (int) originalItem.get("quantity");

                if (returnItem.getQuantity() > originalQuantity) {
                    System.err.println("Jumlah retur untuk " + productCode + " melebihi jumlah pembelian");
                    return false;
                }

                // Dapatkan data produk
                Product product = getProductByCode(productCode);
                if (product == null) {
                    System.err.println("Data produk tidak ditemukan: " + productCode);
                    return false;
                }

                // Tambahkan item ke transaksi refund
                refund.addItem(new CartItem(product, returnItem.getQuantity()));
            }

            // Proses transaksi return
            refund.processTransaction();
            boolean success = refund.serializeTransaction();

            if (success) {
                // Update inventory jika perlu
                updateInventoryForReturn(itemsToReturn);
            }

            return success;

        } catch (Exception e) {
            System.err.println("Error processing return: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Get product data from database
    private Product getProductByCode(String productCode) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM produk WHERE kode = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, productCode);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String kode = rs.getString("kode");
                        String nama = rs.getString("nama");
                        double harga = rs.getDouble("harga");
                        String tipe = rs.getString("tipe");

                        switch (tipe) {
                            case "Non-Perishable":
                                return new NonPerishableProduct(kode, nama, harga);

                            case "Perishable":
                                try (PreparedStatement ps = conn.prepareStatement(
                                        "SELECT tanggal_kadaluarsa FROM perishable_produk WHERE kode = ?")) {
                                    ps.setString(1, kode);
                                    try (ResultSet rsPerish = ps.executeQuery()) {
                                        if (rsPerish.next()) {
                                            LocalDate tgl = rsPerish.getDate("tanggal_kadaluarsa").toLocalDate();
                                            return new PerishableProduct(kode, nama, harga, tgl);
                                        }
                                    }
                                }
                                break;

                            case "Digital":
                                try (PreparedStatement ps = conn.prepareStatement(
                                        "SELECT url, vendor FROM digital_produk WHERE kode = ?")) {
                                    ps.setString(1, kode);
                                    try (ResultSet rsDigital = ps.executeQuery()) {
                                        if (rsDigital.next()) {
                                            URL url = new URL(rsDigital.getString("url"));
                                            String vendor = rsDigital.getString("vendor");
                                            return new DigitalProduct(kode, nama, harga, url, vendor);
                                        }
                                    }
                                }
                                break;

                            case "Bundle":
                                return new BundleProduct(kode, nama, new ArrayList<>());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting product: " + e.getMessage());
        }
        return null;
    }

    // Update inventory after return
    private void updateInventoryForReturn(List<ReturnItem> returnItems) {
        try (Connection conn = getConnection()) {
            // Mulai transaksi
            conn.setAutoCommit(false);

            // Asumsi ada tabel inventory
            String sql = "UPDATE inventory SET stock = stock + ? WHERE product_code = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (ReturnItem item : returnItems) {
                    stmt.setInt(1, item.getQuantity());
                    stmt.setString(2, item.getProductCode());
                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error updating inventory: " + e.getMessage());
        }
    }

    // Inner class untuk menangani item yang dikembalikan
    public static class ReturnItem {
        private String productCode;
        private int quantity;
        private String returnReason;

        public ReturnItem(String productCode, int quantity, String returnReason) {
            this.productCode = productCode;
            this.quantity = quantity;
            this.returnReason = returnReason;
        }

        public String getProductCode() {
            return productCode;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getReturnReason() {
            return returnReason;
        }
    }
}