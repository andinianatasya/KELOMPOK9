package user;

import javafx.application.Application;
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

import java.util.Optional;

public class KeranjangUser {
    private User currentUser;
    private ShoppingCart cart;
    private TableView<CartItem> tableView;
    private Label totalLabel;

    public KeranjangUser() {
    }

    public KeranjangUser(User user) {
        this.currentUser = user;
        this.cart = ShoppingCart.getInstance();
        this.cart.setCurrentUser(user);
    }

    public void start(Stage primaryStage) {
        // ===== LOGO =====
        Image logo = new Image(getClass().getResourceAsStream("/img/logo.png"));
        ImageView logoView = new ImageView(logo);
        logoView.setFitHeight(40);
        logoView.setPreserveRatio(true);

        // ===== LABEL "Keranjang Anda" =====
        Label keranjangLabel = new Label("Keranjang Anda");
        keranjangLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox keranjangBar = new HBox(keranjangLabel);
        keranjangBar.setAlignment(Pos.CENTER);

        // ===== TOMBOL KEMBALI & USER =====
        Image kembaliImage = new Image(getClass().getResourceAsStream("/img/kembali.png"));
        ImageView kembaliIcon = new ImageView(kembaliImage);
        kembaliIcon.setFitHeight(30);
        kembaliIcon.setPreserveRatio(true);
        Button kembaliBtn = new Button();
        kembaliBtn.setGraphic(kembaliIcon);
        kembaliBtn.setStyle("-fx-background-color: transparent;");
        kembaliBtn.setOnAction(e -> primaryStage.close());

        Image userImage = new Image(getClass().getResourceAsStream("/img/user.png"));
        ImageView userIcon = new ImageView(userImage);
        userIcon.setFitHeight(30);
        userIcon.setPreserveRatio(true);

        HBox iconContainer = new HBox(10, kembaliBtn, userIcon);
        iconContainer.setAlignment(Pos.CENTER_RIGHT);

        // ===== TOP BAR =====
        BorderPane topBar = new BorderPane();
        topBar.setLeft(logoView);
        topBar.setCenter(keranjangBar);
        topBar.setRight(iconContainer);
        topBar.setPadding(new Insets(10));

        // ===== TABLE CART ITEMS =====
        tableView = new TableView<>();
        tableView.setPrefWidth(900);
        tableView.setPrefHeight(300);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("Keranjang belanja Anda kosong"));

        TableColumn<CartItem, String> kodeCol = new TableColumn<>("Kode");
        kodeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProduct().getKode()));

        TableColumn<CartItem, String> namaCol = new TableColumn<>("Nama Barang");
        namaCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProduct().getNama()));

        TableColumn<CartItem, String> hargaCol = new TableColumn<>("Harga Satuan");
        hargaCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProduct().getHargaFormatted()));

        TableColumn<CartItem, Integer> qtyCol = new TableColumn<>("Jumlah");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<CartItem, String> subtotalCol = new TableColumn<>("Subtotal");
        subtotalCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSubtotalFormatted()));

        TableColumn<CartItem, Void> aksiCol = new TableColumn<>("Aksi");
        aksiCol.setCellFactory(col -> new TableCell<CartItem, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Hapus");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                editBtn.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    showEditQuantityDialog(item);
                });

                deleteBtn.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Apakah Anda yakin ingin menghapus " + item.getProduct().getNama() + " dari keranjang?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            cart.removeItem(item.getProduct().getKode());
                            refreshTableData();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        tableView.getColumns().addAll(kodeCol, namaCol, hargaCol, qtyCol, subtotalCol, aksiCol);

        // ===== TOTAL SECTION =====
        totalLabel = new Label("Total: Rp0,00");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button checkoutBtn = new Button("Checkout");
        checkoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        checkoutBtn.setOnAction(e -> handleCheckout(primaryStage));

        HBox totalSection = new HBox(20, totalLabel, checkoutBtn);
        totalSection.setAlignment(Pos.CENTER_RIGHT);
        totalSection.setPadding(new Insets(10));

        // ===== ROOT LAYOUT =====
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(topBar, tableView, totalSection);

        // Load data into table
        refreshTableData();

        Scene scene = new Scene(root, 950, 500);
        primaryStage.setTitle("Keranjang Belanja");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void refreshTableData() {
        try {
            // Create new observable list from cart items
            ObservableList<CartItem> items = FXCollections.observableArrayList();
            items.addAll(cart.getItems());

            // Set items to table
            tableView.getItems().clear();
            tableView.getItems().addAll(items);

            // Update total label
            totalLabel.setText("Total: " + cart.getTotalFormatted());

            // Force refresh table
            tableView.refresh();
        } catch (Exception e) {
            System.err.println("Error updating table view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showEditQuantityDialog(CartItem item) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Edit Jumlah");
        dialog.setHeaderText("Edit jumlah untuk " + item.getProduct().getNama());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Spinner<Integer> quantitySpinner = new Spinner<>();
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, item.getQuantity());
        quantitySpinner.setValueFactory(valueFactory);
        quantitySpinner.setEditable(true);

        grid.add(new Label("Jumlah:"), 0, 0);
        grid.add(quantitySpinner, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                return quantitySpinner.getValue();
            }
            return null;
        });

        Optional<Integer> result = dialog.showAndWait();

        result.ifPresent(quantity -> {
            try {
                // Update quantity in the cart
                cart.updateItemQuantity(item.getProduct().getKode(), quantity);

                // Refresh table
                refreshTableData();
            } catch (Exception e) {
                System.err.println("Error updating quantity: " + e.getMessage());
                e.printStackTrace();

                // Show error message to user
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Gagal mengupdate jumlah barang: " + e.getMessage());
                alert.showAndWait();
            }
        });
    }

    private void handleCheckout(Stage stage) {
        if (cart.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Keranjang belanja Anda kosong!");
            alert.showAndWait();
            return;
        }

        // Confirm checkout
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Checkout");
        confirmAlert.setHeaderText("Konfirmasi Pesanan Anda");
        confirmAlert.setContentText("Total belanja: " + cart.getTotalFormatted() +
                "\nLanjutkan checkout?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Process transaction
            PurchaseTransaction transaction = cart.checkout();

            if (transaction != null) {
                // Show success message
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Checkout Berhasil");
                successAlert.setHeaderText("Transaksi Berhasil");
                successAlert.setContentText("ID Transaksi: " + transaction.getTransactionId() +
                        "\nTotal: " + String.format("Rp%,.2f", transaction.calculateTotal()).replace('.', ',') +
                        "\n\nTerima kasih telah berbelanja!");
                successAlert.showAndWait();

                // Close cart window
                stage.close();
            } else {
                // Show error
                Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Gagal memproses transaksi!");
                errorAlert.showAndWait();
            }
        }
    }
}