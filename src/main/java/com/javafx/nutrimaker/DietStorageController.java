package com.javafx.nutrimaker;

import static com.javafx.nutrimaker.animations.AnimationPersonalized.*;
import com.javafx.nutrimaker.models.Diet;
import com.javafx.nutrimaker.models.DietSummary;
import com.javafx.nutrimaker.models.User;
import com.javafx.nutrimaker.repository.DietRepository;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

public class DietStorageController implements Initializable {

    private final int LIMIT = 10;
    private int offset;    
    
    @FXML
    private TableView<DietSummary> dietsTable;
    @FXML
    private TableColumn<DietSummary, String> numCol;
    @FXML
    private TableColumn<DietSummary, String> patientCol;
    @FXML
    private TableColumn<DietSummary, String> weightCol;
    @FXML
    private TableColumn<DietSummary, String> heightCol;
    @FXML
    private TableColumn<DietSummary, String> dateCol;
    @FXML
    private TableColumn<DietSummary, String> actionsCol;
    @FXML
    private Button createButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button prevButton;
    
    private ObservableList<DietSummary> dietsList = FXCollections.observableArrayList();
    
    private String userEmail;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setFadeAndScaleAnimation(createButton);
        setFadeAndScaleAnimation(nextButton);
        setFadeAndScaleAnimation(prevButton);
        dietsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
        dietsTable.setSelectionModel(null);
        dietsTable.getColumns().forEach(column -> column.setReorderable(false));
        try {
            showDietList();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }    
    
    public void setUserEmail(String uE) {
        userEmail = uE;
    }
    
    @FXML
    private void createDiet(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("PatientForm.fxml"));
        Parent root = loader.load();
        PatientFormController patientController = loader.getController();
        patientController.setUserEmail(userEmail);
        
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Crear Dieta");
        stage.show();
    }
    
    @FXML
    private void next(ActionEvent event) throws IOException{
        offset+=10;
        showDietList();
    }
    
    @FXML
    private void prev(ActionEvent event) throws IOException{
        offset-=10;
        if (offset < 0){
            offset+=10;
        }
        showDietList();
    }

    private void showDietList() throws IOException {
        refreshTable();
        refreshButtons();
        numCol.setCellValueFactory(new PropertyValueFactory<>("numDiet"));
        patientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        weightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
        heightCol.setCellValueFactory(new PropertyValueFactory<>("height"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("creationDate"));
        //Hace seteo de botones por cada fila de dieta que se genera
        
        Callback<TableColumn<DietSummary, String>, TableCell<DietSummary, String>> cellFactory = (TableColumn<DietSummary, String> param) -> {
             final TableCell<DietSummary,String> cell = new TableCell<DietSummary,String>(){
                @Override   
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        ImageView pdfIcon = new ImageView(
                        DietStorageController.class.getResource("images/pdf.png").toExternalForm());
                        ImageView cloneIcon = new ImageView(
                        DietStorageController.class.getResource("images/clone.png").toExternalForm()); 
                        ImageView editIcon = new ImageView(
                        DietStorageController.class.getResource("images/edit.png").toExternalForm());
                        ImageView deleteIcon = new ImageView(
                        DietStorageController.class.getResource("images/delete.png").toExternalForm());
                        DropShadow shadow = new DropShadow(5, 2, 2, Color.rgb(0, 0, 0, 0.4));
                        Button pdf = new Button("",pdfIcon);
                        Button clone = new Button("",cloneIcon);
                        Button edit = new Button("",editIcon);
                        Button delete = new Button("",deleteIcon);
                        
                        for(ImageView icon: new ImageView[]{pdfIcon,cloneIcon,editIcon,deleteIcon}){
                            icon.setEffect(shadow);
                            icon.setFitHeight(30);
                            icon.setFitWidth(30);
                        }

                        for(Button btn: new Button[]{pdf,clone,edit,delete}){
                            btn.setPrefSize(30, 30);
                            btn.setStyle("-fx-background-color: none; -fx-border-color: none;");
                            btn.setCursor(Cursor.HAND);
                            setFadeAndScaleAnimation(btn);
                        }
                        
                        pdf.setOnAction(event -> {
                            DietSummary dietSummary = dietsTable.getItems().get(getIndex());
                            DietRepository dietRepo = new DietRepository();
                            Diet diet = null;
                            try {
                                diet = dietRepo.getDietObjectById(dietSummary.getDietId());
                            } catch (IOException | ParseException ex) {
                                Logger.getLogger(DietStorageController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            try {
                                PDFBuilder.exportPdf(diet);
                            } catch (FileNotFoundException ex) {
                                Logger.getLogger(DietStorageController.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException | URISyntaxException ex) {
                                Logger.getLogger(DietStorageController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });

                        clone.setOnAction(event -> {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("AskDialog.fxml"));
                            Parent root = null;
                            try {
                                root = loader.load();
                            } catch (IOException ex) {
                                Logger.getLogger(DietStorageController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                            AskDialogController ask = loader.getController();
                            ask.setLabelText("¿Desea Clonar");
                            Stage clonePopUp = new Stage();
                            clonePopUp.initModality(Modality.APPLICATION_MODAL);
                            clonePopUp.setScene(new Scene(root));
                            clonePopUp.initStyle(StageStyle.UNDECORATED);
                            ask.setStage(clonePopUp);
                            clonePopUp.showAndWait();
                            
                            if(!ask.isSelection()){
                                return;
                            }
                            
                            DietSummary dietSummary = dietsTable.getItems().get(getIndex());
                            DietRepository dietRepo = new DietRepository();
                            try {
                                
                                Diet diet = dietRepo.getDietObjectById(dietSummary.getDietId());
                                if(!dietRepo.cloneDietById(diet.getDietID())){
                                    return;
                                }
                            } catch (IOException | ParseException ex) {
                                Logger.getLogger(DietStorageController.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            try {
                                refreshTable();
                            } catch (IOException ex) {
                                Logger.getLogger(DietStorageController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });

                        edit.setOnAction(event -> {
                            DietSummary selected = getTableView().getItems().get(getIndex());
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("EditDiet.fxml"));
                                Parent root = loader.load();
                                EditDietController controller = loader.getController();
                                controller.setDietId(selected.getDietId());
                                Stage popup = new Stage();
                                popup.initOwner(dietsTable.getScene().getWindow());
                                popup.initModality(Modality.WINDOW_MODAL);
                                popup.setTitle("Editar Dieta");
                                popup.setScene(new Scene(root));
                                popup.showAndWait();
                                if (controller.isSaved()) {
                                    showDietList();
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(DietStorageController.class.getName()).log(Level.SEVERE, "Error al editar dieta", ex);
                                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                                alert.setContentText("No se pudo abrir la edición: " + ex.getMessage());
                                alert.showAndWait();
                            }
                        });
                        
                        delete.setOnAction(event -> {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("AskDialog.fxml"));
                            Parent root = null;
                            try {
                                root = loader.load();
                            } catch (IOException ex) {
                                Logger.getLogger(DietStorageController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                            AskDialogController ask = loader.getController();
                            ask.setLabelText("¿Desea Eliminar");
                            Stage deletePopUp = new Stage();
                            deletePopUp.initModality(Modality.APPLICATION_MODAL);
                            deletePopUp.setScene(new Scene(root));
                            deletePopUp.initStyle(StageStyle.UNDECORATED);
                            ask.setStage(deletePopUp);
                            
                            deletePopUp.showAndWait();
                            
                            if(!ask.isSelection()){
                                return;
                            }
                            
                            DietSummary dietSummary = dietsTable.getItems().get(getIndex());
                            DietRepository dietRepo = new DietRepository();
                            try {
                                Diet diet = dietRepo.getDietObjectById(dietSummary.getDietId());
                                dietRepo.deleteDiet(diet.getDietID());
                            } catch (IOException | ParseException ex) {
                                Logger.getLogger(DietStorageController.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            try {
                                refreshTable();
                            } catch (IOException ex) {
                                Logger.getLogger(DietStorageController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });
                        HBox actions = new HBox(10);
                        actions.getChildren().addAll(pdf,clone,edit,delete);
                        actions.setAlignment(Pos.CENTER);
                        actions.setPadding(new Insets(5));
                        setGraphic(actions);
                    }
                }
                
            };
            return cell;
            
        };
        actionsCol.setCellFactory(cellFactory);
        dietsTable.setItems(dietsList);
    }
    
    private void refreshButtons() throws IOException{
        DietRepository dietRepo = new DietRepository();        
        prevButton.setVisible(offset > 0);        
        nextButton.setVisible(offset < (dietRepo.getTotalDietsCount(Session.requireUserId()) - LIMIT));
    }
    
    private void refreshTable() throws IOException{
        dietsList.clear();
        DietRepository dietRepo= new DietRepository();
        List<DietSummary> diets = dietRepo.getDiets(offset, LIMIT, Session.requireUserId()); 
        int numDiet=offset;
        for(DietSummary diet : diets){
            dietsList.add(new DietSummary(++numDiet,diet.getDietId(),
                    diet.getPatientName(),
                    diet.getWeight(),
                    diet.getHeight(),
                    diet.getCreationDate()));
            dietsTable.setItems(dietsList);
        }        
    }
}