package mx.gob.recibos.portal_recibos;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.ResponseBody;
import util.Conexion;
import util.EmpleadoDAO;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index"; // Cargará index.html desde templates/
    }

    @PostMapping("/login")
    public String login(
            @RequestParam("usuario") String usuario,
            @RequestParam("password") String password,

            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Validar que el usuario contenga solo números
        if (!usuario.matches("\\d+")) {
            redirectAttributes.addFlashAttribute("errorUsuario", "El usuario debe contener solo números");
            return "redirect:/";
        }

        try (Connection conn = Conexion.obtenerConexionPRODUC()) {
            EmpleadoDAO dao = new EmpleadoDAO(conn);

            if (dao.validarCredenciales(usuario, password)) {
                // Guardar solo el nombre de usuario en sesión
                session.setAttribute("usuario", usuario);
                return "redirect:/menu";
            } else {
                redirectAttributes.addFlashAttribute("error", "Credenciales inválidas");
                return "redirect:/";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error de sistema: " + e.getMessage());
            return "redirect:/";
}
    }

    @GetMapping("/menu")
    public String menuPrincipal(HttpSession session) {
        if (session.getAttribute("usuario") == null) return "redirect:/";
        return "menu";
    }
    @GetMapping("/actualizacion")
    public String actualizacionDatos(HttpSession session) {
        if (session.getAttribute("usuario") == null) return "rediect:/";
        return "actualizacion";
    }
    //@GetMapping("/editar")
    //public String editarDatos() {
    //return "editar";
    //}
    @GetMapping("/homologacion")
    public String vistaHomologacion(HttpSession session, Model model) {
        if (session.getAttribute("usuario") == null) return "redirect:/";
        return "homologacion"; // templates/homologacion.html

    }

    @PostMapping("/homologacion")
    public String procesarHomologacion(@RequestParam("claveMal") String claveMal,
                                       @RequestParam("claveOk")  String claveOk,
                                       @RequestParam("usuario")  String usuario,
                                       HttpSession session,
                                       Model model) {
        if (session.getAttribute("usuario") == null) return "redirect:/";

        // Validaciones básicas
        if (claveMal == null || claveMal.isBlank() ||
                claveOk  == null || claveOk.isBlank()  ||
                usuario  == null || usuario.isBlank()) {
            model.addAttribute("mensajeError", "Llena todos los campos.");
            model.addAttribute("claveMal", claveMal);
            model.addAttribute("claveOk",  claveOk);
            model.addAttribute("usuario",  usuario);
            return "homologacion";
        }
        if (claveMal.trim().equals(claveOk.trim())) {
            model.addAttribute("mensajeError", "La Clave Mal y la Clave Ok no pueden ser iguales.");
            model.addAttribute("claveMal", claveMal);
            model.addAttribute("claveOk",  claveOk);
            model.addAttribute("usuario",  usuario);
            return "homologacion";
        }

        // Llamada al procedimiento en Oracle
        try (Connection conn = Conexion.obtenerConexionSIPWEB();
             CallableStatement cs = conn.prepareCall("{ call PROC_HOMOLOGAR_MIN(?, ?, ?, ?) }")) {

            cs.setString(1, claveMal.trim());
            cs.setString(2, claveOk.trim());
            cs.setString(3, usuario.trim());
            cs.registerOutParameter(4, Types.NUMERIC);

            cs.execute();
            int afectados = cs.getInt(4);

            model.addAttribute("mensajeOk", "Homologación realizada. Registros actualizados: " + afectados);
        } catch (Exception e) {
            model.addAttribute("mensajeError", "Error al homologar: " + e.getMessage());
        }

        // Reponer valores en la vista
        model.addAttribute("claveMal", claveMal);
        model.addAttribute("claveOk",  claveOk);
        model.addAttribute("usuario",  usuario);
        return "homologacion";
    }

    @GetMapping("/buscarDetalles")
    @ResponseBody
    public List<Map<String, Object>> buscarDetalles(
            @RequestParam("claveSp") String claveSp,
            @RequestParam(value = "anio", required = false) Integer anio,
            HttpSession session) {

        if (session.getAttribute("usuario") == null) return null;

        try (Connection conn = Conexion.obtenerConexionSIPWEB()) {
            EmpleadoDAO dao = new EmpleadoDAO(conn);
            return dao.buscarDetallesPorClave(claveSp, anio);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("/busquedas")
    public String busquedas(
            HttpSession session,
            Model model,
            @RequestParam(value = "claveSp", required = false) String claveSp,
            @RequestParam(value = "anio", required = false) Integer anio) {

        if (session.getAttribute("usuario") == null) return "redirect:/";

        // Obtener años disponibles para el dropdown
        List<Integer> añosDisponibles = obtenerAñosDisponibles();
        model.addAttribute("añosDisponibles", añosDisponibles);

        // Si se proporcionó claveSp, realizar búsqueda
        if (claveSp != null && !claveSp.isEmpty()) {
            try (Connection conn = Conexion.obtenerConexionSIPWEB()) {
                EmpleadoDAO dao = new EmpleadoDAO(conn);
                List<Map<String, Object>> resultados = dao.buscarDetallesPorClave(claveSp, anio);
                model.addAttribute("resultados", resultados);
            } catch (Exception e) {
                model.addAttribute("error", "Error en búsqueda: " + e.getMessage());
            }
        }

        return "busquedas";
    }

    private List<Integer> obtenerAñosDisponibles() {
        List<Integer> años = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexionSIPWEB()) {
            EmpleadoDAO dao = new EmpleadoDAO(conn);
            List<String> tablas = dao.obtenerTablasKDDES();

            for (String tabla : tablas) {
                try {
                    años.add(Integer.parseInt(tabla.replace("KDDES", "")));
                } catch (NumberFormatException e) {
                    // Ignorar tablas con formato no numérico
                }
            }

            // Ordenar años descendente
            Collections.sort(años, Collections.reverseOrder());
        } catch (Exception e) {
            // Manejar error, pero continuar
            System.err.println("Error al obtener años disponibles: " + e.getMessage());
        }
        return años;
    }
    @GetMapping("/credencializacion")
    public String verCredencializacion(HttpSession session) {
        if (session.getAttribute("usuario") == null) return "redirect:/";
        return "credencializacion";
    }
    @GetMapping("/historial")
    public String historialCredenciales(HttpSession session) {
        if (session.getAttribute("usuario") == null) return "redirect:/";
        return "historial"; // Apunta al archivo historial.html
    }
    @GetMapping("/chequeos")
    public String chequeosProcesados(HttpSession session) {
        if (session.getAttribute("usuario") == null) return "redirect:/";
        return "chequeos";
    }
    @GetMapping("/logout")
    public String logout(HttpSession session, Model model) {
        session.invalidate();
        model.addAttribute("mensajeLogout", "Has cerrado sesión correctamente.");
        return "index"; // Redirige al login
    }

    @GetMapping("/buscar")
    public String buscarPorClave(@RequestParam("claveSp") String claveSp, Model model) {
        System.out.println("/buscar claveSp=" + claveSp);
        try (Connection conn = Conexion.obtenerConexionSIPWEB()) {
            EmpleadoDAO dao = new EmpleadoDAO(conn);
            Empleado emp = dao.buscarPorClave(claveSp);
            System.out.println("/buscar resultado emp=" + (emp == null ? "null" : emp.getClaveSp()));
            model.addAttribute("empleado", emp);
        } catch (Exception e) {
            System.out.println("Error en búsqueda: " + e.getMessage());
        }
        return "actualizacion"; // HTML Thymeleaf con los datos cargados
    }

    @GetMapping("/editar")
    public String editarEmpleado(@RequestParam("claveSp") String claveSp, Model model) {
        try (Connection conn = Conexion.obtenerConexionSIPWEB()) {
            EmpleadoDAO dao = new EmpleadoDAO(conn);
            Empleado emp = dao.buscarPorClave(claveSp);
            model.addAttribute("empleado", emp);
        } catch (Exception e) {
            System.out.println("❌ Error al buscar para editar: " + e.getMessage());
        }
        return "editar"; // Thymeleaf buscará editar.html
    }
    @PostMapping("/guardar")
    public String guardarCambios(
            @RequestParam("claveSp")String claveSp,
            @RequestParam("apellidoPaterno") String apellidoPaterno,
            @RequestParam("curp") String curp,
            @RequestParam("rfc") String rfc,
            @RequestParam("issemym") String issemym,
            Model model) {

        try (Connection conn = Conexion.obtenerConexionSIPWEB()) {
            EmpleadoDAO dao = new EmpleadoDAO(conn);

            Empleado emp = new Empleado();
            emp.setClaveSp(claveSp);
            emp.setApellidoPaterno(apellidoPaterno);
            emp.setCurp(curp);
            emp.setRfc(rfc);
            emp.setIssemym(issemym);

            dao.actualizarEmpleado(emp);
            model.addAttribute("mensaje", "Datos actualizados correctamente");
        } catch (Exception e) {
            System.out.println("❌ Error al actualizar: " + e.getMessage());
        }
        return "redirect:/buscar?claveSp=" + claveSp; // para ver el cambio al instante
}
}