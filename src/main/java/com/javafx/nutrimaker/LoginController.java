package com.javafx.nutrimaker;

import static com.javafx.nutrimaker.animations.AnimationPersonalized.*;
import com.javafx.nutrimaker.models.User;
import com.javafx.nutrimaker.repository.UserRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.stage.StageStyle;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.control.Button;

public class LoginController implements Initializable {
    static final String REG_EXP_VER_EMAIL= "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,63}$";
    static final String REG_EXP_VER_PASS= ".{8,16}";
    
    @FXML
    private PasswordField passwordTextField;
    @FXML
    private TextField emailTextField;
    @FXML
    private Button signupButton;
    @FXML
    private Button loginButton;

    private boolean isEmpty() {
        return passwordTextField.getText().isEmpty() || emailTextField.getText().isEmpty();
    }
    
    private boolean checkPassword() {
        return passwordTextField.getText().matches(REG_EXP_VER_PASS);
    }
    
    private boolean checkEmail() {
        return emailTextField.getText().matches(REG_EXP_VER_EMAIL);
    }

    private boolean verifyCredentials() {
        UserRepository userRepo = new UserRepository();

        boolean result = userRepo.verifyPasswordByEmail(
            emailTextField.getText(),
            passwordTextField.getText()
        );
        return result;
    }
    private void dietStorage(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("DietStorage.fxml"));
        Parent root = loader.load();
        DietStorageController dietController = loader.getController();
        dietController.setUserEmail(emailTextField.getText());        
        
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Almacenamiento de Dietas");
        stage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    	Session.clear();
        setFadeAndScaleAnimation(loginButton);
        setFadeAndScaleAnimation(signupButton);
        setFadeAnimation(emailTextField);
        setFadeAnimation(passwordTextField);
        
        //db connection
    }
    
    public void dialog(String warningMessage) throws IOException {
        DialogHelper.showMessage(warningMessage);
    }

    public void invalidCredentials() throws IOException {
        DialogHelper.showFxml(DialogHelper.CREDENCIALES_INVALIDAS);
    }
    
    @FXML
    public void login(ActionEvent event) throws IOException {
        if(isEmpty()){
            dialog("No se admite texto vacio");
            return;
        }
        if (!checkPassword() || !checkEmail()) {
            invalidCredentials();
            return;
        }

        try {
            if (!verifyCredentials()) {
                invalidCredentials();
                return;
            }

            Integer userId = new UserRepository().getIdByEmail(emailTextField.getText());
            if (userId == null) {
                dialog("No fue posible identificar al usuario.");
                return;
            }

            Session.start(emailTextField.getText(), new UserRepository().getIdByEmail(emailTextField.getText()));
            
        } catch (java.io.UncheckedIOException | IllegalStateException e) {
            dialog("No fue posible conectar con el servicio. Inténtalo más tarde.");
            return;
        }

        dietStorage(event);  
    }

    @FXML
    public void signUp(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SignUpForm.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Register");
        stage.show();
    }
}
