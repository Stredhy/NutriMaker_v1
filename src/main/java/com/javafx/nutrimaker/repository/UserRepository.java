package com.javafx.nutrimaker.repository;

import com.javafx.nutrimaker.database.DatabaseClient;
import com.google.gson.JsonArray;
import org.mindrot.jbcrypt.BCrypt;
import java.io.IOException;
import static com.javafx.nutrimaker.database.DatabaseClient.*;

public class UserRepository {
    private final DatabaseClient db = new DatabaseClient();

    public String getAllUsers() throws IOException {
        return db.read(c -> items(query(c,
                "SELECT ID_USUARIO AS user_id, CORREO AS email FROM USUARIO ORDER BY ID_USUARIO")));
    }

    public boolean insertUser(String email, String plainPassword) {
        try {
            String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            db.transaction(c -> {
                int id = nextId(c, "USUARIO", "ID_USUARIO");
                execute(c, "INSERT INTO USUARIO (ID_USUARIO,CORREO,PASSWORD) VALUES (?,?,?)", id, email, hash);
                return id;
            });
            return true;
        } catch (IOException e) { e.printStackTrace(); return false; }
    }

    public Integer getIdByEmail(String email) {
        try {
            JsonArray rows = db.read(c -> query(c,
                    "SELECT ID_USUARIO AS user_id FROM USUARIO WHERE CORREO=?", email));
            return rows.isEmpty() ? null : rows.get(0).getAsJsonObject().get("user_id").getAsInt();
        } catch (IOException e) { e.printStackTrace(); return null; }
    }

    public boolean verifyPasswordByEmail(String email, String plainPassword) {
        try {
            JsonArray rows = db.read(c -> query(c,
                    "SELECT PASSWORD AS password_hash FROM USUARIO WHERE CORREO=?", email));
            if (rows.isEmpty() || rows.get(0).getAsJsonObject().get("password_hash").isJsonNull()) return false;
            return BCrypt.checkpw(plainPassword, rows.get(0).getAsJsonObject().get("password_hash").getAsString());
        } catch (IOException | IllegalArgumentException e) { e.printStackTrace(); return false; }
    }
}
