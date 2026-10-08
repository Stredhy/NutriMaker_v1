package com.javafx.nutrimaker.repository;

import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.models.*;
import com.google.gson.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

import static com.javafx.nutrimaker.database.DatabaseClient.*;

public class MealRepository {

    private final DatabaseClient db = new DatabaseClient();

    public Meal getMealById(int id) throws IOException {
        return db.read(c ->
                MealAdapter.adaptAndDeserialize(
                        one(query(
                                c,
                                "SELECT * FROM mealbase WHERE meal_base_id=?",
                                id
                        ))
                )
        );
    }

    private Meal selectMeal(
            Connection c,
            int calories,
            String type,
            String mealTime,
            Set<Integer> usedMeals
    ) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM mealbase " +
                "WHERE calories >= 1 " +
                "AND calories <= ? " +
                "AND meal_type = ? " +
                "AND meal_time = ? "
        );

        List<Object> params = new ArrayList<>();

        params.add(calories);
        params.add(type);
        params.add(mealTime);

        if (!usedMeals.isEmpty()) {

            sql.append("AND meal_base_id NOT IN (");

            for (int i = 0; i < usedMeals.size(); i++) {

                if (i > 0) {
                    sql.append(",");

                }

                sql.append("?");
            }

            sql.append(") ");

            params.addAll(usedMeals);
        }

        sql.append("ORDER BY RAND() LIMIT 1");

        JsonArray rows =
                query(
                        c,
                        sql.toString(),
                        params.toArray()
                );

        return rows.isEmpty()
                ? null
                : MealAdapter.adaptAndDeserialize(
                        rows.get(0).toString()
                );
    }

    public Meal getMealsByTypeAndCalories(
        int calories,
        String type
    ) throws IOException {

        return db.read(c -> {

            JsonArray rows = query(
                    c,
                    "SELECT * FROM mealbase " +
                    "WHERE calories >= 1 " +
                    "AND calories <= ? " +
                    "AND meal_type = ? " +
                    "ORDER BY RAND() LIMIT 1",
                    calories,
                    type
            );

            return rows.isEmpty()
                    ? null
                    : MealAdapter.adaptAndDeserialize(
                            rows.get(0).toString()
                    );
        });
    }
    
    private Map<String, Object> dietData(
            int userId,
            int patientId,
            ValoresNutricionales values,
            String restDay,
            int mealsPerDay,
            String note
    ) {

        return new Gson().fromJson(
                new Gson().toJson(
                        new DietRequest(
                                userId,
                                patientId,
                                values,
                                restDay,
                                mealsPerDay,
                                note
                        )
                ),
                new com.google.gson.reflect.TypeToken<
                        Map<String, Object>
                        >() {
                }.getType()
        );
    }

    public boolean sendDiet(
            int userId,
            int patientId,
            ValoresNutricionales values,
            String restDay,
            int mealsPerDay,
            String note
    ) throws IOException {

        return db.read(c ->
                DietRepository.insertDiet(
                        c,
                        dietData(
                                userId,
                                patientId,
                                values,
                                restDay,
                                mealsPerDay,
                                note
                        )
                ) > 0
        );
    }

    public boolean createNewDiet(
            double calories,
            int mealsPerDay,
            String restDay,
            int userId,
            int patientId,
            String note
    ) {

        if (!Double.isFinite(calories)
                || calories <= 0
                || calories > 6000
                || mealsPerDay < 1
                || mealsPerDay > 6
                || patientId <= 0) {

            return false;
        }

        try {

            return db.transaction(c -> {

                List<ComidaProgramada> plan =
                        DistribuidorDeCalorias.distribuir(
                                calories,
                                mealsPerDay
                        );

                List<LocalTime> tiempos =
                        DistribuidorDeCalorias.obtenerHoras(
                                mealsPerDay
                        );

                ValoresNutricionales totals =
                        new ValoresNutricionales();

                // Guarda los alimentos utilizados durante
                // TODA la dieta para evitar repeticiones.
                Set<Integer> usedMeals =
                        new HashSet<>();

                for (int i = 0; i < plan.size(); i++) {

                    ComidaProgramada scheduled =
                            plan.get(i);

                    String mealTime =
                            tiempos.get(i).toString();
                    int remaining =
                            (int) scheduled.getCaloriasAsignadas();

                    while (remaining > 0) {

                        Meal meal =
                                selectMeal(
                                        c,
                                        remaining,
                                        scheduled.getTipo(),
                                        mealTime,
                                        usedMeals
                                );

                        if (meal == null) {
                            break;
                        }

                        scheduled.agregarOpcion(meal);

                        totals.agregar(meal);

                        // Registramos el ID para que no
                        // vuelva a utilizarse.
                        usedMeals.add(
                                meal.getMealBaseId()
                        );

                        remaining =
                                (int) (
                                        remaining
                                                - meal.getCalories()
                                );
                    }

                    if (scheduled.getOpciones().isEmpty()) {

                        throw new IOException(
                                "No hay alimentos para "
                                        + mealTime
                                        + " dentro del presupuesto de calorías."
                        );
                    }
                }

                int dietId =
                        DietRepository.insertDiet(
                                c,
                                dietData(
                                        userId,
                                        patientId,
                                        totals,
                                        restDay,
                                        mealsPerDay,
                                        note
                                )
                        );

                List<LocalTime> times =
                        DistribuidorDeCalorias.obtenerHoras(
                                mealsPerDay
                        );

                LocalDate start =
                        LocalDate.now();

                for (int i = 0; i < 7; i++) {

                    LocalDate day =
                            start.plusDays(i);

                    if (day.getDayOfWeek()
                            .name()
                            .equalsIgnoreCase(restDay)) {

                        continue;
                    }

                    for (int j = 0;
                         j < plan.size();
                         j++) {

                        ComidaProgramada scheduled =
                                plan.get(j);

                        for (Meal meal :
                                scheduled.getOpciones()) {

                            execute(
                                    c,
                                    "INSERT INTO diet_meal " +
                                    "(diet_id, meal_base_id, day, " +
                                    "time_of_day, meal_type) " +
                                    "VALUES (?, ?, ?, ?, ?)",

                                    dietId,
                                    meal.getMealBaseId(),
                                    day,
                                    day.atTime(times.get(j)),
                                    scheduled.getTipo()
                            );
                        }
                    }
                }

                return true;
            });

        } catch (IOException e) {

            e.printStackTrace();

            return false;
        }
    }
}