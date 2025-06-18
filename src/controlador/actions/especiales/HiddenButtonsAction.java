package controlador.actions.especiales;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
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
import vista.config.MenuItemType;     // Para definir los items del popup
import vista.theme.ThemeManager;
//import vista.config.ViewUIConfig;       // Para pasar al PopupMenuBuilder

public class HiddenButtonsAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    // private VisorView viewRef; // No es estrictamente necesario si solo usamos e.getSource()
    private Map<String, Action> actionMapRef; // Para obtener las Actions de los botones ocultos
//    private ViewUIConfig uiConfigRef;         // Para el PopupMenuBuilder
    private ThemeManager themeManager;
    
    // Podríamos tener una lista de los COMANDOS de los botones que podrían ir aquí
    // private List<String> overflowButtonCommands; 
    private ConfigurationManager configManagerRef;         // <--- NUEVO CAMPO
    private ActionListener specialConfigActionListenerRef;

    // Constructor REFACTORIZADO
    public HiddenButtonsAction(
            // VisorView view, // Opcional
            String name,
            ImageIcon icon,
            Map<String, Action> actionMap, // Para obtener las actions de los botones
            ThemeManager themeManager,         // Para el builder
            ConfigurationManager configManager,         
            ActionListener specialConfigActionListener
            // List<String> overflowButtonCommands // Opcional: lista de comandos a mostrar
            
    ) {
        super(name, icon);
        // this.viewRef = view;
        this.actionMapRef = Objects.requireNonNull(actionMap, "ActionMap no puede ser nulo");
//        this.uiConfigRef = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser nulo");
        this.themeManager = Objects.requireNonNull(themeManager, "ViewUIConfig no puede ser nulo");
        
        this.configManagerRef = Objects.requireNonNull(configManager);
        this.specialConfigActionListenerRef = Objects.requireNonNull(specialConfigActionListener);
        // this.overflowButtonCommands = overflowButtonCommands != null ? overflowButtonCommands : new ArrayList<>();

        putValue(Action.SHORT_DESCRIPTION, "Mostrar acciones adicionales o botones que no caben");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (actionMapRef == null || themeManager == null) {
            System.err.println("ERROR CRÍTICO [HiddenButtonsAction]: Dependencias nulas.");
            return;
        }

        Object source = e.getSource();
        if (!(source instanceof Component)) {
            System.err.println("[HiddenButtonsAction] La fuente del evento no es un Component.");
            return;
        }
        Component invokerComponent = (Component) source;

        System.out.println("[HiddenButtonsAction actionPerformed] Mostrando menú de botones ocultos/adicionales...");

        // --- Lógica para determinar qué botones mostrar ---
        // ESTA ES LA PARTE COMPLEJA QUE DEJAREMOS COMO TODO O SIMPLIFICADA
        List<MenuItemDefinition> itemsParaPopup = new ArrayList<>();

        // TODO: Implementar la lógica para determinar qué botones están "ocultos"
        // y necesitan mostrarse en este menú.
        // Por ahora, podemos poner unos placeholders o una lista fija.

        // Ejemplo con placeholders/lista fija:
        // (Usa los AppActionCommands de los botones que quieras que aparezcan aquí)
        String[] comandosBotonesOcultosEjemplo = {
            AppActionCommands.CMD_ARCHIVO_IMPRIMIR, // Suponiendo que Imprimir podría estar oculto
            AppActionCommands.CMD_IMAGEN_PROPIEDADES,
            // ... añade más comandos de acciones que quieras en este menú de desbordamiento
        };

        for (String comando : comandosBotonesOcultosEjemplo) {
            Action action = actionMapRef.get(comando);
            if (action != null) {
                String actionName = (String) action.getValue(Action.NAME);
                // Usamos el nombre de la Action como texto del ítem de menú
                itemsParaPopup.add(new MenuItemDefinition(comando, MenuItemType.ITEM, actionName != null ? actionName : comando, null));
            }
        }
        
        if (itemsParaPopup.isEmpty()) {
            itemsParaPopup.add(new MenuItemDefinition(null, MenuItemType.ITEM, "(No hay acciones adicionales)", null));
        }

        // --- Construir y mostrar el JPopupMenu ---
        PopupMenuBuilder popupBuilder = new PopupMenuBuilder(
        		this.themeManager,
                this.configManagerRef,
                this.specialConfigActionListenerRef
            );
        
        JPopupMenu popupMenu = popupBuilder.buildPopupMenuWithNestedMenus(itemsParaPopup, this.actionMapRef);

        popupMenu.show(invokerComponent, 0, invokerComponent.getHeight());
    }
}