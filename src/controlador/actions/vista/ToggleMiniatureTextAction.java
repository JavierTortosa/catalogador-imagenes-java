package controlador.actions.vista; 

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem; // Para la lógica del actionPerformed

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import controlador.commands.AppActionCommands;

 public class ToggleMiniatureTextAction extends BaseVisorAction {

     private static final long serialVersionUID = 1L;

     public ToggleMiniatureTextAction(VisorController controller) {
         super("Mostrar Nombres en Miniaturas", controller); // Texto para Action.NAME
         putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar los nombres de archivo debajo de las miniaturas");
         putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_VISTA_TOGGLE_MINIATURE_TEXT);

         // Leer el estado inicial desde ConfigurationManager para SELECTED_KEY
         if (controller != null && controller.getConfigurationManager() != null) {
             boolean initialState = controller.getConfigurationManager().getBoolean("miniaturas.ui.mostrar_nombres", true);
             putValue(Action.SELECTED_KEY, initialState);
         } else {
             putValue(Action.SELECTED_KEY, Boolean.TRUE); // Fallback si no hay config
             System.err.println("WARN [ToggleMiniatureTextAction]: Controller o ConfigManager nulos al inicializar estado.");
         }
     }

     @Override
     public void actionPerformed(ActionEvent e) {
         if (controller == null) {
             System.err.println("Error: Controller es null en ToggleMiniatureTextAction.");
             return;
         }
         controller.logActionInfo(e);

         boolean nuevoEstadoLogico;
         Object source = e.getSource();
         if (source instanceof JCheckBoxMenuItem) {
             nuevoEstadoLogico = ((JCheckBoxMenuItem) source).isSelected();
         } else {
             boolean estadoActualAction = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
             nuevoEstadoLogico = !estadoActualAction;
         }

         // Actualizar el SELECTED_KEY de esta Action primero
         putValue(Action.SELECTED_KEY, nuevoEstadoLogico);
         System.out.println("  [ToggleMiniatureTextAction] Action.SELECTED_KEY actualizado a: " + nuevoEstadoLogico);

         // Llamar al método del controller para aplicar el cambio y guardar config
         controller.setMostrarNombresMiniaturas(nuevoEstadoLogico);
     }
 } // --- FIN clase ToggleMiniatureTextAction
 