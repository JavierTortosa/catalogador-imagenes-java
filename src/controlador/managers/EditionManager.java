package controlador.managers;

import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import controlador.VisorController; // <-- NUEVO IMPORT
import controlador.commands.AppActionCommands;
import modelo.VisorModel;
import servicios.image.ImageEdition;
// No se necesita importar VisorView

public class EditionManager {

    private final VisorModel model;
    private final VisorController controller; // <-- CAMBIO: Referencia al controlador
    private final ZoomManager zoomManager;

    public EditionManager(VisorModel model, VisorController controller, ZoomManager zoomManager) {
        this.model = Objects.requireNonNull(model);
        this.controller = Objects.requireNonNull(controller); // <-- CAMBIO
        this.zoomManager = Objects.requireNonNull(zoomManager);
    }

    public void aplicarVolteoHorizontal() {
        if (!validarPrecondiciones("aplicarVolteoHorizontal")) return;
        
        BufferedImage imagenOriginal = model.getCurrentImage();
        BufferedImage imagenVolteada = ImageEdition.flipHorizontal(imagenOriginal);
        
        procesarResultadoEdicion(
            imagenVolteada,
            "Volteo horizontal",
            AppActionCommands.CMD_IMAGEN_VOLTEAR_H,
            false // Voltear no cambia dimensiones, no es necesario resetear zoom
        );
    }

    public void aplicarVolteoVertical() {
        if (!validarPrecondiciones("aplicarVolteoVertical")) return;

        BufferedImage imagenOriginal = model.getCurrentImage();
        BufferedImage imagenVolteada = ImageEdition.flipVertical(imagenOriginal);

        procesarResultadoEdicion(
            imagenVolteada,
            "Volteo vertical",
            AppActionCommands.CMD_IMAGEN_VOLTEAR_V,
            false // Voltear no cambia dimensiones
        );
    }

    public void aplicarRotarIzquierda() {
        if (!validarPrecondiciones("aplicarRotarIzquierda")) return;
        
        BufferedImage imagenOriginal = model.getCurrentImage();
        BufferedImage imagenRotada = ImageEdition.rotateLeft(imagenOriginal);
        
        procesarResultadoEdicion(
            imagenRotada,
            "Rotación izquierda",
            AppActionCommands.CMD_IMAGEN_ROTAR_IZQ,
            true // Rotar SÍ cambia dimensiones, necesita resetear zoom
        );
    }

    public void aplicarRotarDerecha() {
        if (!validarPrecondiciones("aplicarRotarDerecha")) return;

        BufferedImage imagenOriginal = model.getCurrentImage();
        BufferedImage imagenRotada = ImageEdition.rotateRight(imagenOriginal);

        procesarResultadoEdicion(
            imagenRotada,
            "Rotación derecha",
            AppActionCommands.CMD_IMAGEN_ROTAR_DER,
            true // Rotar SÍ cambia dimensiones
        );
    }

    // --- MÉTODOS HELPER PRIVADOS ---

    private boolean validarPrecondiciones(String nombreMetodo) {
        if (model == null || controller == null || zoomManager == null) {
            System.err.println("ERROR [" + nombreMetodo + "]: Dependencias nulas.");
            return false;
        }
        if (model.getCurrentImage() == null) {
            System.out.println("[" + nombreMetodo + "] No hay imagen cargada.");
            return false;
        }
        return true;
    }

    private void procesarResultadoEdicion(BufferedImage imagenEditada, String nombreOperacion, String actionCommand, boolean resetearZoom) {
        JFrame mainFrame = controller.getView(); // Obtener el frame desde el controller
        
        if (imagenEditada != null) {
            System.out.println("  -> " + nombreOperacion + " exitosa. Actualizando modelo...");
            model.setCurrentImage(imagenEditada);
            
            if (resetearZoom) {
                System.out.println("  -> Reseteando zoom/pan debido a cambio de dimensiones.");
                model.resetZoomState();
            }
            
            zoomManager.refrescarVistaSincrono();
            
            // Llamar a la animación a través del ConfigManager obtenido del Controller
            ConfigApplicationManager configManager = controller.getConfigApplicationManager();
            if (configManager != null) {
                configManager.aplicarAnimacionBoton(actionCommand);
            }
            
            System.out.println("[EditionManager] " + nombreOperacion + " aplicada y vista actualizada.");
        } else {
            System.err.println("ERROR [EditionManager]: " + nombreOperacion + " devolvió null.");
            JOptionPane.showMessageDialog(mainFrame, "No se pudo realizar la operación: " + nombreOperacion, "Error de Edición", JOptionPane.ERROR_MESSAGE);
        }
    }
} // --- FIN de la clase EditionManager ---