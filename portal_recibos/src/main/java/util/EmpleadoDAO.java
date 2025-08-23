package util;

import mx.gob.recibos.portal_recibos.Empleado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.sql.*;

public class EmpleadoDAO {

    private final Connection conn;

    public EmpleadoDAO(Connection conn) {
        this.conn = conn;
    }

    private static final String BASE_SELECT =
            "SELECT CLAVE_SP, NOMBRE, APELLIDO_PATERNO, RFC, CURP, ISSEMYM " +
                    "FROM KDRHEMP WHERE TRIM(CLAVE_SP) = TRIM(?)";

    // === READ: uno por clave ===
    public Empleado buscarPorClave(String claveSp) {
        Empleado emp = null;
        try (PreparedStatement stmt = conn.prepareStatement(BASE_SELECT)) {
            stmt.setString(1, claveSp);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    emp = mapearEmpleado(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error al buscar empleado: " + e.getMessage());
        }
        return emp;
    }

    // === READ: lista por clave (hoy es igualdad; útil si luego cambias a LIKE) ===
    public List<Empleado> buscarPorClaveLista(String claveSp) {
        List<Empleado> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(BASE_SELECT)) {
            stmt.setString(1, claveSp);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearEmpleado(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error al consultar lista de empleados: " + e.getMessage());
        }
        return lista;
    }

    // === UPDATE: guarda en APELLIDO_PATERNO el "nombre completo" como quedamos ===
    public void actualizarEmpleado(Empleado empleado) {
        String sql = "UPDATE KDRHEMP " +
                "SET APELLIDO_PATERNO = ?, RFC = ?, CURP = ?, ISSEMYM = ? " +
                "WHERE TRIM(CLAVE_SP) = TRIM(?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, empleado.getApellidoPaterno());
            stmt.setString(2, empleado.getRfc());
            stmt.setString(3, empleado.getCurp());
            stmt.setString(4, empleado.getIssemym());
            stmt.setString(5, empleado.getClaveSp());

            int filas = stmt.executeUpdate();
            System.out.println(filas > 0 ? "✅ Empleado actualizado correctamente"
                    : "⚠ No se encontró un empleado con esa CLAVE_SP");
        } catch (SQLException e) {
            System.out.println("❌ Error al actualizar empleado: " + e.getMessage());
        }
    }

    // === Helper ===
    private Empleado mapearEmpleado(ResultSet rs) throws SQLException {
        Empleado emp = new Empleado();
        emp.setClaveSp(rs.getString("CLAVE_SP"));
        emp.setNombre(rs.getString("NOMBRE")); // no lo mostramos, pero lo dejamos
        emp.setApellidoPaterno(rs.getString("APELLIDO_PATERNO")); // aquí viene el nombre completo
        emp.setRfc(rs.getString("RFC"));
        emp.setCurp(rs.getString("CURP"));
        emp.setIssemym(rs.getString("ISSEMYM"));
        return emp;
}
    public boolean validarCredenciales(String usuario, String password) throws SQLException {
        String sql = "SELECT COUNT(1) FROM RTSEGU WHERE ETYEMP = ? AND ETPASS = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public Empleado buscarPorUsuario(String usuario) throws SQLException {
        String sql = "SELECT * FROM empleados WHERE usuario = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Empleado emp = new Empleado();
                    emp.setClaveSp(rs.getString("clave_sp"));
                    // Añade aquí todos los campos necesarios
                    return emp;
                }
            }
        }
        return null;
    }

    // método para buscar en KDDESXXXX
    public List<Map<String, Object>> buscarDetallesPorClave(String claveSp, Integer anio) throws SQLException {
        List<Map<String, Object>> resultados = new ArrayList<>();

        // Obtener todas las tablas KDDES disponibles
        List<String> tablasKDDES = obtenerTablasKDDES();

        for (String tabla : tablasKDDES) {
            // Extraer el año del nombre de la tabla
            int añoTabla = Integer.parseInt(tabla.replace("KDDES", ""));

            // Si se especificó un año y no coincide, saltar
            if (anio != null && añoTabla != anio) continue;

            try {
                String sql = "SELECT CLAVE_SP, PERIODO, " + añoTabla + " AS AÑO, " +
                        "CLAVE_CATEGORIA AS PUESTO, " +
                        "CLAVE_PLAZA AS PLAZA, " +
                        "CLAVE_ADSCRIPCION AS ADSCRIPCION " +
                        "FROM " + tabla + " WHERE TRIM(CLAVE_SP) = TRIM(?)";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, claveSp);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            row.put("CLAVE_SP", rs.getString("CLAVE_SP"));
                            row.put("PERIODO", rs.getString("PERIODO"));
                            row.put("AÑO", rs.getInt("AÑO"));
                            row.put("PUESTO", rs.getString("PUESTO"));
                            row.put("PLAZA", rs.getString("PLAZA"));
                            row.put("ADSCRIPCION", rs.getString("ADSCRIPCION"));
                            resultados.add(row);
                        }
                    }
                }
            } catch (SQLException e) {
                // Tabla no existe o hay problemas, continuar con siguiente
                System.err.println("⚠ Error al consultar tabla " + tabla + ": " + e.getMessage());
            }
        }

        // Ordenar resultados por año descendente (más recientes primero)
        Collections.sort(resultados, (a, b) ->
                Integer.compare((Integer)b.get("AÑO"), (Integer)a.get("AÑO")));

        return resultados;
    }

    public List<String> obtenerTablasKDDES() throws SQLException {
        List<String> tablas = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();

        // Obtener todas las tablas que empiezan con "KDDES"
        try (ResultSet rs = metaData.getTables(null, null, "KDDES%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tablas.add(rs.getString("TABLE_NAME"));
            }
        }

        // Ordenar tablas por año descendente
        Collections.sort(tablas, Collections.reverseOrder());
        return tablas;
}

}