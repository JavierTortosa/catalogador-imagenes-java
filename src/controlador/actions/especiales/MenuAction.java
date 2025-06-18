package controlador.actions.especiales;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import controlador.commands.AppActionCommands;
import servicios.ConfigurationManager;
import vista.builders.PopupMenuBuilder;   // Para construir el JPopupMenu
import vista.config.MenuItemDefinition; // Para la estructura del menú
import vista.config.ViewUIConfig;       // Para pasar al PopupMenuBuilder
import vista.theme.ThemeManager;

public class MenuAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private List<MenuItemDefinition> menuStructureToDisplay; // La estructura COMPLETA del menú principal
    private Map<String, Action> actionMapRef;              // El actionMap completo
//    private ViewUIConfig uiConfigRef;                      // Para el PopupMenuBuilder
    private ConfigurationManager configManagerRef;         // 
    private ActionListener specialConfigActionListenerRef; //
    private ThemeManager themeManager;
    
    
    // Constructor REFACTORIZADO
    public MenuAction(
            String name,
            ImageIcon icon,
            List<MenuItemDefinition> fullMenuStructure,
            Map<String, Action> actionMap,
//            ViewUIConfig uiConfig,
            ThemeManager themeManager,
            ConfigurationManager configManager,
            ActionListener specialConfigActionListener
    ) {
        super(name, icon);
        // this.viewRef = view; // Ya no es necesario si solo usamos e.getSource()
        this.menuStructureToDisplay = Objects.requireNonNull(fullMenuStructure, "La estructura del menú no puede ser nula");
        this.actionMapRef = Objects.requireNonNull(actionMap, "ActionMap no puede ser nulo");
        this.themeManager = Objects.requireNonNull(themeManager, "ViewUIConfig no puede ser nulo");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser nulo en MenuAction");
        this.specialConfigActionListenerRef = Objects.requireNonNull(specialConfigActionListener, "specialConfigActionListener (VisorController) no puede ser nulo en MenuAction");
        
        putValue(Action.SHORT_DESCRIPTION, "Mostrar el menú principal de la aplicación");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ESPECIAL_MENU);
    }

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

        // Crear el JPopupMenu usando el PopupMenuBuilder
        // El PopupMenuBuilder necesita poder manejar la estructura jerárquica completa
        // tal como lo hace MenuBarBuilder.
        PopupMenuBuilder popupBuilder = new PopupMenuBuilder(this.themeManager, this.configManagerRef, this.specialConfigActionListenerRef);
        JPopupMenu popupMenu = popupBuilder.buildPopupMenuWithNestedMenus(this.menuStructureToDisplay, this.actionMapRef);

        // Mostrar el menú emergente en la posición del botón que invocó la acción
        // Se muestra debajo del botón.
        popupMenu.show(invokerComponent, 0, invokerComponent.getHeight());
    }
}