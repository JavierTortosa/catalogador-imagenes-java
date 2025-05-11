// Paquete: controlador.actions.vista (o similar)
package controlador.actions.vista;

import java.awt.event.ActionEvent;

import javax.swing.Action;
// Quita la importación de ImageIcon si no vas a ponerle un icono a esta Action directamente
// import javax.swing.ImageIcon;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import controlador.commands.AppActionCommands;
import vista.util.IconUtils;
// Quita IconUtils si no se usa aquí
// import vista.util.IconUtils;

public class MostrarDialogoListaAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L; // Genera uno si es necesario

    public MostrarDialogoListaAction(VisorController controller, IconUtils iconUtils, int iconoAncho, int iconoAlto) {
        // 1. Llama al constructor de BaseVisorAction.
        //    El texto "Mostrar Lista de Imágenes" podría usarse si un JMenuItem
        //    se crea directamente desde esta Action sin un texto explícito en MenuItemDefinition.
        //    Si MenuItemDefinition siempre provee el texto, este nombre es menos crítico.
        super("Mostrar Lista de Imágenes", controller); // O el texto que prefieras para Action.NAME

        // 2. Establecer propiedades de la Action
        putValue(Action.SHORT_DESCRIPTION, "Muestra un diálogo con la lista de todas las imágenes cargadas");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA);

        // 3. Icono (Opcional para esta Action)
        //    Si este comando SOLO va a estar en el menú, probablemente no necesite un icono aquí.
        //    Si también lo usas en un botón de la toolbar que NO tiene un icono definido en
        //    ToolbarButtonDefinition, entonces podrías añadirlo.
        //    Por ahora, lo dejaremos sin icono en la Action, ya que el ToolbarButtonDefinition
        //    para "4004-Lista_48x48.png" ya especifica el icono.
        
        if (iconUtils != null) { // Asumiendo que pasas iconUtils y las dimensiones
            ImageIcon icon = iconUtils.getScaledIcon("4004-Lista_48x48.png", iconoAncho, iconoAlto);
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
            }
        }
        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null) {
            controller.logActionInfo(e); // Buena práctica
            controller.mostrarDialogoListaImagenes(); // Llama al método existente en el controller
        } else {
            System.err.println("Error: Controller es null en MostrarDialogoListaAction.");
        }
    }
}