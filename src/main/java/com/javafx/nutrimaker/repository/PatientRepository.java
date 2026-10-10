package com.javafx.nutrimaker.repository;

import com.javafx.nutrimaker.database.DatabaseClient;
import com.google.gson.JsonArray;
import java.io.IOException;
import static com.javafx.nutrimaker.database.DatabaseClient.*;

public class PatientRepository {
    private final DatabaseClient db = new DatabaseClient();

    private static final String SELECT_PATIENT = """
        SELECT ID_PACIENTE AS patient_id, NOMBRE AS name, ALTURA AS height,
               PESO AS weight, EDAD AS age
        FROM PACIENTE
        """;

    public String getAllPatients() throws IOException {
        return db.read(c -> items(query(c, SELECT_PATIENT + " ORDER BY ID_PACIENTE")));
    }

    public String getPatientById(int id) throws IOException {
        return db.read(c -> one(query(c, SELECT_PATIENT + " WHERE ID_PACIENTE=?", id)));
    }

    public int createPatientAndGetId(String name, int age, Double weight, Double height) throws IOException {
        return db.transaction(c -> {
            int id = nextId(c, "PACIENTE", "ID_PACIENTE");
            execute(c, "INSERT INTO PACIENTE (ID_PACIENTE,NOMBRE,EDAD,PESO,ALTURA) VALUES (?,?,?,?,?)",
                    id, name, age, weight, height);
            return id;
        });
    }

    public boolean createPatient(String name, int age, Double weight, Double height) {
        try { createPatientAndGetId(name, age, weight, height); return true; }
        catch (IOException e) { e.printStackTrace(); return false; }
    }

    public String updatePatient(int id, String name, int age, Double weight, Double height) throws IOException {
        return db.read(c -> {
            int changed = execute(c,
                    "UPDATE PACIENTE SET NOMBRE=?,EDAD=?,PESO=?,ALTURA=? WHERE ID_PACIENTE=?",
                    name, age, weight, height, id);
            if (changed == 0) throw new IOException("No existe el paciente " + id);
            return one(query(c, SELECT_PATIENT + " WHERE ID_PACIENTE=?", id));
        });
    }

    public String deletePatient(int id) throws IOException {
        return db.transaction(c -> {
            // La eliminación de un paciente con dietas relacionadas puede estar bloqueada por claves foráneas.
            int deleted = execute(c, "DELETE FROM PACIENTE WHERE ID_PACIENTE=?", id);
            return "{\"deleted\":" + deleted + "}";
        });
    }
}
