package controlador.managers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import modelo.ListContext;
import modelo.ListContext.NavigationState;
import modelo.VisorModel;

/**
 * Gestiona la lógica de navegación jerárquica entre carpetas (drill-down).
 * Mantiene un historial de navegación para poder "subir" de nivel.
 */
public class FolderNavigationManager {

    private static final Logger logger = LoggerFactory.getLogger(FolderNavigationManager.class);

    private final VisorModel model;
    private final GeneralController generalController; // Para orquestar la recarga

    public FolderNavigationManager(VisorModel model, GeneralController generalController) {
        this.model = Objects.requireNonNull(model);
        this.generalController = Objects.requireNonNull(generalController);
    } // --- FIN del Constructor ---
    

    /**
     * Navega a la carpeta padre de la que se está visualizando actualmente.
     * Si el historial de navegación no está vacío, utiliza el historial para "salir".
     * Si el historial está vacío, simplemente sube al directorio padre.
     */
    public void navegarACarpetaPadre() {
        logger.debug("[FolderNavManager] Solicitud para navegar a la carpeta padre/anterior.");

        // PRIORIDAD 1: Usar el historial si existe.
        if (!model.getCurrentListContext().getHistorialNavegacion().isEmpty()) {
            logger.debug("  -> El historial no está vacío. Saliendo de subcarpeta...");
            salirDeSubcarpetaConHistorial();
            return; // El trabajo está hecho.
        }

        // PRIORIDAD 2: Si no hay historial, subir al directorio padre.
        logger.debug("  -> El historial está vacío. Intentando subir al directorio padre.");
        Path carpetaActual = model.getCarpetaRaizActual();
        if (carpetaActual == null) {
            logger.warn("  -> No hay carpeta actual cargada. No se puede navegar.");
            return;
        }

        Path carpetaPadre = carpetaActual.getParent();

        if (carpetaPadre != null && Files.isDirectory(carpetaPadre)) {
            logger.debug("  -> Carpeta padre encontrada: " + carpetaPadre);
            
            // La clave a seleccionar será el nombre de la carpeta de la que venimos.
            String claveASeleccionar = carpetaActual.getFileName().toString();
            
            generalController.solicitarCargaDesdeNuevaRaiz(carpetaPadre, claveASeleccionar);
        } else {
            logger.info("  -> Ya se está en la carpeta raíz del sistema o el padre no es accesible.");
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    } // --- Fin del método navegarACarpetaPadre ---
    

    /**
     * Navega "hacia dentro" a la carpeta contenedora de la imagen actualmente seleccionada.
     */
    public void entrarEnSubcarpeta() {
        logger.debug("[FolderNavManager] Solicitud para entrar en subcarpeta (ir a carpeta de la imagen).");

        if (model == null || generalController == null) {
            logger.error("  -> ERROR: Modelo o GeneralController nulos.");
            return;
        }

        String claveSeleccionada = model.getSelectedImageKey();
        Path carpetaRaizActual = model.getCarpetaRaizActual();

        if (claveSeleccionada == null || carpetaRaizActual == null) {
            logger.warn("  -> No hay imagen o carpeta raíz seleccionada. No se puede entrar.");
            return;
        }

        Path rutaImagenCompleta = model.getRutaCompleta(claveSeleccionada);
        if (rutaImagenCompleta == null) {
            logger.error("  -> No se pudo obtener la ruta completa para la clave: " + claveSeleccionada);
            return;
        }

        // --- LÓGICA MODIFICADA ---
        Path carpetaDestino = rutaImagenCompleta.getParent();
        
        // Comprobamos si realmente estamos "entrando" en una subcarpeta o si ya estamos allí.
        if (carpetaRaizActual.equals(carpetaDestino)) {
            logger.info("  -> La imagen seleccionada ya está en la carpeta raíz actual. No se navega.");
            java.awt.Toolkit.getDefaultToolkit().beep();
            return;
        }
        
        logger.info("  -> Carpeta destino encontrada: " + carpetaDestino);
        
        // Guardar el estado actual en el historial ANTES de navegar
        model.getCurrentListContext().getHistorialNavegacion().push(
            new ListContext.NavigationState(carpetaRaizActual, claveSeleccionada)
        );
        logger.debug("  -> Estado de navegación actual guardado en el historial. Profundidad: " + model.getCurrentListContext().getHistorialNavegacion().size());

        // Orquestar la carga de la nueva carpeta (sin preselección, cargará la primera imagen).
        generalController.solicitarCargaDesdeNuevaRaiz(carpetaDestino, null);
        
    } // --- Fin del método entrarEnSubcarpeta ---
    

    /**
     * Navega "hacia fuera" usando el historial de navegación, volviendo al estado anterior.
     * Este método es la implementación de "Atrás".
     */
    public void salirDeSubcarpetaConHistorial() {
        logger.debug("[FolderNavManager] Solicitud para salir de subcarpeta usando el historial.");

        java.util.Stack<ListContext.NavigationState> historial = model.getCurrentListContext().getHistorialNavegacion();

        if (historial.isEmpty()) {
            logger.info("  -> El historial de navegación está vacío. No se puede salir.");
            // En lugar de hacer beep, ahora delegamos a navegarACarpetaPadre, que tiene la lógica de fallback
            navegarACarpetaPadre();
            return;
        }

        // 1. Sacamos el último estado guardado de la pila.
        ListContext.NavigationState estadoAnterior = historial.pop();
        logger.debug("  -> Estado anterior recuperado. Carpeta: " + estadoAnterior.carpetaPadre + ", Clave: " + estadoAnterior.claveImagenSeleccionada);
        logger.debug("  -> Profundidad del historial ahora: " + historial.size());

        // 2. Orquestamos la carga de la carpeta anterior, seleccionando la imagen desde la que entramos.
        generalController.solicitarCargaDesdeNuevaRaiz(estadoAnterior.carpetaPadre, estadoAnterior.claveImagenSeleccionada);

    } // --- Fin del método salirDeSubcarpetaConHistorial ---
    

    /**
     * Vuelve a la carpeta raíz de la sesión de navegación actual, es decir,
     * al estado que había antes de entrar en cualquier subcarpeta.
     */
    public void volverACarpetaRaiz() {
        logger.debug("[FolderNavManager] Solicitud para volver a la carpeta raíz de la sesión.");

        java.util.Stack<NavigationState> historial = model.getCurrentListContext().getHistorialNavegacion();

        if (historial.isEmpty()) {
            logger.info("  -> Ya se está en la carpeta raíz de la sesión.");
            java.awt.Toolkit.getDefaultToolkit().beep();
            return;
        }

        // 1. Obtenemos el PRIMER estado que se guardó en el historial sin sacarlo.
        NavigationState estadoRaiz = historial.firstElement();
        logger.debug("  -> Estado raíz encontrado. Carpeta: " + estadoRaiz.carpetaPadre + ", Clave: " + estadoRaiz.claveImagenSeleccionada);
        
        // 2. Vaciamos completamente el historial.
        historial.clear();
        logger.debug("  -> Historial de navegación limpiado.");

        // 3. Orquestamos la carga de esa carpeta raíz, seleccionando la imagen original.
        generalController.solicitarCargaDesdeNuevaRaiz(estadoRaiz.carpetaPadre, estadoRaiz.claveImagenSeleccionada);

    } // --- Fin del método volverACarpetaRaiz ---
    
} // --- FIN de la clase FolderNavigationManager ---