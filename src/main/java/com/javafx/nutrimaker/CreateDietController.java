/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.javafx.nutrimaker;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import static com.javafx.nutrimaker.animations.AnimationPersonalized.*;

import com.javafx.nutrimaker.models.User;
import com.javafx.nutrimaker.repository.DietRepository;
import com.javafx.nutrimaker.repository.PatientRepository;
import com.javafx.nutrimaker.repository.UserRepository;
import com.javafx.nutrimaker.repository.MealRepository;
import com.javafx.nutrimaker.DialogController;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * FXML Controller class
 *
 * @author mimoe
 */
public class CreateDietController implements Initializable {

    private String userEmail;
    
    private int patientId;

    public void setPatientId(int patientId) { this.patientId = patientId; }

    @FXML
    private Button saveButton;

    @FXML
    private TextField calTextField;

    @FXML
    private TextField commentTextField;

    @FXML
    private ChoiceBox<String> quantityFoodDistribution;

    @FXML
    private ChoiceBox<String> selectFreeDay;

    @FXML
    public void dialog(String warningMessage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("DialogInputs.fxml"));
        Parent parent = loader.load();
        DialogController dialog = loader.getController();
        dialog.setText(warningMessage);
        Scene scene = new Scene(parent);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }

    private boolean checkInputs() throws IOException {
        int cantCalories;

        if (commentTextField.getText().isEmpty() || calTextField.getText().isEmpty() || selectFreeDay.getValue() == null || quantityFoodDistribution.getValue() == null) {
            dialog("No pueden existir entradas vacías");
            return false;
        }

        if (!calTextField.getText().matches("\\d+")) {
            dialog("Solo se permiten números");
            return false;
        }

        cantCalories = Integer.parseInt(calTextField.getText());
        if (!(0 < cantCalories && cantCalories <= 6000)) {
            dialog("Rango de calorías permitido 0 - 6000");
            return false;
        }
        return true;
    }

    private String getRestDay() {
        String day = selectFreeDay.getValue();
        return switch (day) {
            case "Lunes" -> "MONDAY";
            case "Martes" -> "TUESDAY";
            case "Miercoles" -> "WEDNESDAY";
            case "Jueves" -> "THURSDAY";
            case "Viernes" -> "FRIDAY";
            case "Sabado" -> "SATURDAY";
            case "Domingo" -> "SUNDAY";
            default -> "UNKNOWN";
        };
    }

    public void setUserEmail(String uE) {
        userEmail = uE;
    }

    @FXML
    private void save(ActionEvent event) throws IOException {
        if (checkInputs()) {
            double cantCalories = Double.parseDouble(calTextField.getText());
            int foodQuantity = Integer.parseInt(quantityFoodDistribution.getValue());
            String note = commentTextField.getText();
            MealRepository mealRepository = new MealRepository();
            if (mealRepository.createNewDiet(cantCalories, foodQuantity, getRestDay(), User.getUser().getId(), patientId, note)) {
                dialog("¡La dieta se ha creado con exito!");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("DietStorage.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Almacenamiento de Dietas");
                stage.show();
            } else {
                dialog("No se pudo crear la dieta. Revisa la conexión a MySQL y que el catálogo tenga alimentos suficientes.");
            }
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setFadeAndScaleAnimation(saveButton);
        String[] days = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"};
        selectFreeDay.getItems().addAll(days);
        String[] foodDistribution = {"3", "4", "5", "6"};
        quantityFoodDistribution.getItems().addAll(foodDistribution);
        // TODO
    }

}
