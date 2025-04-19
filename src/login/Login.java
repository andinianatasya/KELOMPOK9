package login;

import dao.UserDao;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import model.Auth;
import model.User;
import admin.ProdukAdminView;
import user.ProdukUserView;

import java.sql.SQLException;

public class Login extends Application {

    private UserDao userDAO;

    public Login() {
        this.userDAO = new UserDao();
    }

    private VBox createLoginForm(StackPane container, VBox registerForm) {
        VBox loginForm = new VBox(15);
        loginForm.setAlignment(Pos.CENTER);
        loginForm.setPadding(new Insets(30));
        loginForm.setStyle("-fx-background-color: white; -fx-background-radius: 12;");

        Label title = new Label("Masuk");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setPrefWidth(300);

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setPrefWidth(300);

        Button loginButton = new Button("Masuk");
        loginButton.setStyle("-fx-background-color: #1e90ff; -fx-text-fill: white; -fx-font-size: 16px;");
        loginButton.setPrefWidth(300);

        // Add login functionality
        loginButton.setOnAction(e -> {
            String inputUsername = username.getText().trim();
            String inputPassword = password.getText().trim();

            errorLabel.setVisible(false);

            if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
                errorLabel.setText("Username dan password tidak boleh kosong!");
                errorLabel.setVisible(true);
                return;
            }

            try {
                User authenticatedUser = userDAO.authenticate(inputUsername, inputPassword);
                if (authenticatedUser != null) {
                    Stage currentStage = (Stage) loginButton.getScene().getWindow();

                    // Redirect based on role
                    if ("admin".equalsIgnoreCase(authenticatedUser.getRole())) {
                        new ProdukAdminView(authenticatedUser, currentStage);
                    } else {
                        new ProdukUserView(authenticatedUser, currentStage);
                    }
                } else {
                    errorLabel.setText("Username atau password salah!");
                    errorLabel.setVisible(true);
                }
            } catch (SQLException ex) {
                errorLabel.setText("Error terjadi: " + ex.getMessage());
                errorLabel.setVisible(true);
                ex.printStackTrace();
            }
        });

        Text switchToRegister = new Text("Belum punya akun? Daftar di sini");
        switchToRegister.setStyle("-fx-fill: #1e90ff; -fx-underline: true;");
        switchToRegister.setOnMouseClicked(e -> {
            container.getChildren().setAll(registerForm);
        });

        loginForm.getChildren().addAll(title, errorLabel, username, password, loginButton, switchToRegister);
        return loginForm;
    }

    private VBox createRegisterForm(StackPane container, VBox loginForm) {
        VBox registerForm = new VBox(15);
        registerForm.setAlignment(Pos.CENTER);
        registerForm.setPadding(new Insets(30));
        registerForm.setStyle("-fx-background-color: white; -fx-background-radius: 12;");

        Label title = new Label("Daftar Akun");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);

        TextField username = new TextField();
        username.setPromptText("Username (min. 8 karakter)");
        username.setPrefWidth(300);

        PasswordField password = new PasswordField();
        password.setPromptText("Password (min. 8 karakter, huruf & angka)");
        password.setPrefWidth(300);

        Button registerButton = new Button("Daftar");
        registerButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 16px;");
        registerButton.setPrefWidth(300);

        // Add registration functionality
        registerButton.setOnAction(e -> {
            String inputUsername = username.getText().trim();
            String inputPassword = password.getText().trim();

            errorLabel.setVisible(false);

            // Validate username
            if (!Auth.isValidUsername(inputUsername)) {
                errorLabel.setText("Username harus minimal 8 karakter!");
                errorLabel.setVisible(true);
                return;
            }

            // Validate password
            if (!Auth.isValidPassword(inputPassword)) {
                errorLabel.setText("Password harus minimal 8 karakter dan berisi huruf dan angka!");
                errorLabel.setVisible(true);
                return;
            }

            try {
                boolean registered = userDAO.registerUser(inputUsername, inputPassword);
                if (registered) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Registrasi Berhasil");
                    alert.setHeaderText(null);
                    alert.setContentText("Akun berhasil dibuat! Silahkan masuk dengan akun baru Anda.");
                    alert.showAndWait();

                    container.getChildren().setAll(loginForm);
                } else {
                    errorLabel.setText("Username sudah digunakan. Silahkan pilih username lain.");
                    errorLabel.setVisible(true);
                }
            } catch (SQLException ex) {
                errorLabel.setText("Error terjadi: " + ex.getMessage());
                errorLabel.setVisible(true);
                ex.printStackTrace();
            }
        });

        Text switchToLogin = new Text("Sudah punya akun? Masuk di sini");
        switchToLogin.setStyle("-fx-fill: #1e90ff; -fx-underline: true;");
        switchToLogin.setOnMouseClicked(e -> {
            container.getChildren().setAll(loginForm);
        });

        registerForm.getChildren().addAll(title, errorLabel, username, password, registerButton, switchToLogin);
        return registerForm;
    }

    @Override
    public void start(Stage primaryStage) {
        // Set up the admin user if it doesn't exist
        try {
            userDAO.setupAdminUser();
        } catch (SQLException e) {
            System.out.println("Failed to create admin user: " + e.getMessage());
            e.printStackTrace();
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #c6bebe;");

        // Bagian kiri: logo
        VBox logoBox = new VBox();
        logoBox.setAlignment(Pos.CENTER);
        logoBox.setPrefWidth(400);
        Image logo = new Image(getClass().getResourceAsStream("/img/logo.png"));
        ImageView logoView = new ImageView(logo);
        logoView.setFitWidth(250);
        logoView.setPreserveRatio(true);
        logoBox.getChildren().add(logoView);

        // StackPane untuk bergantian form login dan register
        StackPane formContainer = new StackPane();

        // Buat form login dan daftar
        VBox loginForm = createLoginForm(formContainer, null);
        VBox registerForm = createRegisterForm(formContainer, loginForm);

        // Perbaiki referensi silang
        VBox fixedLoginForm = createLoginForm(formContainer, registerForm);
        VBox fixedRegisterForm = createRegisterForm(formContainer, fixedLoginForm);
        formContainer.getChildren().add(fixedLoginForm);

        // Gabungkan kiri dan kanan
        HBox mainLayout = new HBox(50);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(40));
        mainLayout.getChildren().addAll(logoBox, formContainer);

        root.setCenter(mainLayout);

        Scene scene = new Scene(root, 1200, 600);
        primaryStage.setTitle("Aplikasi POS");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}