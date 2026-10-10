/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.javafx.nutrimaker;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.javafx.nutrimaker.models.Diet;
import com.javafx.nutrimaker.models.Ingredient;
import com.javafx.nutrimaker.models.Meal;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 *
 * @author mimoe
 */
public class PDFBuilder {
    private static final String PATH = System.getProperty("user.home") + "/Documents/diet.pdf";
    private static final URL LOGO_PATH = PDFBuilder.class.getResource("images/nutrimakerLogo.png");

    public PDFBuilder(PDFBuilder pdf){}
    
    public PDFBuilder(Diet diet){}
    
    public static void exportPdf(Diet diet) throws FileNotFoundException, IOException, MalformedURLException, URISyntaxException{
        generatePdf(diet);
        Desktop.getDesktop().open(new File(PATH));
    }
    
    private static void generatePdf(Diet diet) throws FileNotFoundException, MalformedURLException, IOException, URISyntaxException{         
        String fontPath = PDFBuilder.class.getResource("fonts/ComicShannsMonoNerdFont-Regular.otf").toURI().getPath();
        
        ImageData imageData = ImageDataFactory.create(LOGO_PATH);
        Image img = new Image(imageData);
        img.scaleToFit(200,200);

        PdfWriter pdfWriter = new PdfWriter(PATH);
        PdfDocument pdfDoc = new PdfDocument(pdfWriter);
        pdfDoc.addNewPage();

        // Obtener dimensiones de la pagina
        float x = pdfDoc.getFirstPage().getPageSize().getRight();
        float y = pdfDoc.getFirstPage().getPageSize().getTop();

        img.setFixedPosition(x - 160, y - 140);

        Document doc = new Document(pdfDoc,PageSize.A4);
        doc.add(img);
        PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED);
        doc.setFont(font);
        doc.setFontSize(10);  

        
        //Datos del paciente
        Paragraph namePara = new Paragraph("Nombre: " + diet.getPatient().getName()).setFontSize(12);
        Paragraph infoPatient = new Paragraph();

        infoPatient.addTabStops(new TabStop(100, TabAlignment.LEFT));

        infoPatient.addTabStops(new TabStop(185, TabAlignment.LEFT));
        infoPatient.addTabStops(new TabStop(305, TabAlignment.LEFT));

        infoPatient.add(new Tab()); 
        infoPatient.add("Edad: " + diet.getPatient().getAge());
        infoPatient.add(new Tab());
        infoPatient.add("Estatura: " + diet.getPatient().getHeight() + " cm");
        infoPatient.add(new Tab());        
        infoPatient.add("Peso: " + diet.getPatient().getWeight() + " kg");

        for(Paragraph paragraph : new Paragraph[]{namePara,infoPatient}){
            doc.add(paragraph);
        }
        
        doc.add(new LineSeparator(new SolidLine(1))); // linea separadora

        //Comentarios
        Paragraph comments = new Paragraph("Comentarios:");
        
        doc.add(comments);
        doc.add(new Paragraph(diet.getNote()));

        doc.add(new LineSeparator(new SolidLine(1))); // linea separadora

        //Valores nutrimentales
        
        Paragraph nutriValues = new Paragraph();
       
        nutriValues.add("Calorias: " + diet.getCalories() + " Kcal    ");
        nutriValues.add("Proteinas: " + diet.getProtein() + " g    ");
        nutriValues.add("Grasas: " + diet.getFats() + " g    ");
        nutriValues.add("Calcio: " + diet.getCalcium() + " g    ");
        nutriValues.add("Sodio: " + diet.getSodium() + " g    ");
        nutriValues.add("Hierro: " + diet.getIron() + " g");
        
        doc.add(nutriValues);
        
        doc.add(new LineSeparator(new SolidLine(1))); // linea separadora
                
        //settear dietas
        doc.add(setMeals(diet));
        
        doc.close();
   }
    
   private static Table setMeals(Diet diet) throws IOException, URISyntaxException{
        String fontBoldPath = PDFBuilder.class.getResource("fonts/ComicShannsMonoNerdFont-Bold.otf").toURI().getPath();
        PdfFont fontBold = PdfFontFactory.createFont(fontBoldPath, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", new Locale("es", "ES"));

        Map<String,Integer> mealsSort = new HashMap<>();
        mealsSort.put("BREAKFAST", 1);
        mealsSort.put("SNACK", 2);
        mealsSort.put("LUNCH", 3);
        mealsSort.put("DINNER", 4);
        // También admite los tipos si el modelo ya los entrega en español.
        mealsSort.put("DESAYUNO", 1);
        mealsSort.put("COLACION", 2);
        mealsSort.put("COLACIÓN", 2);
        mealsSort.put("COMIDA", 3);
        mealsSort.put("CENA", 4);

        List<String> days = Arrays.asList(
            "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"
        );
        Map<String, List<Meal>> mealsByDay = new LinkedHashMap<>();
        for (String day : days) {
            mealsByDay.put(day, new ArrayList<>());
        }

        Locale spanish = new Locale("es", "ES");
        for (Meal meal : diet.getMeals()) {
            if (meal.getDay() == null) {
                throw new IllegalStateException(
                    "La comida \"" + meal.getName()
                    + "\" (ID: " + meal.getMealBaseId()
                    + ") no tiene un día asignado."
                );
            }

            String day = sdf.format(meal.getDay()).toLowerCase(spanish);
            List<Meal> dayMeals = mealsByDay.get(day);
            if (dayMeals != null) {
                dayMeals.add(meal);
            }
        }

        Comparator<Meal> mealComparator = Comparator.comparing(meal ->
            mealsSort.getOrDefault(meal.getMealType().toUpperCase(spanish), 99)
        );
        for (String day : days) {
            mealsByDay.get(day).sort(mealComparator);
        }

        // Una sola columna: cada día ocupa una fila horizontal completa.
        // Así se evita la columna lateral con el nombre del día girado.
        Table table = new Table(UnitValue.createPercentArray(new float[]{1}));
        table.setWidth(UnitValue.createPercentValue(100));

        for (String day : days) {
            List<Meal> meals = mealsByDay.get(day);
            if (meals.isEmpty()) {
                continue;
            }

            Paragraph dayTitle = new Paragraph(day.toUpperCase(spanish))
                .setFont(fontBold)
                .setFontSize(12)
                .setMarginTop(4)
                .setMarginBottom(5);
            Cell dayCell = new Cell()
                .add(dayTitle)
                .setPaddingLeft(8)
                .setPaddingTop(5)
                .setPaddingBottom(3)
                .setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(1));
            table.addCell(dayCell);

            Cell mealsCell = new Cell()
                .setPaddingLeft(12)
                .setPaddingRight(8)
                .setPaddingTop(2)
                .setPaddingBottom(8)
                .setBorder(Border.NO_BORDER);

            for (Meal meal : meals) {
                Paragraph type = new Paragraph(
                    meal.getMealType() + "  \uE28D \uE2A5 \uE28D  " + meal.getName()
                ).setFont(fontBold)
                 .setMarginTop(3)
                 .setMarginBottom(2);
                mealsCell.add(type);
                mealsCell.add(setIngredients(meal));
            }
            table.addCell(mealsCell);
        }

        return table;
    }

    private static Paragraph setIngredients(Meal meal){
        Paragraph paragraph = new Paragraph();
        for(Ingredient ingredient : meal.getIngredients()){
            paragraph.add((ingredient != meal.getIngredients().get(meal.getIngredients().size() - 1))?
                    ingredient.getName() + " \uEA9C " + ingredient.getAmount() + "  \uE29E  " :
                    ingredient.getName() + " \uEA9C " + ingredient.getAmount());
        }
        return paragraph;
    }
}
