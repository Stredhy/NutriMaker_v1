package com.javafx.nutrimaker.repository;

import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.models.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static com.javafx.nutrimaker.database.DatabaseClient.*;

/** Repositorio adaptado al esquema existente: DIETA, PACIENTE, COMIDA e INGREDIENTE_COMIDA. */
public class DietRepository {
    private final DatabaseClient db = new DatabaseClient();

    private static final Map<String, String> FIELD_MAP = Map.ofEntries(
            Map.entry("user_id", "ID_USUARIO"),
            Map.entry("patient_id", "ID_PACIENTE"),
            Map.entry("calories", "CALORIAS"),
            Map.entry("fat", "GRASAS"),
            Map.entry("cholesterol", "COLESTEROL"),
            Map.entry("sodium", "SODIO"),
            Map.entry("carbohydrates", "CARBOHIDRATOS"),
            Map.entry("protein", "PROTEINA"),
            Map.entry("calcium", "CALCIO"),
            Map.entry("iron", "HIERRO"),
            Map.entry("note", "COMENTARIO"),
            Map.entry("rest_day", "DIA_DESCANSO"),
            Map.entry("target_gender", "GENERO_DESTINATARIO"),
            Map.entry("meals_per_day", "CANTIDAD_COMIDAS"),
            Map.entry("creation_date", "FECHA_CREACION"),
            // También se aceptan nombres del esquema español si un llamador los envía.
            Map.entry("ID_USUARIO", "ID_USUARIO"), Map.entry("ID_PACIENTE", "ID_PACIENTE"),
            Map.entry("CALORIAS", "CALORIAS"), Map.entry("GRASAS", "GRASAS"),
            Map.entry("COLESTEROL", "COLESTEROL"), Map.entry("SODIO", "SODIO"),
            Map.entry("CARBOHIDRATOS", "CARBOHIDRATOS"), Map.entry("PROTEINA", "PROTEINA"),
            Map.entry("CALCIO", "CALCIO"), Map.entry("HIERRO", "HIERRO"),
            Map.entry("COMENTARIO", "COMENTARIO"), Map.entry("DIA_DESCANSO", "DIA_DESCANSO"),
            Map.entry("GENERO_DESTINATARIO", "GENERO_DESTINATARIO"),
            Map.entry("CANTIDAD_COMIDAS", "CANTIDAD_COMIDAS"),
            Map.entry("FECHA_CREACION", "FECHA_CREACION")
    );

    private static final String DIET_COLUMNS = """
        ID_DIETA AS diet_id, ID_USUARIO AS user_id, ID_PACIENTE AS patient_id,
        CALORIAS AS calories, GRASAS AS fat, COLESTEROL AS cholesterol,
        SODIO AS sodium, CARBOHIDRATOS AS carbohydrates, PROTEINA AS protein,
        CALCIO AS calcium, HIERRO AS iron, COMENTARIO AS note,
        FECHA_CREACION AS creation_date, DIA_DESCANSO AS rest_day,
        GENERO_DESTINATARIO AS target_gender, CANTIDAD_COMIDAS AS meals_per_day
        """;

    public List<DietSummary> getDiets(int offset, int limit, int userId) throws IOException {
        if (offset < 0 || limit < 1) throw new IllegalArgumentException("Paginación inválida");
        JsonArray rows = db.read(c -> query(c, """
            SELECT d.ID_DIETA AS diet_id, p.NOMBRE AS patient_name,
                   p.PESO AS weight, p.ALTURA AS height,
                   DATE(d.FECHA_CREACION) AS creation_date
            FROM DIETA d
            JOIN PACIENTE p ON p.ID_PACIENTE=d.ID_PACIENTE
            WHERE d.ID_USUARIO=?
            ORDER BY d.ID_DIETA DESC LIMIT ? OFFSET ?
            """, userId, limit, offset));
        List<DietSummary> result = new ArrayList<>();
        for (JsonElement row : rows) result.add(new Gson().fromJson(row, DietSummary.class));
        return result;
    }

    public int getTotalDietsCount(int userId) throws IOException {
        JsonArray rows = db.read(c -> query(c,
                "SELECT COUNT(*) AS total FROM DIETA WHERE ID_USUARIO=?", userId));
        return rows.isEmpty() ? 0 : rows.get(0).getAsJsonObject().get("total").getAsInt();
    }

    public String getDietById(int id) throws IOException {
        return db.read(c -> one(query(c, "SELECT " + DIET_COLUMNS + " FROM DIETA WHERE ID_DIETA=?", id)));
    }

    public String getDietsByUserId(int id) throws IOException {
        return db.read(c -> items(query(c,
                "SELECT " + DIET_COLUMNS + " FROM DIETA WHERE ID_USUARIO=? ORDER BY ID_DIETA DESC", id)));
    }

    public String getDietsByPatientId(int id) throws IOException {
        return db.read(c -> items(query(c,
                "SELECT " + DIET_COLUMNS + " FROM DIETA WHERE ID_PACIENTE=? ORDER BY ID_DIETA DESC", id)));
    }

    private static Map<String, Object> validated(Map<String, Object> data) {
        if (data == null || data.isEmpty()) throw new IllegalArgumentException("No se recibieron campos de dieta");
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String column = FIELD_MAP.get(entry.getKey());
            if (column == null) throw new IllegalArgumentException("Campo de dieta no permitido: " + entry.getKey());
            Object value = entry.getValue();
            if ("FECHA_CREACION".equals(column) && value instanceof String date) {
                try {
                    value = LocalDateTime.ofInstant(Instant.parse(date), ZoneOffset.UTC);
                } catch (RuntimeException ex) {
                    // Permite también fechas SQL ya serializadas como yyyy-MM-dd HH:mm:ss.
                    value = LocalDateTime.parse(date.replace(' ', 'T'));
                }
            }
            values.put(column, value);
        }
        return values;
    }

    /** Inserta DIETA usando un ID explícito porque el esquema proporcionado no declara AUTO_INCREMENT. */
    public static int insertDiet(Connection c, Map<String, Object> data) throws SQLException {
        Map<String, Object> values = validated(data);
        values.putIfAbsent("FECHA_CREACION", LocalDateTime.now());
        int id = nextId(c, "DIETA", "ID_DIETA");
        List<String> columns = new ArrayList<>();
        columns.add("ID_DIETA");
        columns.addAll(values.keySet());
        List<Object> args = new ArrayList<>();
        args.add(id);
        args.addAll(values.values());
        String sql = "INSERT INTO DIETA (" + String.join(",", columns) + ") VALUES (" +
                String.join(",", Collections.nCopies(columns.size(), "?")) + ")";
        execute(c, sql, args.toArray());
        return id;
    }

    public String createDiet(Map<String, Object> data) throws IOException {
        return db.transaction(c -> {
            int id = insertDiet(c, data);
            return one(query(c, "SELECT " + DIET_COLUMNS + " FROM DIETA WHERE ID_DIETA=?", id));
        });
    }

    public String updateDiet(int id, Map<String, Object> data) throws IOException {
        Map<String, Object> values = validated(data);
        List<Object> args = new ArrayList<>(values.values());
        args.add(id);
        String assignments = String.join(",", values.keySet().stream().map(k -> k + "=?").toList());
        return db.transaction(c -> {
            int changed = execute(c, "UPDATE DIETA SET " + assignments + " WHERE ID_DIETA=?", args.toArray());
            if (changed == 0) throw new IOException("No existe la dieta " + id);
            return one(query(c, "SELECT " + DIET_COLUMNS + " FROM DIETA WHERE ID_DIETA=?", id));
        });
    }

    public String deleteDiet(int id) throws IOException {
        return db.transaction(c -> {
            execute(c, "DELETE FROM INGREDIENTE_COMIDA WHERE ID_COMIDA IN (SELECT ID_COMIDA FROM COMIDA WHERE ID_DIETA=?)", id);
            execute(c, "DELETE FROM COMIDA WHERE ID_DIETA=?", id);
            int deleted = execute(c, "DELETE FROM DIETA WHERE ID_DIETA=?", id);
            return "{\"deleted\":" + deleted + "}";
        });
    }

    public boolean cloneDietById(int id) throws IOException {
        return db.transaction(c -> {
            JsonArray rows = query(c, "SELECT " + DIET_COLUMNS + " FROM DIETA WHERE ID_DIETA=?", id);
            if (rows.isEmpty()) return false;
            Map<String, Object> data = new Gson().fromJson(rows.get(0), new TypeToken<Map<String, Object>>() {}.getType());
            data.remove("diet_id");
            data.remove("creation_date");
            int newId = insertDiet(c, data);
            JsonArray meals = query(c, "SELECT ID_COMIDA FROM COMIDA WHERE ID_DIETA=? ORDER BY HORA,ID_COMIDA", id);
            for (JsonElement element : meals) {
                int sourceMealId = element.getAsJsonObject().get("ID_COMIDA").getAsInt();
                copyMealToDiet(c, sourceMealId, newId, null);
            }
            return true;
        });
    }

    /** Conserva el día de origen al clonar comidas; permite sobrescribir hora y día para un plan semanal. */
    public static int copyMealToDiet(Connection c, int sourceMealId, int targetDietId, LocalTime overrideTime)
            throws SQLException, IOException {
        return copyMealToDiet(c, sourceMealId, targetDietId, overrideTime, null);
    }

    /** dayOfWeek: 1=lunes ... 7=domingo. null conserva el día de la fila fuente. */
    public static int copyMealToDiet(Connection c, int sourceMealId, int targetDietId,
                                     LocalTime overrideTime, Integer dayOfWeek)
            throws SQLException, IOException {
        JsonArray sourceRows = query(c, """
            SELECT ID_COMIDA, HORA, DIA_SEMANA, TIPO_COMIDA, GRUPO_COMIDA, CALORIAS, GRASAS,
                   COLESTEROL, SODIO, CARBOHIDRATOS, PROTEINA, CALCIO, HIERRO, NOMBRE_COMIDA
            FROM COMIDA WHERE ID_COMIDA=?
            """, sourceMealId);
        if (sourceRows.isEmpty()) throw new IOException("No existe la comida de origen " + sourceMealId);
        JsonObject r = sourceRows.get(0).getAsJsonObject();
        int newMealId = nextId(c, "COMIDA", "ID_COMIDA");
        Object hour = overrideTime != null ? Time.valueOf(overrideTime) :
                (r.get("HORA").isJsonNull() ? null : Time.valueOf(r.get("HORA").getAsString()));
        Integer day = dayOfWeek != null ? dayOfWeek :
                (r.has("DIA_SEMANA") && !r.get("DIA_SEMANA").isJsonNull()
                        ? r.get("DIA_SEMANA").getAsInt() : null);
        execute(c, """
            INSERT INTO COMIDA
              (ID_COMIDA,ID_DIETA,HORA,DIA_SEMANA,TIPO_COMIDA,GRUPO_COMIDA,CALORIAS,GRASAS,
               COLESTEROL,SODIO,CARBOHIDRATOS,PROTEINA,CALCIO,HIERRO,NOMBRE_COMIDA)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, newMealId, targetDietId, hour, day,
                nullableString(r, "TIPO_COMIDA"), nullableString(r, "GRUPO_COMIDA"),
                nullableDouble(r, "CALORIAS"), nullableDouble(r, "GRASAS"),
                nullableDouble(r, "COLESTEROL"), nullableDouble(r, "SODIO"),
                nullableDouble(r, "CARBOHIDRATOS"), nullableDouble(r, "PROTEINA"),
                nullableDouble(r, "CALCIO"), nullableDouble(r, "HIERRO"), nullableString(r, "NOMBRE_COMIDA"));
        execute(c, """
            INSERT INTO INGREDIENTE_COMIDA (ID_INGREDIENTE,ID_COMIDA,CANTIDAD)
            SELECT ID_INGREDIENTE,?,CANTIDAD FROM INGREDIENTE_COMIDA WHERE ID_COMIDA=?
            """, newMealId, sourceMealId);
        return newMealId;
    }

    private static String nullableString(JsonObject row, String key) {
        return !row.has(key) || row.get(key).isJsonNull() ? null : row.get(key).getAsString();
    }
    private static Double nullableDouble(JsonObject row, String key) {
        return !row.has(key) || row.get(key).isJsonNull() ? null : row.get(key).getAsDouble();
    }

    public Diet getDietObjectById(int id) throws IOException, ParseException {
        String json = db.read(c -> {
            JsonArray rows = query(c, """
                SELECT d.ID_DIETA AS diet_id, d.ID_USUARIO AS user_id, d.ID_PACIENTE AS patient_id,
                       d.CALORIAS AS calories, d.GRASAS AS fat, d.COLESTEROL AS cholesterol,
                       d.SODIO AS sodium, d.CARBOHIDRATOS AS carbohydrates, d.PROTEINA AS protein,
                       d.CALCIO AS calcium, d.HIERRO AS iron, d.COMENTARIO AS note,
                       d.FECHA_CREACION AS creation_date, d.DIA_DESCANSO AS rest_day,
                       d.GENERO_DESTINATARIO AS target_gender, d.CANTIDAD_COMIDAS AS meals_per_day,
                       p.NOMBRE AS patient_name, p.EDAD AS age, p.PESO AS weight, p.ALTURA AS height,
                       c.ID_COMIDA AS diet_meal_id, c.ID_COMIDA AS meal_base_id,
                       c.HORA AS time_of_day, c.DIA_SEMANA AS day_of_week,
                       c.TIPO_COMIDA AS meal_type, c.GRUPO_COMIDA AS meal_group,
                       c.NOMBRE_COMIDA AS meal_name, c.CALORIAS AS meal_calories,
                       c.GRASAS AS meal_fat, c.COLESTEROL AS cholesterol_meal,
                       c.SODIO AS meal_sodium, c.CARBOHIDRATOS AS carbohydrates_meal,
                       c.PROTEINA AS meal_protein, c.CALCIO AS meal_calcium, c.HIERRO AS meal_iron,
                       i.ID_INGREDIENTE AS ingredient_id, i.NOMBRE AS ingredient_name,
                       ic.CANTIDAD AS ingredient_amount
                FROM DIETA d
                JOIN PACIENTE p ON p.ID_PACIENTE=d.ID_PACIENTE
                LEFT JOIN COMIDA c ON c.ID_DIETA=d.ID_DIETA
                LEFT JOIN INGREDIENTE_COMIDA ic ON ic.ID_COMIDA=c.ID_COMIDA
                LEFT JOIN INGREDIENTE i ON i.ID_INGREDIENTE=ic.ID_INGREDIENTE
                WHERE d.ID_DIETA=?
                ORDER BY c.DIA_SEMANA,c.HORA,c.ID_COMIDA,i.ID_INGREDIENTE
                """, id);
            if (rows.isEmpty()) throw new IOException("La dieta no existe: " + id);
            return items(rows);
        });
        return buildDietFromFlatJson(json);
    }

    public Diet buildDietFromFlatJson(String json) throws ParseException {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray rows = root.getAsJsonArray("items");
        Diet diet = new Diet();
        Map<Integer, Meal> mealsMap = new LinkedHashMap<>();
        LocalDate weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);

        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();
            if (diet.getDietID() == 0) {
                diet.setDietID(intValue(row, "diet_id", 0));
                String fullDate = stringValue(row, "creation_date", null);
                if (fullDate != null) {
                    String dateOnly = fullDate.length() >= 10 ? fullDate.substring(0, 10) : fullDate;
                    LocalDate creationDay = LocalDate.parse(dateOnly);
                    weekStart = creationDay.with(java.time.DayOfWeek.MONDAY);
                    diet.setCreationDate(new SimpleDateFormat("yyyy-MM-dd").parse(dateOnly));
                }
                diet.setCalories(doubleValue(row, "calories"));
                diet.setProtein(doubleValue(row, "protein"));
                diet.setCalcium(doubleValue(row, "calcium"));
                diet.setSodium(doubleValue(row, "sodium"));
                diet.setIron(doubleValue(row, "iron"));
                diet.setFats(doubleValue(row, "fat"));
                diet.setNote(stringValue(row, "note", null));
                Patient patient = new Patient(
                        stringValue(row, "patient_name", null), intValue(row, "age", 0),
                        nullableDouble(row, "weight"), nullableDouble(row, "height"),
                        intValue(row, "patient_id", 0));
                diet.setPatient(patient);
            }

            if (!row.has("diet_meal_id") || row.get("diet_meal_id").isJsonNull()) continue;
            int occurrenceId = row.get("diet_meal_id").getAsInt();
            Meal meal = mealsMap.get(occurrenceId);
            if (meal == null) {
                meal = new Meal();
                meal.setMealBaseId(intValue(row, "meal_base_id", occurrenceId));
                meal.setName(stringValue(row, "meal_name", "Comida"));
                meal.setIngredients(new ArrayList<>());
                int dayOfWeek = intValue(row, "day_of_week", 1);
                if (dayOfWeek < 1 || dayOfWeek > 7) dayOfWeek = 1;
                LocalDate mealDate = weekStart.plusDays(dayOfWeek - 1L);
                meal.setDay(Date.from(mealDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                String timeString = stringValue(row, "time_of_day", null);
                if (timeString != null && !timeString.isBlank()) {
                    LocalTime localTime = LocalTime.parse(timeString.length() >= 8 ? timeString.substring(0, 8) : timeString);
                    meal.setTimeOfDay(Date.from(LocalDate.now().atTime(localTime).atZone(ZoneId.systemDefault()).toInstant()));
                }
                meal.setMealType(stringValue(row, "meal_type", null));
                meal.setMealGroup(stringValue(row, "meal_group", null));
                meal.setCalories(doubleValue(row, "meal_calories"));
                meal.setFat(doubleValue(row, "meal_fat"));
                meal.setCholesterol(doubleValue(row, "cholesterol_meal"));
                meal.setSodium(doubleValue(row, "meal_sodium"));
                meal.setCarbohydrates(doubleValue(row, "carbohydrates_meal"));
                meal.setProtein(doubleValue(row, "meal_protein"));
                meal.setCalcium(doubleValue(row, "meal_calcium"));
                meal.setIron(doubleValue(row, "meal_iron"));
                mealsMap.put(occurrenceId, meal);
            }

            if (row.has("ingredient_name") && !row.get("ingredient_name").isJsonNull()) {
                int ingredientId = intValue(row, "ingredient_id", -1);
                boolean exists = meal.getIngredients().stream().anyMatch(i ->
                        i.getName() != null && i.getName().equalsIgnoreCase(row.get("ingredient_name").getAsString()));
                if (!exists) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setName(row.get("ingredient_name").getAsString());
                    ingredient.setAmount(doubleValue(row, "ingredient_amount"));
                    meal.getIngredients().add(ingredient);
                }
            }
        }
        diet.setMeals(new ArrayList<>(mealsMap.values()));
        return diet;
    }

    private static String stringValue(JsonObject row, String key, String fallback) {
        return !row.has(key) || row.get(key).isJsonNull() ? fallback : row.get(key).getAsString();
    }
    private static int intValue(JsonObject row, String key, int fallback) {
        return !row.has(key) || row.get(key).isJsonNull() ? fallback : row.get(key).getAsInt();
    }
    private static double doubleValue(JsonObject row, String key) {
        return !row.has(key) || row.get(key).isJsonNull() ? 0.0 : row.get(key).getAsDouble();
    }
}
