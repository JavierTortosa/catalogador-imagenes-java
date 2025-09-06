package controlador.utils;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Un registro centralizado para almacenar y acceder a todos los componentes
 * de la UI de la aplicación.
 */
public class ComponentRegistry {

	private static final Logger logger = LoggerFactory.getLogger(ComponentRegistry.class);
	
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
	
    private final Map<String, Component> components = new HashMap<>();
    private final Map<String, Set<String>> tags = new HashMap<>();
    
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
            logger.warn("ComponentRegistry WARN: Se intentó registrar un componente con un nombre nulo o vacío. Se ignora.");
            return;
        }
        if (component == null) {
            logger.warn("ComponentRegistry WARN: Se intentó registrar un componente nulo para la clave '" + name + "'. Se ignora.");
            return;
        }

        if (components.containsKey(name)) {
            logger.warn("ComponentRegistry WARN: La clave '" + name + "' ya existe. El componente anterior será sobrescrito.");
        }

        components.put(name, component);
    }// --- FIN del metodo register
    
    
    // --- NUEVO MÉTODO DE REGISTRO CON ETIQUETAS ---
    public void register(String key, Component component, String... tagNames) {
        register(key, component);
        for (String tagName : tagNames) {
            tags.computeIfAbsent(tagName, k -> new HashSet<>()).add(key);
        }
    } // --- Fin del método register (con etiquetas) ---

    
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
        return (T) components.get(name);
    }// --- Fin del método get ---
    
    
 // --- NUEVO MÉTODO PARA OBTENER COMPONENTES POR ETIQUETA ---
    public List<Component> getComponentsByTag(String tagName) {
        Set<String> keys = tags.getOrDefault(tagName, Collections.emptySet());
        List<Component> result = new ArrayList<>();
        for (String key : keys) {
            Component component = components.get(key);
            if (component != null) {
                result.add(component);
            }
        }
        return result;
    } // --- Fin del método getComponentsByTag ---

    
    /**
     * Devuelve una colección de TODOS los componentes registrados.
     * @return Una colección de todos los Component registrados.
     */
    // MODIFICACIÓN: Devuelve una colección de Component.
    public Collection<Component> getAllComponents() {
        return components.values();
    }// --- FIN del metodo getAllComponents
    
    
    /**
     * Devuelve una colección de solo los JComponent registrados.
     * Muy útil para operaciones de UI de Swing como aplicar temas.
     * @return Una colección de todos los JComponent registrados.
     */
    public Collection<JComponent> getAllJComponents() {
        return components.values().stream()
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
        return Collections.unmodifiableSet(components.keySet());
    }// --- FIN del metodo getAllComponentKeys
    

    /**
     * Muestra en la consola una lista de todos los componentes registrados,
     * útil para depuración.
     */
    public void printRegistryContents() {
        logger.debug("--- Contenido del ComponentRegistry ---");
        if (components.isEmpty()) {
            logger.debug("El registro está vacío.");
        } else {
            components.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // Ordenar por clave para una salida consistente
                .forEach(entry -> {
                    logger.debug("Clave: " + entry.getKey() + " -> Componente: " + entry.getValue().getClass().getName());
                });
        }
        logger.debug("--- Fin del Contenido (" + components.size() + " componentes) ---");
    }// --- FIN del metodo printRegistryContents
    
    
    /**
     * Elimina del registro todos los componentes cuyas claves comiencen con los prefijos
     * "interfaz.boton." o "toolbar.". Esencial para limpiar antes de una reconstrucción de UI.
     */
    public void unregisterToolbarComponents() {
        logger.debug("  [ComponentRegistry] Eliminando componentes de toolbars del registro...");
        // Usamos removeIf para eliminar de forma segura mientras iteramos
        components.keySet().removeIf(key -> 
	        key.startsWith("toolbar.")  
	        || key.startsWith("interfaz.boton.")
	        || key.startsWith("interfaz.dpad.") 
	        || key.startsWith("textfield.") 
        );
    } // --- Fin del método unregisterToolbarComponents ---
    
    
    /**
     * Elimina un componente del registro por su clave.
     * Si la clave no existe, el método no hace nada.
     *
     * @param key La clave del componente a eliminar. No debe ser nula.
     * @return El componente que fue eliminado, o null si no se encontró ningún componente con esa clave.
     */
    public Component unregister(String key) {
        if (key == null || key.isBlank()) {
            logger.warn("ComponentRegistry WARN: Se intentó desregistrar un componente con una clave nula o vacía. Se ignora.");
            return null;
        }

        // También debemos eliminar la clave de cualquier etiqueta a la que pertenezca.
        tags.values().forEach(keySet -> keySet.remove(key));
        
        // El método remove() de un Map devuelve el valor asociado a la clave, o null si no existía.
        Component removedComponent = components.remove(key);

        if (removedComponent != null) {
            logger.debug("  [ComponentRegistry] Componente con clave '" + key + "' eliminado del registro.");
        } else {
            // Esto no es necesariamente un error, puede que se intente eliminar algo que ya no existe.
            // logger.debug("  [ComponentRegistry] Se intentó eliminar la clave '" + key + "', pero no se encontró en el registro.");
        }
        
        return removedComponent;
    } // --- FIN del método unregister ---
    
    
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
        for (Component component : components.values()) { // <-- CORREGIDO: Itera sobre 'Component'
            // Comprobamos si el componente es una instancia del tipo que buscamos
            if (type.isInstance(component)) {
                // Hacemos un cast seguro y lo añadimos a la lista
                componentsOfType.add(type.cast(component));
            }
        }
        
        return componentsOfType;
    } // --- FIN del metodo getAllComponentsOfType ---
    
}// --- FIN de la clase ComponentRegistry ---