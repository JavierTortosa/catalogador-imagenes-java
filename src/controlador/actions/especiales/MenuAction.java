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
import servicios.ConfigurationManager;
import vista.builders.PopupMenuBuilder;
import vista.config.MenuItemDefinition;
import vista.theme.ThemeManager;

public class MenuAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final List<MenuItemDefinition> menuStructureToDisplay;
    private final Map<String, Action> actionMapRef;
    private final ConfigurationManager configManagerRef;
    private final ThemeManager themeManager;

    /**
     * Constructor refactorizado que ya no depende de un ActionListener de fallback.
     * @param name El nombre de la acción.
     * @param icon El icono para la acción.
     * @param fullMenuStructure La estructura completa del menú a mostrar.
     * @param actionMap El mapa de acciones de la aplicación.
     * @param themeManager El gestor de temas para aplicar estilos.
     * @param configManager El gestor de configuración.
     */
    public MenuAction(
            String name,
            ImageIcon icon,
            List<MenuItemDefinition> fullMenuStructure,
            Map<String, Action> actionMap,
            ThemeManager themeManager,
            ConfigurationManager configManager
    ) {
        super(name, icon);
        this.menuStructureToDisplay = Objects.requireNonNull(fullMenuStructure, "La estructura del menú no puede ser nula");
        this.actionMapRef = Objects.requireNonNull(actionMap, "ActionMap no puede ser nulo");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser nulo");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser nulo en MenuAction");
        
        putValue(Action.SHORT_DESCRIPTION, "Mostrar el menú principal de la aplicación");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ESPECIAL_MENU);
    } // ---FIN de metodo MenuAction (constructor)---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (menuStructureToDisplay == null || actionMapRef == null || themeManager == null) {
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

        // Crear el JPopupMenu usando el PopupMenuBuilder corregido.
        PopupMenuBuilder popupBuilder = new PopupMenuBuilder(this.themeManager, this.configManagerRef);
        JPopupMenu popupMenu = popupBuilder.buildPopupMenuWithNestedMenus(this.menuStructureToDisplay, this.actionMapRef);

        // Mostrar el menú emergente en la posición del botón que invocó la acción.
        // Se muestra debajo del botón.
        popupMenu.show(invokerComponent, 0, invokerComponent.getHeight());
    } // ---FIN de metodo actionPerformed---
    
} // --- FIN de la clase MenuAction ---