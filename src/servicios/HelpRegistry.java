package servicios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.ayuda.HelpTopic;

/**
 * Un servicio singleton que actúa como la fuente única de la verdad para
 * toda la información de ayuda extraída de la UI.
 * Es poblado por los builders (ToolbarBuilder, MenuBarBuilder) durante la inicialización.
 */
public class HelpRegistry {

    private static final Logger logger = LoggerFactory.getLogger(HelpRegistry.class);
    private static final HelpRegistry INSTANCE = new HelpRegistry();
    private final Map<String, HelpTopic> topics = new LinkedHashMap<>();
    private final Map<String, String> customHelpTexts = new HashMap<>();

    // Constructor privado para el patrón Singleton
    private HelpRegistry() {
        addCustomHelpTexts();
    }

    public static HelpRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registra un nuevo tema de ayuda. Si ya existe un tema con el mismo comando,
     * se le da prioridad al que tiene un icono (probablemente de una Toolbar).
     * @param topic El HelpTopic a registrar.
     */
    public void registerTopic(HelpTopic topic) {
        if (topic == null || topic.command() == null) return;
        
        HelpTopic existing = topics.get(topic.command());
        if (existing == null || (topic.iconName() != null && !topic.iconName().isBlank())) {
            topics.put(topic.command(), topic);
        }
    }

    /**
     * Devuelve una lista de todos los temas de ayuda registrados.
     * @return Una lista de HelpTopics.
     */
    public List<HelpTopic> getAllTopics() {
        return new ArrayList<>(topics.values());
    }

    /**
     * Devuelve el texto de ayuda para un comando, aplicando la jerarquía de prioridades.
     * @param topic El HelpTopic cuya descripción se quiere obtener.
     * @return El texto de ayuda final.
     */
    public String getHelpTextForTopic(HelpTopic topic) {
        if (topic == null) return "";
        // Prioridad 1: Texto personalizado
        if (customHelpTexts.containsKey(topic.command())) {
            return customHelpTexts.get(topic.command());
        }
        // Prioridad 2: Descripción por defecto del topic (extraída del tooltip o menú)
        return topic.description();
    }

    /**
     * Aquí es donde se pueden "hardcodear" textos de ayuda personalizados para
     * anular los que se extraen automáticamente de la UI.
     */
    private void addCustomHelpTexts() {
        logger.debug("Cargando textos de ayuda personalizados...");
        // Ejemplo:
        // customHelpTexts.put(
        //     AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR, 
        //     "Este es un texto de ayuda mucho más detallado y personalizado para 'Ajustar a Pantalla'."
        // );
        // customHelpTexts.put(
        //     AppActionCommands.CMD_NAV_SIGUIENTE,
        //     "Navega a la siguiente imagen en la lista. Si la navegación circular está activa, volverá al principio."
        // );
    }

}