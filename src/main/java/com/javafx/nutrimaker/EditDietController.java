package com.javafx.nutrimaker;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.javafx.nutrimaker.repository.PatientRepository;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ButtonType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.gson.JsonParser;
import com.javafx.nutrimaker.models.User;
import com.javafx.nutrimaker.repository.DietRepository;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import static com.javafx.nutrimaker.animations.AnimationPersonalized.setFadeAndScaleAnimation;
import javafx.stage.Stage;

public class EditDietController implements Initializable {
    @FXML private Label dietLabel;
    @FXML private TextField calTextField;
    @FXML private TextField commentTextField;
    @FXML private ChoiceBox<String> quantityFoodDistribution;
    @FXML private ChoiceBox<String> selectFreeDay;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button cloneButton;
    @FXML private RadioButton existingPatientRadio;
    @FXML private RadioButton newPatientRadio;
    @FXML private ComboBox<PatientChoice> existingPatientCombo;
    @FXML private TextField newPatientName;
    @FXML private TextField newPatientAge;
    @FXML private TextField newPatientWeight;
    @FXML private TextField newPatientHeight;
    @FXML private javafx.scene.layout.VBox existingPatientPane;
    @FXML private javafx.scene.layout.VBox newPatientPane;
    @FXML private Label originalPatientLabel;

    private final DietRepository repository = new DietRepository();
    private final PatientRepository patientRepository = new PatientRepository();
    private int originalPatientId;
    private PatientChoice originalPatient;
    private final List<PatientChoice> patients = new ArrayList<>();
    private int dietId;
    private String initialRestDay;
    private boolean saved;

    private static final String[] DAYS = {
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    };
    private static final String[] LABELS = {
        "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };

    @Override
    public void initialize(URL url, ResourceBundle bundle) {
        selectFreeDay.setItems(FXCollections.observableArrayList(LABELS));
        quantityFoodDistribution.setItems(FXCollections.observableArrayList("3", "4", "5", "6"));
        setFadeAndScaleAnimation(saveButton);
        setFadeAndScaleAnimation(cancelButton);
        setFadeAndScaleAnimation(cloneButton);
        ToggleGroup modes = new ToggleGroup();
        existingPatientRadio.setToggleGroup(modes);
        newPatientRadio.setToggleGroup(modes);
        existingPatientRadio.setSelected(true);
        modes.selectedToggleProperty().addListener((obs, old, value) -> togglePatientMode());
        togglePatientMode();
        existingPatientCombo.setEditable(true);
        existingPatientCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(PatientChoice p) { return p == null ? "" : p.toString(); }
            @Override public PatientChoice fromString(String s) {
                return patients.stream().filter(p -> p.toString().equalsIgnoreCase(s.trim())).findFirst().orElse(null);
            }
        });
        existingPatientCombo.getEditor().textProperty().addListener((obs, old, value) -> {
            if (existingPatientCombo.getSelectionModel().getSelectedItem() != null) return;
            String filter = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
            existingPatientCombo.getItems().setAll(patients.stream()
                .filter(p -> p.name.toLowerCase(java.util.Locale.ROOT).contains(filter)).toList());
        });
    }

    public void setDietId(int id) throws IOException {
    	if (!Session.isActive()) throw new IOException("No hay sesión activa");
        this.dietId = id;
        JsonObject data = JsonParser.parseString(
            repository.getDietByIdForUser(id, Session.requireUserId())
        ).getAsJsonObject();
        dietLabel.setText("Editar dieta #" + id);
        originalPatientId = data.get("patient_id").getAsInt();
        initialRestDay = data.has("rest_day") && !data.get("rest_day").isJsonNull()
            ? data.get("rest_day").getAsString() : null;
        loadPatients();
        calTextField.setText(data.get("calories").isJsonNull() ? "" : data.get("calories").getAsString());
        commentTextField.setText(data.get("note").isJsonNull() ? "" : data.get("note").getAsString());
        if (!data.get("meals_per_day").isJsonNull())
            quantityFoodDistribution.setValue(data.get("meals_per_day").getAsString());
        if (!data.get("rest_day").isJsonNull()) {
            String day = data.get("rest_day").getAsString();
            for (int i = 0; i < DAYS.length; i++) {
                if (DAYS[i].equalsIgnoreCase(day)) {
                    selectFreeDay.setValue(LABELS[i]);
                    break;
                }
            }
        }
    }


    private static final class PatientChoice {
        final int id, age;
        final String name;
        final double weight, height;
        PatientChoice(JsonObject row) {
            id = row.get("patient_id").getAsInt();
            name = row.get("name").getAsString();
            age = row.get("age").isJsonNull() ? 0 : row.get("age").getAsInt();
            weight = row.get("weight").isJsonNull() ? 0 : row.get("weight").getAsDouble();
            height = row.get("height").isJsonNull() ? 0 : row.get("height").getAsDouble();
        }
        @Override public String toString() { return name + " (#" + id + ")"; }
    }

    private void togglePatientMode() {
        boolean existing = existingPatientRadio.isSelected();
        existingPatientPane.setVisible(existing);
        existingPatientPane.setManaged(existing);
        newPatientPane.setVisible(!existing);
        newPatientPane.setManaged(!existing);
    }

    private void loadPatients() throws IOException {
        patients.clear();
        JsonArray rows = JsonParser.parseString(patientRepository.getAllPatients())
            .getAsJsonObject().getAsJsonArray("items");
        for (JsonElement e : rows) {
            PatientChoice p = new PatientChoice(e.getAsJsonObject());
            if (p.id == originalPatientId) originalPatient = p;
            else patients.add(p);
        }
        existingPatientCombo.getItems().setAll(patients);
        if (originalPatient != null) {
            originalPatientLabel.setText("Paciente original: " + originalPatient);
            newPatientAge.setText(originalPatient.age > 0 ? String.valueOf(originalPatient.age) : "");
            newPatientWeight.setText(originalPatient.weight > 0 ? String.valueOf(originalPatient.weight) : "");
            newPatientHeight.setText(originalPatient.height > 0 ? String.valueOf(originalPatient.height) : "");
        }
    }

    @FXML
    private void cloneDiet() {
        try {
            Integer existingId = null;
            String name = null;
            Integer age = null;
            Double weight = null, height = null;
            String target;
            if (existingPatientRadio.isSelected()) {
                PatientChoice selected = existingPatientCombo.getValue();
                if (selected == null || patients.stream().noneMatch(p -> p.id == selected.id))
                    throw new IllegalArgumentException("Selecciona un paciente registrado de la lista.");
                existingId = selected.id;
                target = selected.name;
            } else {
                name = newPatientName.getText() == null ? "" : newPatientName.getText().trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Escribe el nombre del paciente nuevo.");
                age = Integer.valueOf(newPatientAge.getText().trim());
                weight = Double.valueOf(newPatientWeight.getText().trim().replace(',', '.'));
                height = Double.valueOf(newPatientHeight.getText().trim().replace(',', '.'));
                if (age <= 0 || age > 120) throw new IllegalArgumentException("La edad debe estar entre 1 y 120 años.");
                if (!Double.isFinite(weight) || weight <= 0 || weight > 999.99)
                    throw new IllegalArgumentException("El peso debe estar entre 0 y 999.99 kg.");
                if (!Double.isFinite(height) || height <= 0 || height >= 10)
                    throw new IllegalArgumentException("La altura debe estar en metros, por ejemplo 1.75.");
                target = name;
            }
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Clonar dieta");
            confirmation.setHeaderText("Crear una copia para " + target);
            confirmation.setContentText("Se copiarán las comidas, horarios e ingredientes. " +
                "La dieta original no cambiará. Verifica que este plan sea adecuado para el paciente.");
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) return;
            int newId = repository.cloneDietForPatient(dietId, Session.requireUserId(),
                existingId, name, age, weight, height);
            saved = true;
            Alert done = new Alert(Alert.AlertType.INFORMATION);
            done.setTitle("Dieta clonada");
            done.setHeaderText("Dieta #" + newId + " creada");
            done.setContentText("Asignada a " + target + ".");
            done.showAndWait();
            ((Stage) cloneButton.getScene().getWindow()).close();
        } catch (NumberFormatException ex) {
            showError("Revisa edad, peso y altura. Usa altura en metros (ej. 1.75) y peso en kg (ej. 70.5).");
        } catch (IllegalArgumentException | IOException ex) {
            showError(ex.getMessage());
        }
    }

    public boolean isSaved() { return saved; }

    @FXML
    private void save() {
        try {
            String caloriesText = calTextField.getText().trim();
            double calories = Double.parseDouble(caloriesText);
            if (!Double.isFinite(calories) || calories <= 0 || calories > 6000)
                throw new IllegalArgumentException("Las calorías deben estar entre 0 y 6000.");
            String quantity = quantityFoodDistribution.getValue();
            String restLabel = selectFreeDay.getValue();
            if (quantity == null || restLabel == null)
                throw new IllegalArgumentException("Selecciona la cantidad de comidas y el día de descanso.");
            String note = commentTextField.getText().trim();
            if (note.isEmpty()) throw new IllegalArgumentException("Escribe un comentario.");
            int index = java.util.Arrays.asList(LABELS).indexOf(restLabel);
            if (index < 0) throw new IllegalArgumentException("Día de descanso inválido.");

            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("calories", calories);
            fields.put("note", note);
            fields.put("rest_day", DAYS[index]);
            // No permitir alterar la cantidad de comidas sin regenerar el plan.
            // El repositorio también valida este invariante.
            fields.put("meals_per_day", Integer.parseInt(quantity));
            repository.updateDietForUser(dietId, Session.requireUserId(), fields);
            initialRestDay = DAYS[index];
            saved = true;
            ((Stage) saveButton.getScene().getWindow()).close();
        } catch (NumberFormatException ex) {
            showError("Las calorías deben ser un número válido.");
        } catch (IllegalArgumentException | IOException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void cancel() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Editar dieta");
        alert.setHeaderText("No se pudieron guardar los cambios");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
