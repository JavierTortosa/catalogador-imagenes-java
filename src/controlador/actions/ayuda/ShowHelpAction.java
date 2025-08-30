package controlador.actions.ayuda;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import vista.VisorView;
import vista.dialogos.HelpDialog;

public class ShowHelpAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final VisorView mainView; // Necesitamos la ventana principal como "dueña" del diálogo

    public ShowHelpAction(String name, ImageIcon icon, VisorView mainView) {
        super(name, icon);
        this.mainView = Objects.requireNonNull(mainView, "VisorView no puede ser null en ShowHelpAction");
        
        putValue(Action.SHORT_DESCRIPTION, "Abre la ventana de ayuda de la aplicación");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_AYUDA_MOSTRAR_GUIA); // Nuevo comando
    } // --- Fin del constructor ShowHelpAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Crear una instancia de nuestro diálogo de ayuda.
        //    Le pasamos la ventana principal (mainView) como "owner".
        //    Esto asegura que el diálogo se comporte correctamente (ej. se centre sobre la app).
        HelpDialog helpDialog = new HelpDialog(this.mainView);
        
        // 2. Hacer visible el diálogo.
        //    Como el diálogo es modal, la ejecución del código se detendrá aquí
        //    hasta que el usuario cierre la ventana de ayuda.
        helpDialog.setVisible(true);
    } // --- FIN del método actionPerformed ---

} // --- FIN de la clase ShowHelpAction ---