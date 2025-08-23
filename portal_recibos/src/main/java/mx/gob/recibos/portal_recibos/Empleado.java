package mx.gob.recibos.portal_recibos;

public class Empleado {
    private String claveSp;
    private String nombre;
    private String apellidoPaterno;
    private String rfc;
    private String curp;
    private String issemym;

    // Getters y Setters

    public String getClaveSp() {
        return claveSp;
    }

    public void setClaveSp(String claveSp) {
        this.claveSp = claveSp;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }

    public String getRfc() {
        return rfc;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
    }

    public String getCurp() {
        return curp;
    }

    public void setCurp(String curp) {
        this.curp = curp;
    }

    public String getIssemym() {
        return issemym;
    }

    public void setIssemym(String issemym) {
        this.issemym = issemym;
    }
    public Empleado(String claveSp, String nombre, String rfc, String curp, String issemym) {
        this.claveSp = claveSp;
        this.nombre = nombre;
        this.rfc = rfc;
        this.curp = curp;
        this.issemym = issemym;
    }
    public Empleado() {
        // Constructor vacío requerido para setear los atributos manualmente
    }

}
