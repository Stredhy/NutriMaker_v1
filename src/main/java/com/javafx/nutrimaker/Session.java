package com.javafx.nutrimaker;

import com.javafx.nutrimaker.models.User;

/** Única puerta de acceso a la sesión activa de la aplicación. */
public final class Session {

    private static User current;

    private Session() {}

    /** Inicia la sesión del usuario. No guarda la contraseña. */
    public static void start(String email, int userId) {
        current = new User(email, null, userId);
    }

    public static boolean isActive() {
        return current != null;
    }

    /** Devuelve el id del usuario en sesión o lanza error si nadie inició sesión. */
    public static int requireUserId() {
        if (current == null) {
            throw new IllegalStateException("No hay sesión activa");
        }
        return current.getId();
    }

    /** Cierra la sesión. También se usa para limpiar el estado entre pruebas. */
    public static void clear() {
        current = null;
    }
}