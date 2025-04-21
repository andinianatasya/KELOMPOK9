package user;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.*;

import database.db_connect;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReturnBarangUser {
    private User currentUser;
    private TableView<TransactionItemView> tableView;
    private ComboBox<String> transactionComboBox;
    private List<TransactionItemView> selectedItems = new ArrayList<>();
    private TextField reasonField;
    private Label totalLabel;

    public ReturnBarangUser(User user) {
        this.currentUser = user;
    }

    public void start(Stage primaryStage) {
        // ===== LOGO =====
        Image logo = new Image(getClass().getResourceAsStream("/img/logo.png"));
        ImageView logoView = new ImageView(logo);
        logoView.setFitHeight(40);
        logoView.setPreserveRatio(true);

        // ===== LABEL "Return Barang" =====
        Label returnLabel = new Label("Return Barang");
        returnLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox returnBar = new HBox(returnLabel);
        returnBar.setAlignment(Pos.CENTER);

        // ===== TOMBOL KEMBALI =====
        Image kembaliImage = new Image(getClass().getResourceAsStream("/img/kembali.png"));
        ImageView kembaliIcon = new ImageView(kembaliImage);
        kembaliIcon.setFitHeight(30);
        kembaliIcon.setPreserveRatio(true);
        Button kembaliBtn = new Button();
        kembaliBtn.setGraphic(kembaliIcon);
        kembaliBtn.setStyle("-fx-background-color: transparent;");
        kembaliBtn.setOnAction(e -> {
            KeranjangUser keranjang = new KeranjangUser(currentUser);
            keranjang.start(new Stage());
            primaryStage.close();
        });

        HBox iconContainer = new HBox(10, kembaliBtn);
        iconContainer.setAlignment(Pos.CENTER_RIGHT);

        // ===== TOP BAR =====
        BorderPane topBar = new BorderPane();
        topBar.setLeft(logoView);
        topBar.setCenter(returnBar);
        topBar.setRight(iconContainer);
        topBar.setPadding(new Insets(10));

        // ===== TRANSACTION SELECTION =====
        Label transactionLabel = new Label("Pilih Transaksi:");
        transactionComboBox = new ComboBox<>();
        transactionComboBox.setPrefWidth(300);
        transactionComboBox.setOnAction(e -> loadTransactionItems());

        HBox transactionSelectionBox = new HBox(10, transactionLabel, transactionComboBox);
        transactionSelectionBox.setAlignment(Pos.CENTER_LEFT);
        transactionSelectionBox.setPadding(new Insets(10, 0, 10, 0));

        // ===== TABLE TRANSACTION ITEMS =====
        tableView = new TableView<>();
        tableView.setPrefWidth(900);
        tableView.setPrefHeight(300);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("Pilih transaksi untuk melihat item"));
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<TransactionItemView, CheckBox> selectCol = new TableColumn<>("Pilih");
        selectCol.setCellValueFactory(new PropertyValueFactory<>("selectCheckbox"));
        selectCol.setStyle("-fx-alignment: CENTER;");
        selectCol.setPrefWidth(50);

        TableColumn<TransactionItemView, String> kodeCol = new TableColumn<>("Kode");
        kodeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductCode()));

        TableColumn<TransactionItemView, String> namaCol = new TableColumn<>("Nama Barang");
        namaCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));

        TableColumn<TransactionItemView, String> hargaCol = new TableColumn<>("Harga Satuan");
        hargaCol.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("Rp%,.2f", data.getValue().getPrice()).replace('.', ',')));

        TableColumn<TransactionItemView, Integer> qtyCol = new TableColumn<>("Jumlah");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<TransactionItemView, String> subtotalCol = new TableColumn<>("Subtotal");
        subtotalCol.setCellValueFactory(data -> {
            double subtotal = data.getValue().getPrice() * data.getValue().getQuantity();
            return new SimpleStringProperty(String.format("Rp%,.2f", subtotal).replace('.', ','));
        });

        tableView.getColumns().addAll(selectCol, kodeCol, namaCol, hargaCol, qtyCol, subtotalCol);

        // ===== REASON SECTION =====
        Label reasonLabel = new Label("Alasan Return:");
        reasonField = new TextField();
        reasonField.setPrefWidth(400);
        reasonField.setPromptText("Masukkan alasan return barang");

        HBox reasonBox = new HBox(10, reasonLabel, reasonField);
        reasonBox.setAlignment(Pos.CENTER_LEFT);
        reasonBox.setPadding(new Insets(10, 0, 10, 0));

        // ===== TOTAL SECTION =====
        totalLabel = new Label("Total Return: Rp0,00");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button processReturnBtn = new Button("Proses Return");
        processReturnBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        processReturnBtn.setOnAction(e -> processReturn(primaryStage));

        HBox totalSection = new HBox(20, totalLabel, processReturnBtn);
        totalSection.setAlignment(Pos.CENTER_RIGHT);
        totalSection.setPadding(new Insets(10));

        // ===== ROOT LAYOUT =====
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(topBar, transactionSelectionBox, tableView, reasonBox, totalSection);

        // Load transaction IDs
        loadTransactions();

        Scene scene = new Scene(root, 950, 600);
        primaryStage.setTitle("Return Barang");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadTransactions() {
        ObservableList<String> transactions = FXCollections.observableArrayList();

        try (Connection conn = db_connect.getConnection()) {
            String sql = "SELECT transaction_id, transaction_date FROM transactions " +
                    "WHERE user_id = ? AND type = 'PURCHASE' ORDER BY transaction_date DESC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUser.getId());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String transactionId = rs.getString("transaction_id");
                        String transactionDate = rs.getTimestamp("transaction_date").toString();
                        transactions.add(transactionId + " (" + transactionDate + ")");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading transactions: " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", "Failed to load transactions.");
        }

        transactionComboBox.setItems(transactions);
    }

    private void loadTransactionItems() {
        tableView.getItems().clear();
        selectedItems.clear();
        updateTotalLabel();

        String selected = transactionComboBox.getValue();
        if (selected == null || selected.isEmpty()) {
            return;
        }

        // Extract transaction ID from selected value
        String transactionId = selected.split(" \\(")[0];

        try (Connection conn = db_connect.getConnection()) {
            String sql = "SELECT ti.product_code, ti.product_name, ti.quantity, ti.price " +
                    "FROM transaction_items ti " +
                    "WHERE ti.transaction_id = ? AND (ti.returned = FALSE OR ti.returned IS NULL)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, transactionId);

                try (ResultSet rs = stmt.executeQuery()) {
                    ObservableList<TransactionItemView> items = FXCollections.observableArrayList();

                    while (rs.next()) {
                        TransactionItemView item = new TransactionItemView(
                                transactionId,
                                rs.getString("product_code"),
                                rs.getString("product_name"),
                                rs.getInt("quantity"),
                                rs.getDouble("price")
                        );

                        // Add listener to checkbox
                        item.getSelectCheckbox().selectedProperty().addListener((obs, oldVal, newVal) -> {
                            if (newVal) {
                                selectedItems.add(item);
                            } else {
                                selectedItems.remove(item);
                            }
                            updateTotalLabel();
                        });

                        items.add(item);
                    }

                    tableView.setItems(items);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading transaction items: " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", "Failed to load transaction items.");
        }
    }

    private void updateTotalLabel() {
        double total = 0;
        for (TransactionItemView item : selectedItems) {
            total += item.getPrice() * item.getQuantity();
        }

        totalLabel.setText("Total Return: " + String.format("Rp%,.2f", total).replace('.', ','));
    }

    private void processReturn(Stage stage) {
        if (selectedItems.isEmpty()) {
            showWarning("Tidak ada item", "Pilih minimal satu item untuk direturn.");
            return;
        }

        String reason = reasonField.getText().trim();
        if (reason.isEmpty()) {
            showWarning("Alasan kosong", "Masukkan alasan untuk return barang.");
            return;
        }

        // Get transaction ID from selected transaction
        String selected = transactionComboBox.getValue();
        String transactionId = selected.split(" \\(")[0];

        // Create refund transaction
        RefundTransaction refund = new RefundTransaction(
                transactionId,
                currentUser.getId(),
                currentUser.getUsername(),
                reason
        );

        // Add selected items to refund
        for (TransactionItemView item : selectedItems) {
            Product product = new RefundProduct(
                    item.getProductCode(),
                    item.getProductName(),
                    item.getPrice()
            );

            CartItem cartItem = new CartItem(product, item.getQuantity());
            refund.addItem(cartItem);
        }

        // Process refund
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Return");
        confirmAlert.setHeaderText("Konfirmasi Return Barang");
        confirmAlert.setContentText("Total pengembalian: " +
                String.format("Rp%,.2f", refund.calculateTotal()).replace('.', ',') +
                "\nLanjutkan proses return?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Process the refund
            refund.processTransaction();

            if (refund.serializeTransaction()) {
                showInfo("Return Berhasil",
                        "Return barang berhasil diproses.\n" +
                                "Total pengembalian: " + refund.getAmountPaidFormatted());

                // Return to shopping cart
                KeranjangUser keranjang = new KeranjangUser(currentUser);
                keranjang.start(new Stage());
                stage.close();
            } else {
                showError("Database Error", "Gagal menyimpan transaksi return ke database.");
            }
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Inner class to represent transaction items with checkbox
    public static class TransactionItemView {
        private final String transactionId;
        private final String productCode;
        private final String productName;
        private final int quantity;
        private final double price;
        private final CheckBox selectCheckbox;

        public TransactionItemView(String transactionId, String productCode, String productName,
                                   int quantity, double price) {
            this.transactionId = transactionId;
            this.productCode = productCode;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.selectCheckbox = new CheckBox();
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getProductCode() {
            return productCode;
        }

        public String getProductName() {
            return productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getPrice() {
            return price;
        }

        public CheckBox getSelectCheckbox() {
            return selectCheckbox;
        }
    }
}