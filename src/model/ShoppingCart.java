package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingCart {
    private static ShoppingCart instance;
    private Map<String, CartItem> items;
    private User currentUser;

    private ShoppingCart() {
        this.items = new HashMap<>();
    }

    public static ShoppingCart getInstance() {
        if (instance == null) {
            instance = new ShoppingCart();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void addItem(Product product, int quantity) {
        String productCode = product.getKode();

        if (items.containsKey(productCode)) {
            // jika produk sdh ada dikeranjang, update jumlah
            CartItem existingItem = items.get(productCode);
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            // jika blm, tambahkan
            items.put(productCode, new CartItem(product, quantity));
        }

        // Log the activity
        logActivity("Menambahkan " + quantity + " " + product.getNama() + " ke keranjang");
    }

    public void updateItemQuantity(String productCode, int newQuantity) {
        if (items.containsKey(productCode)) {
            CartItem item = items.get(productCode);
            String productName = item.getProduct().getNama();
            int oldQuantity = item.getQuantity();

            if (newQuantity <= 0) {
                removeItem(productCode);
            } else {
                item.setQuantity(newQuantity);
                // Log the activity
                logActivity("Mengubah jumlah " + productName + " di keranjang dari " +
                        oldQuantity + " menjadi " + newQuantity);
            }
        }
    }

    public void removeItem(String productCode) {
        if (items.containsKey(productCode)) {
            CartItem item = items.get(productCode);
            String productName = item.getProduct().getNama();
            items.remove(productCode);
            // Log the activity
            logActivity("Menghapus " + productName + " dari keranjang");
        }
    }

    public List<CartItem> getItems() {
        return new ArrayList<>(items.values());
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getItemCount() {
        return items.size();
    }

    public double getTotal() {
        double total = 0;
        for (CartItem item : items.values()) {
            total += item.getSubtotal();
        }
        return total;
    }

    public String getTotalFormatted() {
        return String.format("Rp%,.2f", getTotal()).replace('.', ',');
    }

    public void clear() {
        items.clear();
    }

    public PurchaseTransaction checkout() {
        if (isEmpty() || currentUser == null) {
            return null;
        }

        PurchaseTransaction transaction = new PurchaseTransaction(
                currentUser.getId(),
                currentUser.getNama()
        );

        for (CartItem item : items.values()) {
            transaction.addItem(item);
        }

        transaction.processTransaction();
        transaction.serializeTransaction();

        // Clear cart after successful checkout
        clear();

        return transaction;
    }

    private void logActivity(String activityDescription) {
        if (currentUser == null) return;

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres",
                "postgres.jnmxqxmrgwmmupkozavo",
                "kelompok9")) {
            String sql = "INSERT INTO logs_activity (user_id, username, status, timestamp, activity) " +
                    "VALUES (?, ?, 'CART', CURRENT_TIMESTAMP, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUser.getId());
                stmt.setString(2, currentUser.getUsername());
                stmt.setString(3, activityDescription);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to log cart activity: " + e.getMessage());
        }
    }
}
