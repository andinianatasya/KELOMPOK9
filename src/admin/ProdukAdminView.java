package admin;

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
import javafx.stage.Stage;
import model.Product;
import model.DataProduk;
import javafx.beans.property.SimpleStringProperty;
import java.sql.PreparedStatement;
import model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class ProdukAdminView {
    private User currentUser;
    private Stage stage;

    ObservableList<Product> masterData = FXCollections.observableArrayList();
    FilteredList<Product> filteredData = new FilteredList<>(masterData, p -> true);

    public ProdukAdminView(User user, Stage stage) {
        this.currentUser = user;
        this.stage = stage;
        initialize();
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

        Image userImage = new Image(getClass().getResourceAsStream("/img/user.png"));
        ImageView userIcon = new ImageView(userImage);
        userIcon.setFitHeight(30);
        userIcon.setPreserveRatio(true);

        HBox iconContainer = new HBox(10, tambahBtn, userIcon);
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
                                try (Connection conn = DriverManager.getConnection("jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres", "postgres.jnmxqxmrgwmmupkozavo", "kelompok9")) {
                                    PreparedStatement ps = conn.prepareStatement("UPDATE produk SET harga = ? WHERE kode = ?");
                                    ps.setDouble(1, newHarga);
                                    ps.setString(2, p.getKode());
                                    ps.executeUpdate();

                                    p.setHarga(newHarga); // update objek lokal
                                    table.refresh(); // refresh tampilan tabel
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
                                try (Connection conn = DriverManager.getConnection("jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres", "postgres.jnmxqxmrgwmmupkozavo", "kelompok9")) {
                                    PreparedStatement ps = conn.prepareStatement("DELETE FROM produk WHERE kode = ?");
                                    ps.setString(1, p.getKode());
                                    ps.executeUpdate();

                                    masterData.remove(p);
                                    filteredData.remove(p);
                                    table.refresh();

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
            Connection conn = DriverManager.getConnection("jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres", "postgres.jnmxqxmrgwmmupkozavo", "kelompok9"); // ganti sesuai koneksi
            DataProduk.loadProdukFromDatabase(conn);
            List<Product> produkList = DataProduk.getProdukList();
            masterData.addAll(produkList);
            conn.close();
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
}
