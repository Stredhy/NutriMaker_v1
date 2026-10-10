package com.javafx.nutrimaker;

import static com.javafx.nutrimaker.animations.AnimationPersonalized.*;
import java.net.URL;
import com.javafx.nutrimaker.repository.PatientRepository;
import java.io.IOException;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.StageStyle;

public class PatientFormController implements Initializable {
     
    static final String REG_EXP_POSIT_FLOAT = "\\d+(\\.\\d*)?";
    static final String REG_EXP_POSIT_INT = "\\d+";
    
    private String userEmail;
    private int patientId;
    
    @FXML
    private TextField nameTextField;
    
    @FXML
    private TextField ageTextField;

    @FXML
    private TextField weightTextField;

    @FXML
    private TextField heightTextField;

    @FXML
    private Button saveButton;
    
    public void setUserEmail(String uE) {
        userEmail = uE;
    }
    

    
    
    
    @FXML
    private void save(ActionEvent event) throws IOException {
        if (checkInputs()) {
            int age = Integer.parseInt(ageTextField.getText());
            double weight = Double.parseDouble(weightTextField.getText());
            double height = Double.parseDouble(heightTextField.getText());

            // Crear el paciente y recuperar su ID real
            int patientId = new PatientRepository().createPatientAndGetId(
                    nameTextField.getText(),
                    age,
                    weight,
                    height
            );

            System.out.println("Paciente creado con ID: " + patientId);

            // Abrir la ventana de creación de dieta pasando el ID
            createDiet(event, patientId);
        }
    }
    
    //1 vacias, 2 formatos, 
    public void dialog(String warningMessage) throws IOException {
        FXMLLoader loader =new FXMLLoader(getClass().getResource("DialogInputs.fxml"));
        Parent parent = loader.load();
        DialogController dialog = loader.getController();
        dialog.setText(warningMessage);
        Scene scene = new Scene(parent);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }
        
    public boolean checkInputs() throws IOException {
        if (nameTextField.getText().isEmpty() || ageTextField.getText().isEmpty() || weightTextField.getText().isEmpty() || heightTextField.getText().isEmpty()) {
            dialog("No pueden existir campos vacios ");
            return false;
        }
        if (!ageTextField.getText().matches(REG_EXP_POSIT_INT)) {
            dialog("Solo se permiten números enteros");
            return false;
        }
        if (!heightTextField.getText().matches(REG_EXP_POSIT_FLOAT) || !weightTextField.getText().matches(REG_EXP_POSIT_FLOAT)) {
            dialog("Solo se permiten números");
            return false;
        }
        
        return true;
    }
    
    private void createDiet(ActionEvent event, int patientId) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("CreateDiet.fxml")
        );

        Parent root = loader.load();

        // Pasar el ID del paciente al controlador de la dieta
        CreateDietController controller = loader.getController();
        controller.setPatientId(patientId);

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();

        ((CreateDietController) loader.getController()).setPatientId(patientId);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Crear Dieta");
        stage.show();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setFadeAnimation(nameTextField);
        setFadeAnimation(ageTextField);
        setFadeAnimation(weightTextField);
        setFadeAnimation(heightTextField);
        setFadeAndScaleAnimation(saveButton);
    }    
    
}
