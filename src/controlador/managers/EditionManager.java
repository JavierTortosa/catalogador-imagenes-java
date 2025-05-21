package controlador.managers;

import java.awt.image.BufferedImage;

import javax.swing.JOptionPane;

import controlador.commands.AppActionCommands;
import modelo.VisorModel;
import servicios.image.ImageEdition; // Tu servicio que realmente hace el volteo
import vista.VisorView;
import controlador.managers.ZoomManager;

public class EditionManager /* implements IEditionManager */ {

    private VisorModel model;
    private VisorView view;
    // private ImageEdition imageEditionService; // Si ImageEdition fuera instanciable
    private ZoomManager zoomManager; // Referencia al ZoomManager

    // Constructor
    public EditionManager(VisorModel model, VisorView view, ZoomManager zoomManager /*, ImageEdition imageEditionService (si es instanciable)*/) {
        this.model = model;
        this.view = view;
        this.zoomManager = zoomManager; // Guardar la referencia
        // this.imageEditionService = imageEditionService;
    }

    /**
     * Aplica un volteo horizontal a la imagen principal actualmente almacenada en el modelo.
     * Actualiza el modelo con la imagen volteada y luego refresca la vista.
     */
    public void aplicarVolteoHorizontal() {
        // --- SECCIÓN 1: VALIDACIONES INICIALES ---
        // 1.1. Validar que las dependencias necesarias (modelo, vista, zoomManager) existan.
        if (model == null || view == null || zoomManager == null) {
            System.err.println("ERROR [EditionManager.aplicarVolteoHorizontal]: Modelo, Vista o ZoomManager nulos.");
            return; // No se puede proceder sin estas dependencias.
        }

        // 1.2. Obtener la imagen original (BufferedImage) desde el VisorModel.
        BufferedImage imagenOriginal = model.getCurrentImage();

        // 1.3. Validar si hay una imagen cargada actualmente en el modelo.
        if (imagenOriginal == null) {
            System.out.println("[EditionManager.aplicarVolteoHorizontal] No hay imagen cargada en el modelo para voltear.");
            // Opcional: Mostrar un mensaje al usuario a través de la vista.
            // JOptionPane.showMessageDialog(view.getFrame(), "No hay imagen cargada para voltear.", "Acción no disponible", JOptionPane.INFORMATION_MESSAGE);
            return; // Salir si no hay imagen.
        }

        System.out.println("[EditionManager.aplicarVolteoHorizontal] Solicitando volteo horizontal a ImageEdition...");

        // --- SECCIÓN 2: REALIZAR LA OPERACIÓN DE VOLTEO ---
        // 2.1. Variable para almacenar la imagen resultante del volteo.
        BufferedImage imagenVolteada = null;
        // 2.2. Bloque try-catch para la operación de edición.
        try {
            // Llamar al método estático de tu servicio ImageEdition.
            imagenVolteada = ImageEdition.flipHorizontal(imagenOriginal);
        } catch (Exception e) {
            // Capturar cualquier excepción inesperada durante la operación de volteo.
            System.err.println("ERROR [EditionManager.aplicarVolteoHorizontal] Excepción en ImageEdition.flipHorizontal: " + e.getMessage());
            e.printStackTrace(); // Imprimir el stack trace para depuración.
            JOptionPane.showMessageDialog(
                view.getFrame(), // Usar el frame de la vista como padre del diálogo.
                "Ocurrió un error inesperado al intentar voltear la imagen.",
                "Error de Edición",
                JOptionPane.ERROR_MESSAGE
            );
            return; // Salir del método si ocurre un error en la edición.
        }

        // --- SECCIÓN 3: PROCESAR EL RESULTADO DE LA OPERACIÓN ---
        // 3.1. Comprobar si la operación de volteo fue exitosa (la imagen resultante no es null).
        if (imagenVolteada != null) {
            // === Caso Éxito: Imagen Volteada Correctamente ===
            System.out.println("  -> Volteo horizontal exitoso. Actualizando modelo...");
            
            // 3.1.1. Actualizar la imagen actual en el VisorModel con la imagen volteada.
            model.setCurrentImage(imagenVolteada);

            // 3.1.2. Resetear el estado de zoom y paneo en el modelo.
            //        Esto es opcional para el volteo (que no cambia dimensiones),
            //        pero puede ser un comportamiento deseado para que el usuario vea
            //        la imagen centrada y sin zoom después de una edición.
            //        Para rotaciones de 90º es más crítico. Decide si lo quieres aquí.
            // model.resetZoomState();
            // System.out.println("  -> (Opcional) Estado de zoom/pan reseteado en el Modelo.");


            // 3.1.3. Solicitar al ZoomManager que refresque la visualización de la imagen principal.
            //        ZoomManager usará el currentImage (ahora volteado) del modelo,
            //        el zoomFactor y offsets actuales del modelo, y ImageDisplayUtils.
            System.out.println("  -> Solicitando a ZoomManager que refresque la vista principal...");
            zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
            
            // 3.1.4. Aplicar una animación visual al botón correspondiente en la toolbar (feedback para el usuario).
            //        Se asume que el comando corto del botón es "Espejo_Horizontal_48x48".
            //        Este comando se usaría en UIDefinitionService para este botón.
//            view.aplicarAnimacionBoton(AppActionCommands.CMD_IMG_ESPEJO_H_SIMPLE); // Usar la constante del comando
            view.aplicarAnimacionBoton(AppActionCommands.CMD_IMAGEN_VOLTEAR_H);//TOOLBAR_BTN_VOLTEAR_H);
            
            System.out.println("[EditionManager.aplicarVolteoHorizontal] Volteo horizontal aplicado y vista actualizada.");

        } else {
            // === Caso Error: ImageEdition.flipHorizontal devolvió null ===
            System.err.println("ERROR [EditionManager.aplicarVolteoHorizontal]: ImageEdition.flipHorizontal devolvió null.");
            JOptionPane.showMessageDialog(
                view.getFrame(),
                "No se pudo realizar el volteo horizontal de la imagen.",
                "Error de Volteo",
                JOptionPane.ERROR_MESSAGE
            );
        }
    } // --- FIN aplicarVolteoHorizontal ---


    /**
     * Aplica un volteo vertical a la imagen principal actualmente almacenada en el modelo.
     * Actualiza el modelo con la imagen volteada y luego refresca la vista.
     */
    public void aplicarVolteoVertical() {
        // --- SECCIÓN 1: VALIDACIONES INICIALES ---
        // 1.1. Validar que las dependencias necesarias (modelo, vista, zoomManager) existan.
        if (model == null || view == null || zoomManager == null) {
            System.err.println("ERROR [EditionManager.aplicarVolteoVertical]: Modelo, Vista o ZoomManager nulos.");
            return;
        }

        // 1.2. Obtener la imagen original (BufferedImage) desde el VisorModel.
        BufferedImage imagenOriginal = model.getCurrentImage();

        // 1.3. Validar si hay una imagen cargada actualmente en el modelo.
        if (imagenOriginal == null) {
            System.out.println("[EditionManager.aplicarVolteoVertical] No hay imagen cargada en el modelo para voltear.");
            return;
        }

        System.out.println("[EditionManager.aplicarVolteoVertical] Solicitando volteo vertical a ImageEdition...");

        // --- SECCIÓN 2: REALIZAR LA OPERACIÓN DE VOLTEO ---
        // 2.1. Variable para almacenar la imagen resultante del volteo.
        BufferedImage imagenVolteada = null;
        // 2.2. Bloque try-catch para la operación de edición.
        try {
            imagenVolteada = ImageEdition.flipVertical(imagenOriginal);
        } catch (Exception e) {
            System.err.println("ERROR [EditionManager.aplicarVolteoVertical] Excepción en ImageEdition.flipVertical: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(view.getFrame(), "Ocurrió un error inesperado al intentar voltear la imagen.", "Error de Edición", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- SECCIÓN 3: PROCESAR EL RESULTADO DE LA OPERACIÓN ---
        // 3.1. Comprobar si la operación de volteo fue exitosa.
        if (imagenVolteada != null) {
            System.out.println("  -> Volteo vertical exitoso. Actualizando modelo...");
            
            // 3.1.1. Actualizar la imagen actual en el VisorModel.
            model.setCurrentImage(imagenVolteada);

            // 3.1.2. (Opcional) Resetear zoom/pan. Para volteo, usualmente no es necesario si las dimensiones no cambian.
            // model.resetZoomState();

            // 3.1.3. Solicitar al ZoomManager que refresque la visualización.
            System.out.println("  -> Solicitando a ZoomManager que refresque la vista principal...");
            zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
            
            // 3.1.4. Aplicar animación visual al botón.
//                view.aplicarAnimacionBoton(AppActionCommands.CMD_IMG_ESPEJO_V_SIMPLE); // Comando del botón de volteo vertical
            view.aplicarAnimacionBoton(AppActionCommands.CMD_IMAGEN_VOLTEAR_V);//.TOOLBAR_BTN_VOLTEAR_V);
            
            System.out.println("[EditionManager.aplicarVolteoVertical] Volteo vertical aplicado y vista actualizada.");

        } else {
            System.err.println("ERROR [EditionManager.aplicarVolteoVertical]: ImageEdition.flipVertical devolvió null.");
            JOptionPane.showMessageDialog(view.getFrame(), "No se pudo realizar el volteo vertical de la imagen.", "Error de Volteo", JOptionPane.ERROR_MESSAGE);
        }
    } // --- FIN aplicarVolteoVertical ---


    /**
     * Aplica una rotación de 90 grados hacia la izquierda (antihorario) a la imagen principal.
     * Actualiza el modelo, resetea el estado de zoom/pan (porque las dimensiones cambian) y refresca la vista.
     */
    public void aplicarRotarIzquierda() {
        // --- SECCIÓN 1: VALIDACIONES INICIALES ---
        if (model == null || view == null || zoomManager == null) {
            System.err.println("ERROR [EditionManager.aplicarRotarIzquierda]: Modelo, Vista o ZoomManager nulos.");
            return;
        }
        BufferedImage imagenOriginal = model.getCurrentImage();
        if (imagenOriginal == null) {
            System.out.println("[EditionManager.aplicarRotarIzquierda] No hay imagen cargada para rotar.");
            return;
        }
        System.out.println("[EditionManager.aplicarRotarIzquierda] Solicitando rotación izquierda...");

        // --- SECCIÓN 2: REALIZAR LA OPERACIÓN DE ROTACIÓN ---
        BufferedImage imagenRotada = null;
        try {
            imagenRotada = ImageEdition.rotateLeft(imagenOriginal);
        } catch (Exception e) {
            System.err.println("ERROR [EditionManager.aplicarRotarIzquierda] Excepción en ImageEdition.rotateLeft: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(view.getFrame(), "Ocurrió un error inesperado al intentar rotar la imagen.", "Error de Edición", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- SECCIÓN 3: PROCESAR EL RESULTADO DE LA OPERACIÓN ---
        if (imagenRotada != null) {
            System.out.println("  -> Rotación izquierda exitosa. Actualizando modelo...");
            
            // 3.1.1. Actualizar la imagen en el VisorModel.
            model.setCurrentImage(imagenRotada);

            // 3.1.2. ¡CRUCIAL! Resetear el estado de zoom y paneo porque las dimensiones de la imagen han cambiado.
            System.out.println("  -> Reseteando zoom/pan debido a cambio de dimensiones por rotación.");
            model.resetZoomState(); 
            
            // 3.1.3. Solicitar al ZoomManager que refresque la visualización.
            System.out.println("  -> Solicitando a ZoomManager que refresque la vista principal...");
            zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
            
            // 3.1.4. Aplicar animación visual al botón.
//                view.aplicarAnimacionBoton(AppActionCommands.CMD_IMG_ROTAR_IZQ_SIMPLE); // Comando del botón de rotar izquierda
            view.aplicarAnimacionBoton(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ);//.TOOLBAR_BTN_ROTAR_IZQ);
            
            System.out.println("[EditionManager.aplicarRotarIzquierda] Rotación izquierda aplicada y vista actualizada.");
        } else {
            System.err.println("ERROR [EditionManager.aplicarRotarIzquierda]: ImageEdition.rotateLeft devolvió null.");
            JOptionPane.showMessageDialog(view.getFrame(), "No se pudo realizar la rotación izquierda de la imagen.", "Error de Rotación", JOptionPane.ERROR_MESSAGE);
        }
    } // --- FIN aplicarRotarIzquierda ---


    /**
     * Aplica una rotación de 90 grados hacia la derecha (horario) a la imagen principal.
     * Actualiza el modelo, resetea el estado de zoom/pan (porque las dimensiones cambian) y refresca la vista.
     */
    public void aplicarRotarDerecha() {
        // --- SECCIÓN 1: VALIDACIONES INICIALES ---
        if (model == null || view == null || zoomManager == null) {
            System.err.println("ERROR [EditionManager.aplicarRotarDerecha]: Modelo, Vista o ZoomManager nulos.");
            return;
        }
        BufferedImage imagenOriginal = model.getCurrentImage();
        if (imagenOriginal == null) {
            System.out.println("[EditionManager.aplicarRotarDerecha] No hay imagen cargada para rotar.");
            return;
        }
        System.out.println("[EditionManager.aplicarRotarDerecha] Solicitando rotación derecha...");

        // --- SECCIÓN 2: REALIZAR LA OPERACIÓN DE ROTACIÓN ---
        BufferedImage imagenRotada = null;
        try {
            imagenRotada = ImageEdition.rotateRight(imagenOriginal);
        } catch (Exception e) {
            System.err.println("ERROR [EditionManager.aplicarRotarDerecha] Excepción en ImageEdition.rotateRight: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(view.getFrame(), "Ocurrió un error inesperado al intentar rotar la imagen.", "Error de Edición", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- SECCIÓN 3: PROCESAR EL RESULTADO DE LA OPERACIÓN ---
        if (imagenRotada != null) {
            System.out.println("  -> Rotación derecha exitosa. Actualizando modelo...");

            // 3.1.1. Actualizar la imagen en el VisorModel.
            model.setCurrentImage(imagenRotada);

            // 3.1.2. ¡CRUCIAL! Resetear el estado de zoom y paneo.
            System.out.println("  -> Reseteando zoom/pan debido a cambio de dimensiones por rotación.");
            model.resetZoomState();
            
            // 3.1.3. Solicitar al ZoomManager que refresque la visualización.
            System.out.println("  -> Solicitando a ZoomManager que refresque la vista principal...");
            zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
            
            // 3.1.4. Aplicar animación visual al botón.
//                view.aplicarAnimacionBoton(AppActionCommands.CMD_IMG_ROTAR_DER_SIMPLE); // Comando del botón de rotar derecha
            view.aplicarAnimacionBoton(AppActionCommands.CMD_IMAGEN_ROTAR_DER);//.TOOLBAR_BTN_ROTAR_DER);
            
            System.out.println("[EditionManager.aplicarRotarDerecha] Rotación derecha aplicada y vista actualizada.");
        } else {
            System.err.println("ERROR [EditionManager.aplicarRotarDerecha]: ImageEdition.rotateRight devolvió null.");
            JOptionPane.showMessageDialog(view.getFrame(), "No se pudo realizar la rotación derecha de la imagen.", "Error de Rotación", JOptionPane.ERROR_MESSAGE);
        }
    } // --- FIN aplicarRotarDerecha ---

    // El método aplicarRecorte(...) lo dejaremos para más adelante.
}
