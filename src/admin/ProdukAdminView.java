package admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import java.sql.PreparedStatement;
import model.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Cursor;

public class ProdukAdminView {
    private User currentUser;
    private Stage stage;
    private Connection conn;

    ObservableList<Product> masterData = FXCollections.observableArrayList();
    FilteredList<Product> filteredData = new FilteredList<>(masterData, p -> true);

    public ProdukAdminView(User user, Stage stage) {
        this.currentUser = user;
        this.stage = stage;
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
        HBox searchBar = new HBox(10, new Label("\uD83D\uDD0D"), searchField);
        searchBar.setAlignment(Pos.CENTER);

        // ===== TOMBOL TAMBAH & USER =====
        Image tambahImage = new Image(getClass().getResourceAsStream("/img/tambah.png"));
        ImageView tambahIcon = new ImageView(tambahImage);
        tambahIcon.setFitHeight(30);
        tambahIcon.setPreserveRatio(true);
        Button tambahBtn = new Button();
        tambahBtn.setGraphic(tambahIcon);
        tambahBtn.setStyle("-fx-background-color: transparent;");
        // button tambah
        tambahBtn.setOnAction(e -> showAddProductDialog());

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

        HBox iconContainer = new HBox(10, tambahBtn, aktivitasIcon, userIcon);
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

        TableColumn<Product, Void> aksiCol = new TableColumn<>("");
        aksiCol.setPrefWidth(100);
        aksiCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button delBtn = new Button();
            private final HBox btnBox = new HBox(10);

            {
                Image editImg = new Image(getClass().getResourceAsStream("/img/edit.png"));
                ImageView editView = new ImageView(editImg);
                editView.setFitHeight(20);
                editView.setPreserveRatio(true);
                editBtn.setGraphic(editView);
                editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                editBtn.setTooltip(new Tooltip("Edit"));

                Image deleteImg = new Image(getClass().getResourceAsStream("/img/delete.png"));
                ImageView deleteView = new ImageView(deleteImg);
                deleteView.setFitHeight(20);
                deleteView.setPreserveRatio(true);
                delBtn.setGraphic(deleteView);
                delBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                delBtn.setTooltip(new Tooltip("Hapus"));

                btnBox.setAlignment(Pos.CENTER);
                btnBox.getChildren().addAll(editBtn, delBtn);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product p = getTableView().getItems().get(getIndex());

                    editBtn.setOnAction(e -> {
                        TextInputDialog dialog = new TextInputDialog(String.valueOf(p.getHarga()));
                        dialog.setTitle("Edit Harga");
                        dialog.setHeaderText("Edit Harga untuk " + p.getNama());
                        dialog.setContentText("Masukkan harga baru:");

                        dialog.showAndWait().ifPresent(newHargaStr -> {
                            try {
                                double newHarga = Double.parseDouble(newHargaStr);
                                try (Connection conn = getConnection()) {
                                    PreparedStatement ps = conn.prepareStatement("UPDATE produk SET harga = ? WHERE kode = ?");
                                    ps.setDouble(1, newHarga);
                                    ps.setString(2, p.getKode());
                                    ps.executeUpdate();

                                    p.setHarga(newHarga); // update objek lokal
                                    table.refresh(); // refresh tampilan tabel

                                    AdminLogs.logActivity(conn, currentUser.getId(), currentUser.getUsername(),
                                            "SUCSES", "Edit harga produk " + p.getKode() + " menjadi " + newHarga);
                                }
                            } catch (NumberFormatException ex) {
                                Alert err = new Alert(Alert.AlertType.ERROR, "Harga tidak valid.");
                                err.show();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Alert err = new Alert(Alert.AlertType.ERROR, "Gagal mengupdate harga: " + ex.getMessage());
                                err.show();
                            }
                        });
                    });

                    delBtn.setOnAction(e -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Konfirmasi Hapus");
                        alert.setHeaderText("Hapus Produk");
                        alert.setContentText("Apakah Anda yakin ingin menghapus produk \"" + p.getNama() + "\"?");

                        alert.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                try (Connection conn = getConnection()) {
                                    if (p.getTipe().equals("Perishable")) {
                                        PreparedStatement ps = conn.prepareStatement("DELETE FROM perishable_produk WHERE kode = ?");
                                        ps.setString(1, p.getKode());
                                        ps.executeUpdate();
                                    } else if (p.getTipe().equals("Digital")) {
                                        PreparedStatement ps = conn.prepareStatement("DELETE FROM digital_produk WHERE kode = ?");
                                        ps.setString(1, p.getKode());
                                        ps.executeUpdate();
                                    } else if (p.getTipe().equals("Bundle")) {
                                        PreparedStatement ps = conn.prepareStatement("DELETE FROM bundle_isi WHERE kode_bundle = ?");
                                        ps.setString(1, p.getKode());
                                        ps.executeUpdate();
                                    }

                                    PreparedStatement ps = conn.prepareStatement("DELETE FROM produk WHERE kode = ?");
                                    ps.setString(1, p.getKode());
                                    ps.executeUpdate();

                                    masterData.remove(p);
                                    filteredData.remove(p);
                                    table.refresh();

                                    AdminLogs.logActivity(conn, currentUser.getId(), currentUser.getUsername(),
                                            "SUCSES", "Hapus produk " + p.getKode() + " (" + p.getNama() + ")");

                                } catch (NumberFormatException ex) {
                                    Alert err = new Alert(Alert.AlertType.ERROR, "Harga tidak valid.");
                                    err.show();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    Alert err = new Alert(Alert.AlertType.ERROR, "Gagal mengupdate harga: " + ex.getMessage());
                                    err.show();
                                }
                            }
                        });
                    });

                    setGraphic(btnBox);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        table.getColumns().addAll(kodeCol, namaCol, hargaCol, aksiCol);

        // ===== Ambil data dari database =====
        
        try {
            Connection conn = getConnection(); // ganti sesuai koneksi
            DataProduk.loadProdukFromDatabase(conn);
            List<Product> produkList = DataProduk.getProdukList();
            masterData.addAll(produkList);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Gagal mengambil data produk.");
            alert.show();
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(product -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return product.getKode().toLowerCase().contains(lowerCaseFilter);
            });
        });

        table.setItems(filteredData);

        // ===== WRAPPER TABEL BIAR TENGAH =====
        HBox tableWrapper = new HBox(table);
        tableWrapper.setAlignment(Pos.CENTER);

        // ===== ROOT LAYOUT =====
        VBox root = new VBox(30);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.getChildren().addAll(topBar, tableWrapper);

        Scene scene = new Scene(root, 950, 500);
        stage.setTitle("Tampilan Admin");
        stage.setScene(scene);
        stage.show();
    }

    private void showAddProductDialog() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.setTitle("Tambah Produk");

        // Form fields for basic product info
        TextField kodeField = new TextField();
        kodeField.setPromptText("Kode Produk");

        TextField namaField = new TextField();
        namaField.setPromptText("Nama Produk");

        TextField hargaField = new TextField();
        hargaField.setPromptText("Harga");

        // Product type combobox
        ComboBox<String> tipeCombo = new ComboBox<>();
        tipeCombo.getItems().addAll("Non-Perishable", "Perishable", "Digital", "Bundle");
        tipeCombo.setPromptText("Pilih Tipe Produk");

        // Container for type-specific fields
        VBox typeFieldsContainer = new VBox(10);

        // Event to show/hide type-specific fields
        tipeCombo.setOnAction(e -> {
            typeFieldsContainer.getChildren().clear();

            String selectedType = tipeCombo.getValue();
            if (selectedType != null) {
                switch (selectedType) {
                    case "Perishable":
                        DatePicker expiryDatePicker = new DatePicker();
                        expiryDatePicker.setPromptText("Tanggal Kadaluarsa");
                        Label expiryLabel = new Label("Tanggal Kadaluarsa:");
                        typeFieldsContainer.getChildren().addAll(expiryLabel, expiryDatePicker);
                        break;

                    case "Digital":
                        TextField urlField = new TextField();
                        urlField.setPromptText("URL");
                        TextField vendorField = new TextField();
                        vendorField.setPromptText("Vendor");
                        Label urlLabel = new Label("URL:");
                        Label vendorLabel = new Label("Vendor:");
                        typeFieldsContainer.getChildren().addAll(urlLabel, urlField, vendorLabel, vendorField);
                        break;

                    case "Bundle":
                        Label infoLabel = new Label("Isi Bundle (pilih produk):");
                        ListView<String> produkListView = new ListView<>();
                        produkListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

                        // Load all existing products for selection
                        try {
                            Connection conn = getConnection();
                            DataProduk.loadProdukFromDatabase(conn);
                            List<Product> produkList = DataProduk.getProdukList();

                            // Filter out bundle products to avoid circular references
                            for (Product p : produkList) {
                                if (!p.getTipe().equals("Bundle")) {
                                    produkListView.getItems().add(p.getKode() + " - " + p.getNama() + " (" + p.getHargaFormatted() + ")");
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        produkListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                        produkListView.setPrefHeight(150);

                        typeFieldsContainer.getChildren().addAll(infoLabel, produkListView);
                        break;
                }
            }
        });

        Button saveButton = new Button("Simpan");
        saveButton.setOnAction(e -> {
            // Validate basic fields
            if (kodeField.getText().isEmpty() || namaField.getText().isEmpty() ||
                    hargaField.getText().isEmpty() || tipeCombo.getValue() == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Semua field harus diisi!");
                alert.showAndWait();
                return;
            }

            String kode = kodeField.getText();
            String nama = namaField.getText();
            double harga;

            try {
                harga = Double.parseDouble(hargaField.getText());
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Harga harus berupa angka!");
                alert.showAndWait();
                return;
            }

            String tipe = tipeCombo.getValue();

            try {
                Connection conn = getConnection();

                // First insert to main produk table
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO produk (kode, nama, harga, tipe) VALUES (?, ?, ?, ?)");
                ps.setString(1, kode);
                ps.setString(2, nama);
                ps.setDouble(3, harga);
                ps.setString(4, tipe);
                ps.executeUpdate();

                // Handle type-specific tables
                switch (tipe) {
                    case "Perishable":
                        DatePicker datePicker = (DatePicker) typeFieldsContainer.getChildren().get(1);
                        if (datePicker.getValue() == null) {
                            throw new Exception("Tanggal kadaluarsa harus diisi!");
                        }

                        LocalDate expiryDate = datePicker.getValue();
                        ps = conn.prepareStatement(
                                "INSERT INTO perishable_produk (kode, tanggal_kadaluarsa) VALUES (?, ?)");
                        ps.setString(1, kode);
                        ps.setDate(2, java.sql.Date.valueOf(expiryDate));
                        ps.executeUpdate();

                        // Add to local list
                        PerishableProduct newPerishable = new PerishableProduct(kode, nama, harga, expiryDate);
                        masterData.add(newPerishable);
                        break;

                    case "Digital":
                        TextField urlField = (TextField) typeFieldsContainer.getChildren().get(1);
                        TextField vendorField = (TextField) typeFieldsContainer.getChildren().get(3);

                        if (urlField.getText().isEmpty() || vendorField.getText().isEmpty()) {
                            throw new Exception("URL dan vendor harus diisi!");
                        }

                        URL url = new URL(urlField.getText());
                        String vendor = vendorField.getText();

                        ps = conn.prepareStatement(
                                "INSERT INTO digital_produk (kode, url, vendor) VALUES (?, ?, ?)");
                        ps.setString(1, kode);
                        ps.setString(2, url.toString());
                        ps.setString(3, vendor);
                        ps.executeUpdate();

                        // Add to local list
                        DigitalProduct newDigital = new DigitalProduct(kode, nama, harga, url, vendor);
                        masterData.add(newDigital);
                        break;

                    case "Bundle":
                        ListView<String> produkListView = (ListView<String>) typeFieldsContainer.getChildren().get(1);
                        ObservableList<String> selectedItems = produkListView.getSelectionModel().getSelectedItems();

                        if (selectedItems.size() < 1) {
                            throw new Exception("Bundle harus berisi minimal 2 produk!");
                        }

                        List<Product> bundleItems = new ArrayList<>();
                        for (String item : selectedItems) {
                            String produkKode = item.split(" - ")[0];

                            // Add to bundle_isi table
                            ps = conn.prepareStatement(
                                    "INSERT INTO bundle_isi (kode_bundle, kode_produk) VALUES (?, ?)");
                            ps.setString(1, kode);
                            ps.setString(2, produkKode);
                            ps.executeUpdate();

                            // Find product for local list
                            for (Product p : DataProduk.getProdukList()) {
                                if (p.getKode().equals(produkKode)) {
                                    bundleItems.add(p);
                                    break;
                                }
                            }
                        }

                        // Add to local list
                        BundleProduct newBundle = new BundleProduct(kode, nama, bundleItems);
                        masterData.add(newBundle);
                        break;

                    case "Non-Perishable":
                        // No additional fields needed
                        NonPerishableProduct newNonPerishable = new NonPerishableProduct(kode, nama, harga);
                        masterData.add(newNonPerishable);
                        break;
                }

                // Log activity
                AdminLogs.logActivity(conn, currentUser.getId(), currentUser.getUsername(),
                        "SUCSES", "Tambah produk " + kode + " (" + nama + ") tipe " + tipe);

                dialogStage.close();

                // Show success alert
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Sukses");
                success.setHeaderText("Produk Berhasil Ditambahkan");
                success.setContentText("Produk " + nama + " telah berhasil ditambahkan.");
                success.showAndWait();

            } catch (Exception ex) {
                ex.printStackTrace();
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Error");
                error.setHeaderText("Gagal Menambahkan Produk");
                error.setContentText("Error: " + ex.getMessage());
                error.showAndWait();
            }
        });

        Button cancelButton = new Button("Batal");
        cancelButton.setOnAction(e -> dialogStage.close());

        HBox buttonBox = new HBox(10, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Add fields to grid
        grid.add(new Label("Kode:"), 0, 0);
        grid.add(kodeField, 1, 0);
        grid.add(new Label("Nama:"), 0, 1);
        grid.add(namaField, 1, 1);
        grid.add(new Label("Harga:"), 0, 2);
        grid.add(hargaField, 1, 2);
        grid.add(new Label("Tipe:"), 0, 3);
        grid.add(tipeCombo, 1, 3);

        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));
        mainLayout.getChildren().addAll(grid, typeFieldsContainer, buttonBox);

        Scene dialogScene = new Scene(mainLayout, 400, 500);
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
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

}
