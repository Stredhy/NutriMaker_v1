package com.javafx.nutrimaker.database;

import com.google.gson.*;
import java.io.IOException;
import java.sql.*;
import java.time.*;

public class DatabaseClient {
    @FunctionalInterface
    public interface Work<T> { T run(Connection c) throws SQLException, IOException; }
    public Connection connect() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/nutrimaker?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String password = "";

        return DriverManager.getConnection(url, user, password);
    }
    public <T> T read(Work<T> work) throws IOException {
        try (Connection c = connect()) { return work.run(c); }
        catch (SQLException e) { throw new IOException("Error de MySQL: " + e.getMessage(), e); }
    }
    public <T> T transaction(Work<T> work) throws IOException {
        return read(c -> {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | IOException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        });
    }
    private static void bind(PreparedStatement s, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
    }
    public static JsonArray query(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, args);
            try (ResultSet rs = s.executeQuery()) {
                JsonArray rows = new JsonArray();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    JsonObject row = new JsonObject();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String key = meta.getColumnLabel(i);
                        Object v = rs.getObject(i);
                        if (v == null) row.add(key, JsonNull.INSTANCE);
                        else if (v instanceof Number n) row.addProperty(key, n);
                        else if (v instanceof java.sql.Date d) row.addProperty(key, d.toLocalDate().toString());
                        else if (v instanceof Timestamp t) row.addProperty(key, t.toLocalDateTime().toInstant(ZoneOffset.UTC).toString());
                        else if (v instanceof LocalDateTime t) row.addProperty(key, t.toInstant(ZoneOffset.UTC).toString());
                        else row.addProperty(key, v.toString());
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }
    public static int execute(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, args);
            return s.executeUpdate();
        }
    }
    public static int insert(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(s, args);
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se recibiÃ³ el ID del registro creado.");
                return keys.getInt(1);
            }
        }
    }
    public static String items(JsonArray rows) {
        JsonObject result = new JsonObject();
        result.add("items", rows);
        return result.toString();
    }
    public static String one(JsonArray rows) throws IOException {
        if (rows.isEmpty()) throw new IOException("No se encontrÃ³ el registro solicitado.");
        return rows.get(0).toString();
    }
}
