package util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class PruebaConsulta {
    public static void main(String[] args) {
        Connection conn = Conexion.obtenerConexionSIPWEB();

        if (conn != null) {
            try {
                Statement stmt = conn.createStatement();
                String sql = "SELECT CLAVE_SP, NOMBRE, RFC FROM KDRHEMP"; // Ajusta el nombre si es otro
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()) {
                    String clave = rs.getString("CLAVE_SP");
                    String nombre = rs.getString("NOMBRE");
                    String rfc = rs.getString("RFC");

                    System.out.println("Clave: " + clave + ", Nombre: " + nombre + ", RFC: " + rfc);
                }

                rs.close();
                stmt.close();
                conn.close();

            } catch (Exception e) {
                System.out.println("❌ Error al ejecutar consulta: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("❌ No se pudo conectar a la BD.");
        }
    }
}

