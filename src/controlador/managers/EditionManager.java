package controlador.managers;

import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.JOptionPane;

import controlador.VisorController; // <-- NUEVO IMPORT
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IEditionManager;
import controlador.managers.interfaces.IZoomManager;
import modelo.VisorModel;
import servicios.image.ImageEdition;
// No se necesita importar VisorView

public class EditionManager  implements IEditionManager{

    private  VisorModel model;
    private  VisorController controller; // <-- CAMBIO: Referencia al controlador
    private  IZoomManager zoomManager;

    public EditionManager() {
        // Constructor vacío. Las dependencias se inyectan a través de setters.
    } 

    @Override
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
    }// --- Fin del método aplicarVolteoHorizontal ---

    @Override
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
    }// --- Fin del método aplicarVolteoVertical ---

    @Override
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
    }// --- Fin del método aplicarRotarIzquierda ---

    @Override
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
    }// --- Fin del método aplicarRotarDerecha ---
    
    @Override
    public void aplicarRecorte() {
        // Mostrar un JOptionPane informativo como placeholder
        if (controller != null && controller.getView() != null) {
            JOptionPane.showMessageDialog(
                controller.getView(),
                "La funcionalidad de 'Recortar' está pendiente de implementación.",
                "Funcionalidad en Desarrollo",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            System.err.println("La funcionalidad de 'Recortar' está pendiente de implementación (no se pudo mostrar diálogo).");
        }
    } // --- Fin del método aplicarRecorte ---

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
    } // --- Fin del método validarPrecondiciones ---

    private void procesarResultadoEdicion(BufferedImage imagenEditada, String nombreOperacion, String actionCommand, boolean necesitaResetZoom) {
        // La validación del controlador es crucial, ya que es nuestro canal de notificación.
        if (controller == null) {
            System.err.println("ERROR CRÍTICO [procesarResultadoEdicion]: VisorController es nulo. No se puede continuar la operación.");
            return;
        }

        if (imagenEditada != null) {
            System.out.println("  -> " + nombreOperacion + " exitosa. Actualizando modelo...");
            model.setCurrentImage(imagenEditada);
            
            // --- INICIO DE LA MODIFICACIÓN CLAVE ---
            // En lugar de llamar a zoomManager.refrescarVistaSincrono() directamente,
            // notificamos al controlador para que él se encargue de orquestar el refresco.
            System.out.println("  -> Notificando al VisorController para que refresque la vista...");
            controller.solicitarRefrescoDeVistaPorEdicion(necesitaResetZoom);
            // --- FIN DE LA MODIFICACIÓN CLAVE ---
            
            // La animación del botón se mantiene, ya que es una responsabilidad cosmética
            // que puede manejar el ConfigApplicationManager a través del Controller.
            ConfigApplicationManager configManager = controller.getConfigApplicationManager();
            if (configManager != null) {
                configManager.aplicarAnimacionBoton(actionCommand);
            }
            
            System.out.println("[EditionManager] " + nombreOperacion + " aplicada y notificación enviada.");
        } else {
            System.err.println("ERROR [EditionManager]: La operación '" + nombreOperacion + "' devolvió una imagen nula.");
            if (controller.getView() != null) {
                JOptionPane.showMessageDialog(
                    controller.getView(), 
                    "No se pudo realizar la operación: " + nombreOperacion, 
                    "Error de Edición", 
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    } // --- Fin del método procesarResultadoEdicion ---
    
    
 // --- INICIO DE LA MODIFICACIÓN: Setters para inyección de dependencias ---
    public void setModel(VisorModel model) {this.model = Objects.requireNonNull(model);}
    public void setController(VisorController controller) {this.controller = Objects.requireNonNull(controller);}
    public void setZoomManager(IZoomManager zoomManager) {this.zoomManager = Objects.requireNonNull(zoomManager);}
    
} // --- FIN de la clase EditionManager ---