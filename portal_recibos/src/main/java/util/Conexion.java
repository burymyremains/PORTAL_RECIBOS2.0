package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {

    // BASE DE DATOS SIPWEB
    private static final String URL1 = "jdbc:oracle:thin:@//localhost:1521/xepdb1";
    private static final String USUARIO1 = "HUGO_SIPWEB";
    private static final String CONTRASENA1 = "123";

    // BASE DE DATOS PRODUC
    private static final String URL2 = "jdbc:oracle:thin:@//localhost:1521/xepdb1";
    private static final String USUARIO2 = "HUGO_PRODUC";
    private static final String CONTRASENA2 = "123";

    /**
     * Conexión a la base de datos SIPWEB
     */
    public static Connection obtenerConexionSIPWEB() {
        try {
            return DriverManager.getConnection(URL1, USUARIO1, CONTRASENA1);
        } catch (SQLException e) {
            System.out.println("❌ Error al conectar a SIPWEB: " + e.getMessage());
            return null;
        }
    }

    /**
     * Conexión a la base de datos PRODUC
     */
    public static Connection obtenerConexionPRODUC() {
        try {
            return DriverManager.getConnection(URL2, USUARIO2, CONTRASENA2);
        } catch (SQLException e) {
            System.out.println("❌ Error al conectar a PRODUC: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        // Probar conexión a SIPWEB
        try (Connection conn1 = obtenerConexionSIPWEB()) {
            if (conn1 != null) {
                System.out.println("✅ Conexión exitosa a SIPWEB");
            } else {
                System.out.println("❌ No se pudo conectar a SIPWEB");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Probar conexión a PRODUC
        try (Connection conn2 = obtenerConexionPRODUC()) {
            if (conn2 != null) {
                System.out.println("✅ Conexión exitosa a PRODUC");
            } else {
                System.out.println("❌ No se pudo conectar a PRODUC");
            }
        } catch (SQLException e) {
            e.printStackTrace();}
}
}