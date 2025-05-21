package controlador.actions.especiales;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import controlador.commands.AppActionCommands;
import vista.VisorView; // Para obtener el componente fuente
import vista.config.MenuItemDefinition; // Para la estructura del menú
import vista.builders.PopupMenuBuilder;   // Para construir el JPopupMenu
import vista.config.ViewUIConfig;       // Para pasar al PopupMenuBuilder

public class MenuAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    // NO necesitamos VisorView directamente aquí si el PopupMenuBuilder se encarga
    // de la apariencia y solo necesitamos el componente fuente del evento.
    // private VisorView viewRef; 
    
    private List<MenuItemDefinition> menuStructureToDisplay; // La estructura COMPLETA del menú principal
    private Map<String, Action> actionMapRef;              // El actionMap completo
    private ViewUIConfig uiConfigRef;                      // Para el PopupMenuBuilder

    // Constructor REFACTORIZADO
    public MenuAction(
            // VisorView view, // Eliminado si no se usa directamente
            String name,
            ImageIcon icon,
            List<MenuItemDefinition> fullMenuStructure, // La estructura de la JMenuBar
            Map<String, Action> actionMap,
            ViewUIConfig uiConfig
    ) {
        super(name, icon);
        // this.viewRef = view; // Ya no es necesario si solo usamos e.getSource()
        this.menuStructureToDisplay = Objects.requireNonNull(fullMenuStructure, "La estructura del menú no puede ser nula");
        this.actionMapRef = Objects.requireNonNull(actionMap, "ActionMap no puede ser nulo");
        this.uiConfigRef = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser nulo");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar el menú principal de la aplicación");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ESPECIAL_MENU);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (menuStructureToDisplay == null || actionMapRef == null || uiConfigRef == null) {
            System.err.println("ERROR CRÍTICO [MenuAction]: Dependencias para construir el menú nulas.");
            return;
        }

        Object source = e.getSource();
        if (!(source instanceof Component)) {
            System.err.println("[MenuAction] La fuente del evento no es un Component. No se puede mostrar el menú emergente.");
            return;
        }
        Component invokerComponent = (Component) source;

        System.out.println("[MenuAction actionPerformed] Mostrando menú emergente replicando la JMenuBar...");

        // Crear el JPopupMenu usando el PopupMenuBuilder
        // El PopupMenuBuilder necesita poder manejar la estructura jerárquica completa
        // tal como lo hace MenuBarBuilder.
        PopupMenuBuilder popupBuilder = new PopupMenuBuilder(this.uiConfigRef);
        JPopupMenu popupMenu = popupBuilder.buildPopupMenuWithNestedMenus(this.menuStructureToDisplay, this.actionMapRef);

        // Mostrar el menú emergente en la posición del botón que invocó la acción
        // Se muestra debajo del botón.
        popupMenu.show(invokerComponent, 0, invokerComponent.getHeight());
    }
}