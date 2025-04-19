package user;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class KeranjangUser extends Application {
    @Override
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

        // ===== TOMBOL TAMBAH & USER =====
        Image kembaliImage = new Image(getClass().getResourceAsStream("/img/kembali.png"));
        ImageView kembaliIcon = new ImageView(kembaliImage);
        kembaliIcon.setFitHeight(30);
        kembaliIcon.setPreserveRatio(true);
        Button kembaliBtn = new Button();
        kembaliBtn.setGraphic(kembaliIcon);
        kembaliBtn.setStyle("-fx-background-color: transparent;");

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

        // ===== ROOT LAYOUT =====
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(topBar);

        Scene scene = new Scene(root, 950, 500);
        primaryStage.setTitle("Halaman Keranjang");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
