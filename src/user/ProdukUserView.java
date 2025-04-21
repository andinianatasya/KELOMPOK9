package user;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;
import javafx.scene.Cursor;


public class ProdukUserView {

    private User currentUser;
    private Stage stage;
    private ShoppingCart cart;
    private Connection conn;

    public ProdukUserView(User user, Stage stage) {
        this.currentUser = user;
        this.stage = stage;
        this.cart = ShoppingCart.getInstance();
        this.cart.setCurrentUser(user);
        initialize();
    }

    private Connection getConnection() throws Exception {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(
                    "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres",
                    "postgres.jnmxqxmrgwmmupkozavo",
                    "kelompok9");
        }
        return conn;
    }

    private void initialize() {
        // ===== LOGO =====
        Image logo = new Image(getClass().getResourceAsStream("/img/logo.png"));
        ImageView logoView = new ImageView(logo);
        logoView.setFitHeight(40);
        logoView.setPreserveRatio(true);

        // ===== SEARCH BAR =====
        TextField searchField = new TextField();
        searchField.setPromptText("Cari Produk (Kode)");
        searchField.setPrefWidth(300);
        HBox searchBar = new HBox(10, new Label("🔍"), searchField);
        searchBar.setAlignment(Pos.CENTER);

        // ===== ICON KERANJANG & USER =====
        Image cartImage = new Image(getClass().getResourceAsStream("/img/cart.png"));
        ImageView cartIcon = new ImageView(cartImage);
        cartIcon.setFitHeight(30);
        cartIcon.setPreserveRatio(true);
        Tooltip cartTooltip = new Tooltip("Keranjang");
        Tooltip.install(cartIcon,cartTooltip);
        cartIcon.setCursor(Cursor.HAND);

        // Update to show cart item count
        Label cartCountLabel = new Label("0");
        cartCountLabel.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-padding: 2px 6px; -fx-background-radius: 10;");

        StackPane cartPane = new StackPane();
        cartPane.getChildren().add(cartIcon);
        StackPane.setAlignment(cartCountLabel, Pos.TOP_RIGHT);
        cartPane.getChildren().add(cartCountLabel);

        cartPane.setOnMouseClicked(event -> {
            try {
                KeranjangUser keranjang = new KeranjangUser(currentUser);
                Stage keranjangStage = new Stage();
                keranjang.start(keranjangStage);

                // Update cart count when returning from cart screen
                keranjangStage.setOnHidden(e -> {
                    updateCartCountLabel(cartCountLabel);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Initial update of cart count
        updateCartCountLabel(cartCountLabel);

        Image aktivitasImage = new Image(getClass().getResourceAsStream("/img/aktivitas.png"));
        ImageView aktivitasIcon = new ImageView(aktivitasImage);
        aktivitasIcon.setFitHeight(30);
        aktivitasIcon.setPreserveRatio(true);
        Tooltip aktivitasTooltip = new Tooltip("History");
        Tooltip.install(aktivitasIcon,aktivitasTooltip);
        aktivitasIcon.setCursor(Cursor.HAND);
        aktivitasIcon.setOnMouseClicked(e -> {
            try {
                showUserActivityPopup(currentUser);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        Image userImage = new Image(getClass().getResourceAsStream("/img/user.png"));
        ImageView userIcon = new ImageView(userImage);
        userIcon.setFitHeight(30);
        userIcon.setPreserveRatio(true);
        Tooltip userTooltip = new Tooltip("Akun saya");
        Tooltip.install(userIcon,userTooltip);
        userIcon.setCursor(Cursor.HAND);

        userIcon.setOnMouseClicked(event -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Profil Pengguna");

            Label nameLabel = new Label("Nama panggilan:");
            TextField nameField = new TextField(currentUser.getNama());

            VBox content = new VBox(10, nameLabel, nameField);
            content.setPadding(new Insets(10));

            ButtonType updateButtonType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
            ButtonType deleteButtonType = new ButtonType("Hapus Akun", ButtonBar.ButtonData.LEFT);
            dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, deleteButtonType, ButtonType.CANCEL);
            dialog.getDialogPane().setContent(content);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == updateButtonType) {
                    return nameField.getText();
                } else if (dialogButton == deleteButtonType) {
                    return "DELETE";
                }
                return null;
            });

            dialog.showAndWait().ifPresent(result -> {
                if ("DELETE".equals(result)) {
                    boolean confirmed = confirmDelete();
                    if (confirmed) {
                        deleteUserFromDatabase(currentUser.getId());
                        stage.close(); // keluar dari aplikasi setelah hapus akun
                    }
                } else if (result != null && !result.trim().isEmpty()) {
                    updateNamaInDatabase(currentUser.getId(), result.trim());
                    currentUser.setNama(result.trim()); // update di objek lokal
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Nama berhasil diperbarui!");
                    alert.showAndWait();
                }
            });
        });


        HBox iconContainer = new HBox(10, cartPane, aktivitasIcon, userIcon);
        iconContainer.setAlignment(Pos.CENTER_RIGHT);

        BorderPane topBar = new BorderPane();
        topBar.setLeft(logoView);
        topBar.setCenter(searchBar);
        topBar.setRight(iconContainer);
        topBar.setPadding(new Insets(10));

        // ===== TABLE PRODUK =====
        TableView<Product> table = new TableView<>();
        table.setPrefWidth(900);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-table-cell-border-color: transparent; -fx-border-color: transparent;");

        TableColumn<Product, String> kodeCol = new TableColumn<>("Kode");
        kodeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKode()));

        TableColumn<Product, String> namaCol = new TableColumn<>("Nama Barang");
        namaCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNama()));

        TableColumn<Product, String> hargaCol = new TableColumn<>("Harga");
        hargaCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getHargaFormatted()));

        TableColumn<Product, String> tipeCol = new TableColumn<>("Tipe");
        tipeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipe()));

        TableColumn<Product, Void> aksiCol = new TableColumn<>("");
        aksiCol.setPrefWidth(100);
        // Event handler saat baris tabel diklik
        table.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    Product clickedProduct = row.getItem();
                    // Tampilkan pop-up untuk semua jenis produk
                    showProductDetailPopup(clickedProduct);
                }
            });
            return row;
        });

        aksiCol.setCellFactory(col -> new TableCell<>() {
            private final Button addBtn = new Button();

            {
                Image cartImg = new Image(getClass().getResourceAsStream("/img/cart.png"));
                ImageView cartView = new ImageView(cartImg);
                cartView.setFitHeight(20);
                cartView.setPreserveRatio(true);
                addBtn.setGraphic(cartView);
                addBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                addBtn.setTooltip(new Tooltip("Tambahkan ke Keranjang"));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product p = getTableView().getItems().get(getIndex());
                    addBtn.setOnAction(e -> {
                        showAddToCartDialog(p, cartCountLabel);
                    });
                    setGraphic(addBtn);
                }
            }
        });

        table.getColumns().addAll(kodeCol, namaCol, hargaCol, tipeCol, aksiCol);

        // ===== Ambil data dari database via DataProduk.java =====
        ObservableList<Product> masterData = FXCollections.observableArrayList();
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres", "postgres.jnmxqxmrgwmmupkozavo", "kelompok9");
            DataProduk.loadProdukFromDatabase(conn);
            List<Product> produkList = DataProduk.getProdukList();
            masterData.addAll(produkList);
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Gagal mengambil data produk.");
            alert.show();
        }

        // FilteredList untuk pencarian
        FilteredList<Product> filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(product -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return product.getKode().toLowerCase().contains(lowerCaseFilter) ||
                        product.getNama().toLowerCase().contains(lowerCaseFilter);
            });
        });

        table.setItems(filteredData);

        // ===== WRAPPER TABEL =====
        HBox tableWrapper = new HBox(table);
        tableWrapper.setAlignment(Pos.CENTER);

        // ===== ROOT LAYOUT =====
        VBox root = new VBox(30);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.getChildren().addAll(topBar, tableWrapper);

        Scene scene = new Scene(root, 950, 500);
        stage.setTitle("Tampilan User");
        stage.setScene(scene);
        stage.show();
    }

    private void showAddToCartDialog(Product product, Label cartCountLabel) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Tambah ke Keranjang");
        dialog.setHeaderText("Tambahkan " + product.getNama() + " ke keranjang");

        ButtonType addButtonType = new ButtonType("Tambah", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Spinner<Integer> quantitySpinner = new Spinner<>();
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
        quantitySpinner.setValueFactory(valueFactory);
        quantitySpinner.setEditable(true);

        grid.add(new Label("Jumlah:"), 0, 0);
        grid.add(quantitySpinner, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return quantitySpinner.getValue();
            }
            return null;
        });

        Optional<Integer> result = dialog.showAndWait();

        result.ifPresent(quantity -> {
            // Add to cart
            cart.addItem(product, quantity);

            // Update cart count label
            updateCartCountLabel(cartCountLabel);

            // Show confirmation
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Berhasil");
            alert.setHeaderText(null);
            alert.setContentText(quantity + " " + product.getNama() + " telah ditambahkan ke keranjang!");
            alert.showAndWait();
        });
    }

    private void updateCartCountLabel(Label cartCountLabel) {
        int itemCount = cart.getItemCount();
        cartCountLabel.setText(String.valueOf(itemCount));
        cartCountLabel.setVisible(itemCount > 0);
    }

    private void updateNamaInDatabase(int userId, String newNama) {
        String sql = "UPDATE userk9 SET nama = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres", "postgres.jnmxqxmrgwmmupkozavo", "kelompok9");
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newNama);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Gagal memperbarui nama.");
            alert.show();
        }
    }

    private void deleteUserFromDatabase(int userId) {
        String sql = "DELETE FROM userk9 WHERE id = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres", "postgres.jnmxqxmrgwmmupkozavo", "kelompok9");
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Akun berhasil dihapus.");
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Gagal menghapus akun.");
            alert.show();
        }
    }

    private boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Apakah Anda yakin ingin menghapus akun?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Konfirmasi");
        alert.showAndWait();
        return alert.getResult() == ButtonType.YES;
    }


    private void showUserActivityPopup(User user) {
        Stage popupStage = new Stage();
        popupStage.setTitle("Aktivitas " + user.getUsername());

        TableView<Logs_aktivity> table = new TableView<>();

        TableColumn<Logs_aktivity, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<Logs_aktivity, String> activityCol = new TableColumn<>("Aktivitas");
        activityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getActivity()));

        TableColumn<Logs_aktivity, String> timestampCol = new TableColumn<>("Waktu");
        timestampCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimestamp().toString()));

        table.getColumns().addAll(statusCol, activityCol, timestampCol);

        try {
            Connection conn = getConnection(); // method milik class ini sendiri
            List<Logs_aktivity> logs = ShowLogs.getUserLogs(conn, user.getId());
            table.setItems(FXCollections.observableArrayList(logs));
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Gagal Memuat Aktivitas");
            alert.setContentText("Terjadi kesalahan saat mengambil data log aktivitas.");
            alert.showAndWait();
        }

        VBox layout = new VBox(10, table);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 600, 400);
        popupStage.setScene(scene);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.show();
    }
    private void showProductDetailPopup(Product product) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Detail Produk");

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Label titleLabel = new Label(product.getNama());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Layout utama untuk detail produk
        VBox detailLayout = new VBox(5);
        detailLayout.setAlignment(Pos.CENTER_LEFT);

        if (product instanceof PerishableProduct) {
            // Tampilkan informasi Perishable
            PerishableProduct perishable = (PerishableProduct)product;
            Label detailLabel = new Label(perishable.getDetail());
            detailLayout.getChildren().add(detailLabel);

        } else if (product instanceof DigitalProduct) {
            // Tampilkan informasi Digital
            DigitalProduct digital = (DigitalProduct)product;
            Label vendorLabel = new Label("Vendor: " + digital.getvendor());
            Label urlLabel = new Label("URL: " + digital.getURL().toString());
            detailLayout.getChildren().addAll(vendorLabel, urlLabel);

        } else if (product instanceof BundleProduct) {
            // Tampilkan informasi Bundle
            BundleProduct bundle = (BundleProduct)product;
            Label bundleLabel = new Label("Bundle berisi:");

            // Tabel untuk menampilkan isi bundle
            TableView<Product> bundleTable = new TableView<>();
            bundleTable.setPrefHeight(150);

            TableColumn<Product, String> namaCol = new TableColumn<>("Nama");
            namaCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNama()));
            namaCol.setPrefWidth(150);

            TableColumn<Product, String> hargaCol = new TableColumn<>("Harga");
            hargaCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getHargaFormatted()));
            hargaCol.setPrefWidth(100);

            bundleTable.getColumns().addAll(namaCol, hargaCol);
            bundleTable.setItems(FXCollections.observableArrayList(bundle.getIsiBundle()));

            detailLayout.getChildren().addAll(bundleLabel, bundleTable);
        }

        Button closeButton = new Button("Tutup");
        closeButton.setOnAction(e -> popupStage.close());

        layout.getChildren().addAll(titleLabel, detailLayout, closeButton);

        Scene scene = new Scene(layout, 300, product instanceof BundleProduct ? 300 : 150);
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }
}