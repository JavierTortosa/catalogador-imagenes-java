package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.commands.AppActionCommands;
import modelo.VisorModel; // Dependencia para obtener la clave actual, etc.
import java.util.Objects; // Para Objects.requireNonNull

public class RefreshAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private VisorModel model;
    // private Runnable recargarCallback; // Opción para el futuro

    public RefreshAction(String name, ImageIcon icon, VisorModel model /*, Runnable recargarCallback */) {
        super(name, icon);
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser nulo en RefreshAction");
        // this.recargarCallback = recargarCallback;

        putValue(Action.SHORT_DESCRIPTION, "Recargar la lista de archivos de la carpeta actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ESPECIAL_REFRESCAR);
        
        // La Action de refrescar usualmente está habilitada si hay una carpeta cargada
        // o si el modelo no está "vacío". Podrías controlar su estado 'enabled' aquí o
        // desde un manager/controller que sepa cuándo es apropiado refrescar.
        // Por ahora, la dejamos habilitada por defecto.
        // setEnabled(false); // Podría empezar deshabilitada y habilitarse cuando haya una lista.
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[RefreshAction actionPerformed] - Comando: " + e.getActionCommand());

        if (this.model == null) {
            System.err.println("ERROR [RefreshAction]: VisorModel es nulo.");
            return;
        }

        String claveActual = model.getSelectedImageKey();
        System.out.println("  -> RefreshAction: Se debería recargar la lista, intentando mantener la selección en: " + claveActual);
        
        // AQUÍ IRÍA LA LÓGICA PARA DISPARAR LA RECARGA DE LA LISTA.
        // Como discutimos, esto es un punto delicado.
        // Opciones:
        // 1. Que esta Action tenga una referencia a un Manager (ej. ListManager, FileOperationsManager)
        //    que tenga un método recargarLista(claveActual).
        // 2. Que esta Action dispare un evento que VisorController escuche.
        // 3. Que se le inyecte un callback (Runnable/Consumer) a esta Action.

        // Por ahora, solo un TODO:
        System.out.println("  TODO: Implementar la llamada REAL a la lógica de recarga de lista desde RefreshAction.");
        // Ejemplo de cómo podría ser con un callback (si se inyecta `recargarCallback`):
        // if (recargarCallback != null) {
        //     recargarCallback.run(); // O si el callback necesita la clave: ((Consumer<String>)recargarCallback).accept(claveActual);
        // } else {
        //     System.err.println("  RefreshAction: recargarCallback es null, no se puede ejecutar la recarga.");
        // }
        
        // TEMPORALMENTE: Si para avanzar necesitas que haga *algo* y aún no tienes los managers,
        // podrías tener un método público en VisorController y una referencia (¡no ideal!).
        // NO HAGAS ESTO A MENOS QUE SEA ABSOLUTAMENTE PARA DESBLOQUEARTE Y LO QUITES LUEGO:
        // if (VisorController.getInstanciaGlobal() != null) { // Suponiendo un getter estático (¡malo!)
        //    VisorController.getInstanciaGlobal().cargarListaImagenes(claveActual);
        // }
    }
}