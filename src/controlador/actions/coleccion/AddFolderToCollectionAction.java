package controlador.actions.coleccion;

import java.awt.event.ActionEvent;
import java.nio.file.Path;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import controlador.managers.ImageListManager;
import modelo.VisorModel;

/**
 * Acción para iniciar manualmente el proceso de indexación (sincronización)
 * de la carpeta que está activa en el contexto del VISUALIZADOR.
 */
public class AddFolderToCollectionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final ImageListManager imageListManager;
    private final VisorModel model;

    public AddFolderToCollectionAction(String text, ImageIcon icon, ImageListManager imageListManager, VisorModel model) {
        super(text, icon);
        this.imageListManager = imageListManager;
        this.model = model;
        putValue(SHORT_DESCRIPTION, "Añade/actualiza los archivos de la carpeta del explorador a la Colección.");
    } // ---FIN de constructor [AddFolderToCollectionAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (imageListManager == null || model == null) {
            return;
        }

        // --- ¡LA SOLUCIÓN! ---
        // Obtenemos la carpeta desde el contexto específico del VISUALIZADOR,
        // que es el que actúa como nuestro "explorador de archivos".
        Path carpetaAIndexar = model.getVisualizadorListContext().getCarpetaRaizContexto();

        if (carpetaAIndexar == null) {
            JOptionPane.showMessageDialog(null, 
                "No hay una carpeta seleccionada para añadir a la colección.\n\nPrimero, abre una carpeta en el Modo Visualizador.", 
                "Acción no disponible", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Antes de sincronizar, nos aseguramos de que el modelo principal esté
        // apuntando a la carpeta correcta para que sincronizarCarpetaConBD() funcione.
        model.setCarpetaRaizActual(carpetaAIndexar);

        imageListManager.sincronizarCarpetaConBD();
    } // ---FIN de metodo [actionPerformed]---

} // --- FIN de clase AddFolderToCollectionAction ---