package modelo.datos;

import java.util.Objects;

/**
 * POJO que representa una etiqueta (tag) en la base de datos.
 */
public class Tag {

    private long id;
    private String nombre;
    private Long parentId; // Usamos Long para permitir valores nulos.

    // Constructor por defecto
    public Tag() {
    } // ---FIN de constructor [Tag]---
    
    // Constructor para conveniencia
    public Tag(long id, String nombre) {
        this(id, nombre, null); // Llama al constructor más completo
    } // ---FIN de constructor [Tag]---
    
    // Constructor completo
    public Tag(long id, String nombre, Long parentId) {
        this.id = id;
        this.nombre = nombre;
        this.parentId = parentId;
    } // ---FIN de constructor [Tag]---

    // --- Getters y Setters ---

    public long getId() {
        return id;
    } // ---FIN de metodo [getId]---

    public void setId(long id) {
        this.id = id;
    } // ---FIN de metodo [setId]---

    public String getNombre() {
        return nombre;
    } // ---FIN de metodo [getNombre]---

    public void setNombre(String nombre) {
        this.nombre = nombre;
    } // ---FIN de metodo [setNombre]---
    
    public Long getParentId() {
        return parentId;
    } // ---FIN de metodo [getParentId]---

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    } // ---FIN de metodo [setParentId]---

    // --- Métodos de utilidad ---

    @Override
    public String toString() {
        return nombre; // Para que se muestre bien en JLists, etc.
    } // ---FIN de metodo [toString]---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        // Dos tags son iguales si tienen el mismo ID, o si ambos no tienen ID
        // y tienen el mismo nombre (para comparación antes de guardar en BD).
        if (id > 0 && tag.id > 0) {
            return id == tag.id;
        }
        return Objects.equals(nombre, tag.nombre);
    } // ---FIN de metodo [equals]---

    @Override
    public int hashCode() {
        // Usar el nombre para el hashcode es más consistente antes de que se asigne un ID.
        return Objects.hash(nombre);
    } // ---FIN de metodo [hashCode]---

} // --- FIN de clase Tag ---