package controlador.utils;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

/**
 * Un registro centralizado para almacenar y acceder a todos los componentes
 * de la UI de la aplicación.
 */
public class ComponentRegistry {

    /**
     * El mapa que almacena los componentes.
     * La clave es un nombre único y descriptivo (String), preferiblemente canónico.
     * El valor es la instancia del componente Swing (JComponent).
     *
     * Ejemplos de claves:
     * - "frame.main"
     * - "panel.info.superior"
     * - "button.toolbar.abrir"
     * - "list.nombresArchivo"
     */
    private final Map<String, Component> componentMap = new HashMap<>();
    /**
     * Registra un componente en el mapa.
     * Si la clave ya existe, el componente anterior será reemplazado. Se imprimirá
     * una advertencia en la consola para notificar de posibles colisiones de nombres.
     *
     * @param name La clave única y descriptiva para el componente. No debe ser nula.
     * @param component La instancia del componente a registrar. No debe ser nula.
     */
    public void register(String name, Component component) {
        if (name == null || name.isBlank()) {
            System.err.println("ComponentRegistry WARN: Se intentó registrar un componente con un nombre nulo o vacío. Se ignora.");
            return;
        }
        if (component == null) {
            System.err.println("ComponentRegistry WARN: Se intentó registrar un componente nulo para la clave '" + name + "'. Se ignora.");
            return;
        }

        if (componentMap.containsKey(name)) {
            System.err.println("ComponentRegistry WARN: La clave '" + name + "' ya existe. El componente anterior será sobrescrito.");
        }

        componentMap.put(name, component);
    }// --- FIN del metodo register

    /**
     * Obtiene un componente del registro por su nombre.
     * Este método utiliza genéricos para evitar la necesidad de hacer casting manual
     * en el código que lo llama.
     *
     * Ejemplo de uso:
     *   JButton openButton = registry.get("button.toolbar.abrir");
     *
     * @param name La clave del componente a obtener.
     * @param <T>  El tipo de JComponent que se espera. El casting se realiza internamente.
     * @return El componente como el tipo T especificado, o null si la clave no se encuentra.
     */
    @SuppressWarnings("unchecked") // El casting es seguro si las claves se usan consistentemente.
    public <T extends Component> T get(String name) {
        return (T) componentMap.get(name);
    }

    /**
     * Devuelve una colección de TODOS los componentes registrados.
     * @return Una colección de todos los Component registrados.
     */
    // MODIFICACIÓN: Devuelve una colección de Component.
    public Collection<Component> getAllComponents() {
        return componentMap.values();
    }// --- FIN del metodo getAllComponents
    
    /**
     * Devuelve una colección de solo los JComponent registrados.
     * Muy útil para operaciones de UI de Swing como aplicar temas.
     * @return Una colección de todos los JComponent registrados.
     */
    public Collection<JComponent> getAllJComponents() {
        return componentMap.values().stream()
                .filter(JComponent.class::isInstance)
                .map(JComponent.class::cast)
                .collect(java.util.stream.Collectors.toList());
    }// --- FIN del metodo getAllJComponents
    
    /**
     * Devuelve un conjunto de todas las claves (nombres) de los componentes registrados.
     * Útil para depuración o para construir interfaces de configuración que listan
     * todos los componentes disponibles.
     *
     * @return Un Set inmutable con todas las claves del registro.
     */
    public Set<String> getAllComponentKeys() {
        return Collections.unmodifiableSet(componentMap.keySet());
    }// --- FIN del metodo getAllComponentKeys

    /**
     * Muestra en la consola una lista de todos los componentes registrados,
     * útil para depuración.
     */
    public void printRegistryContents() {
        System.out.println("--- Contenido del ComponentRegistry ---");
        if (componentMap.isEmpty()) {
            System.out.println("El registro está vacío.");
        } else {
            componentMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // Ordenar por clave para una salida consistente
                .forEach(entry -> {
                    System.out.println("Clave: " + entry.getKey() + " -> Componente: " + entry.getValue().getClass().getName());
                });
        }
        System.out.println("--- Fin del Contenido (" + componentMap.size() + " componentes) ---");
    }// --- FIN del metodo printRegistryContents
    
    
    /**
     * Devuelve una lista de todos los componentes registrados que son de un tipo específico
     * o una subclase de ese tipo.
     *
     * @param <T> El tipo de componente a buscar.
     * @param type La clase del tipo de componente a buscar (ej. JButton.class).
     * @return Una lista (List<T>) de componentes que coinciden con el tipo. La lista estará vacía si no se encuentra ninguno.
     */
    public <T> List<T> getAllComponentsOfType(Class<T> type) {
        if (type == null) {
            return Collections.emptyList();
        }
        
        List<T> componentsOfType = new ArrayList<>();
        
        // Iteramos sobre todos los valores del mapa como 'Component'
        for (Component component : componentMap.values()) { // <-- CORREGIDO: Itera sobre 'Component'
            // Comprobamos si el componente es una instancia del tipo que buscamos
            if (type.isInstance(component)) {
                // Hacemos un cast seguro y lo añadimos a la lista
                componentsOfType.add(type.cast(component));
            }
        }
        
        return componentsOfType;
    } // --- FIN del metodo getAllComponentsOfType ---
    
    
}// --- FIN de la clase ComponentRegistry ---