package controlador.managers;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.actions.config.SetInfoBarTextFormatAction;
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IViewManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import vista.VisorView;
import vista.builders.ViewBuilder;
import vista.panels.ImageDisplayPanel;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;

public class ViewManager implements IViewManager, ThemeChangeListener, ClipboardOwner {

	private static final Logger logger = LoggerFactory.getLogger(ViewManager.class);
	
    private VisorView view;
    private ConfigurationManager configuration;
    private ComponentRegistry registry;
    private ThemeManager themeManager;
    private Map<String, Action> actionMap;
    private Map<String, AbstractButton> botonesPorNombre;
    private ToolbarManager toolbarManager;
    private ViewBuilder viewBuilder;
    private VisorModel model;
    private DisplayModeManager displayModeManager;
    private InfobarStatusManager statusBarManager;
    

    /**
     * Constructor refactorizado de ViewManager.
     * Ahora es un constructor simple, sin parámetros. Las dependencias
     * se inyectan a través de setters.
     */
    public ViewManager() {
        // El constructor ahora está vacío. La inicialización se hace
        // en los setters.
    } // --- Fin del método ViewManager (constructor) ---

    
    /**
     * Realiza la operación técnica de poner o quitar el modo de pantalla completa.
     * Este método es "sin estado" (stateless) y solo ejecuta la orden recibida.
     *
     * @param fullScreenState true para entrar en pantalla completa, false para salir.
     */
    public void setFullScreen(boolean fullScreenState) {
        if (view == null) {
            logger.error("ERROR [ViewManager.setFullScreen]: La vista (JFrame) es nula.");
            return;
        }

        GraphicsDevice device = view.getGraphicsConfiguration().getDevice();
        
        logger.debug("  [ViewManager] Ejecutando cambio a pantalla completa. Estado solicitado: " + (fullScreenState ? "ACTIVADO" : "DESACTIVADO"));

        // Comprobar si el cambio es realmente necesario para evitar trabajo extra.
        // device.getFullScreenWindow() devuelve la ventana actual en pantalla completa, o null si no hay ninguna.
        boolean isCurrentlyFullScreen = (device.getFullScreenWindow() == view);
        if (isCurrentlyFullScreen == fullScreenState) {
            logger.debug("  -> La ventana ya está en el estado solicitado. No se realiza ninguna acción.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            view.dispose();
            view.setUndecorated(fullScreenState);
            
            if (fullScreenState) {
                device.setFullScreenWindow(view);
            } else {
                device.setFullScreenWindow(null);
            }
            
            view.setVisible(true);
        });
        
    } // --- Fin del método setFullScreen ---
    
    
    /**
     * Activa o desactiva el fondo a cuadros como opción por defecto.
     * Esta acción es persistente y es llamada por la Action del menú.
     * @param activar true para activar el fondo a cuadros, false para desactivarlo.
     */
    @Override
    public void setCheckeredBackgroundEnabled(boolean activar) {
        if (registry == null || configuration == null) {
            logger.error("ERROR [ViewManager]: Registry o ConfigurationManager nulos.");
            return;
        }

        String configKey = "interfaz.menu.vista.fondo_a_cuadros.seleccionado";
        configuration.setString(configKey, String.valueOf(activar));
        
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel != null) {
            displayPanel.setCheckeredBackground(activar);
        } else {
            logger.error("ERROR [ViewManager]: No se pudo encontrar 'panel.display.imagen' en el registro.");
        }
    } // --- Fin del método setCheckeredBackgroundEnabled ---

    /**
     * Establece un color de fondo sólido para la SESIÓN ACTUAL.
     * Este método es llamado por los "puntos de color".
     * @param color El color a aplicar.
     */
    @Override
    public void setSessionBackgroundColor(java.awt.Color color) {
        if (registry == null) return;
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel != null) {
            displayPanel.setSolidBackgroundColor(color);
        }
    } // --- Fin del método setSessionBackgroundColor ---

    /**
     * Establece el fondo a cuadros para la SESIÓN ACTUAL.
     * Este método es llamado por el "punto de color" de cuadros.
     */
    @Override
    public void setSessionCheckeredBackground() {
        if (registry == null) return;
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel != null) {
            displayPanel.setCheckeredBackground(true);
        }
    } // --- Fin del método setSessionCheckeredBackground ---
    
    /**
     * Abre un JColorChooser para que el usuario elija un color de fondo personalizado para la SESIÓN ACTUAL.
     */
    @Override
    public void requestCustomBackgroundColor() {
        if (registry == null) return;

        javax.swing.JFrame mainFrame = registry.get("frame.main"); 
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");

        if (displayPanel == null || mainFrame == null) {
            logger.error("ERROR [ViewManager]: No se puede abrir JColorChooser, falta 'frame.main' o 'panel.display.imagen' en el registro.");
            return;
        }
        
        java.awt.Color colorActual = displayPanel.getBackground();
        java.awt.Color colorElegido = javax.swing.JColorChooser.showDialog(mainFrame, "Seleccionar Color de Fondo", colorActual);
        
        if (colorElegido != null) {
            setSessionBackgroundColor(colorElegido);
        }
    } // --- Fin del método requestCustomBackgroundColor ---

    
    /**
     * Punto de entrada para solicitar actualizaciones de visibilidad de componentes de la UI.
     * AHORA sabe distinguir entre una solicitud para una barra de herramientas completa
     * y una solicitud para un botón individual dentro de una barra.
     * @param uiElementId El identificador del componente o zona a actualizar. Para un botón, este
     *                    será el ID de la toolbar que lo contiene (ej: "navegacion").
     * @param configKey La clave de configuración que cambió. Para un botón, esta será la clave
     *                  completa de visibilidad del botón (ej: "interfaz.boton...").
     * @param nuevoEstado El nuevo estado de visibilidad (true para visible, false para oculto).
     */
    @Override
    public void solicitarActualizacionUI(String uiElementId, String configKey, boolean nuevoEstado) {
        logger.debug("[ViewManager] Solicitud de actualización para UI: '" + uiElementId + "' -> " + nuevoEstado + " (Clave: " + configKey + ")");
        
        // Añadimos la lógica para guardar el estado en la configuración, que es lo que faltaba.
        // Esta lógica se ejecuta para TODOS los casos, ya sean botones, toolbars o paneles.
        if (configKey != null && !configKey.isBlank()) {
            if (configuration != null) {
                configuration.setString(configKey, String.valueOf(nuevoEstado));
                logger.debug("  -> Configuración '" + configKey + "' actualizada a: " + nuevoEstado);
            } else {
                logger.error("  ERROR: ConfigurationManager es nulo. No se puede guardar el estado para la clave: " + configKey);
            }
        }
        
        if (registry == null) {
            logger.error("  ERROR: ComponentRegistry es nulo en ViewManager.");
            return;
        }
        
        
        // CASO 1: La solicitud es para un BOTÓN INDIVIDUAL
        // Lo detectamos porque configKey no es nulo y empieza con "interfaz.boton."
        if (configKey != null && configKey.startsWith("interfaz.boton.")) {
            // La clave base del botón es la clave de config sin el ".visible"
            String buttonBaseKey = configKey.replace(".visible", "");
            AbstractButton button = registry.get(buttonBaseKey);
            
            if (button != null) {
                if (button.isVisible() != nuevoEstado) {
                    button.setVisible(nuevoEstado);
                    logger.debug("  -> Visibilidad del botón '" + buttonBaseKey + "' cambiada a " + nuevoEstado);
                    
                    // Forzar un redibujado de la barra de herramientas que lo contiene.
                    if (button.getParent() instanceof JToolBar) {
                        JToolBar parentToolbar = (JToolBar) button.getParent();
                        parentToolbar.revalidate();
                        parentToolbar.repaint();
                        logger.debug("  -> Contenedor de toolbar '" + parentToolbar.getName() + "' actualizado.");
                    }
                }
                return; // Trabajo hecho, salimos.
            }
        }
        
        // CASO 2: La solicitud es para una BARRA DE HERRAMIENTAS COMPLETA
        // Lo detectamos porque el uiElementId coincide con una clave de toolbar
        String toolbarRegistryKey = "toolbar." + uiElementId;
        JToolBar toolbar = registry.get(toolbarRegistryKey);
        
        if (toolbar != null) {
            if (toolbar.isVisible() != nuevoEstado) {
                toolbar.setVisible(nuevoEstado);
                logger.debug("  -> Visibilidad de la toolbar '" + uiElementId + "' cambiada a " + nuevoEstado);
                
                toolbar.setOpaque(false);
                toolbar.setBorder(null);
                
                revalidateToolbarContainer();
            }
            return; // Trabajo hecho, salimos.
        }
        
        // CASO 3: Lógica de fallback para otros componentes principales (la que ya tenías)
        boolean necesitaRevalidateGeneral = false;
        JFrame mainFrame = registry.get("frame.main");
        if (mainFrame == null) {
            logger.error("  ERROR: El frame principal no está en el registro.");
            return;
        }

        switch (uiElementId) {
            case "Barra_de_Menu":
                JMenuBar menuBar = mainFrame.getJMenuBar();
                if (menuBar != null && menuBar.isVisible() != nuevoEstado) {
                    menuBar.setVisible(nuevoEstado);
                    necesitaRevalidateGeneral = true;
                }
                
                setBotonMenuEspecialVisible(!nuevoEstado);
                break;
            
            case "Barra_de_Botones":
                JPanel toolbarContainer = registry.get("container.toolbars");
                if(toolbarContainer != null && toolbarContainer.isVisible() != nuevoEstado) {
                    toolbarContainer.setVisible(nuevoEstado);
                    necesitaRevalidateGeneral = true;
                }
                break;
              
            case "mostrar_ocultar_la_lista_de_archivos":
                JPanel panelIzquierdo = registry.get("panel.izquierdo.contenedorPrincipal");
                if (panelIzquierdo != null && panelIzquierdo.isVisible() != nuevoEstado) {
                    panelIzquierdo.setVisible(nuevoEstado);
                    ajustarDivisorSplitPane(nuevoEstado);
                    necesitaRevalidateGeneral = true; // Marcamos para que se redibuje el frame.
                }
                break;
                
            case "imagenes_en_miniatura":
                JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");
                if (scrollMiniaturas != null && scrollMiniaturas.isVisible() != nuevoEstado) {
                    scrollMiniaturas.setVisible(nuevoEstado);
                    necesitaRevalidateGeneral = true;
                }
                break;

            case "barra_de_estado":
                JPanel bottomStatusBar = registry.get("panel.estado.inferior");
                if (bottomStatusBar != null && bottomStatusBar.isVisible() != nuevoEstado) {
                    bottomStatusBar.setVisible(nuevoEstado);
                    necesitaRevalidateGeneral = true;
                    logger.info("  -> Visibilidad de 'Barra de Estado' (panel.estado.inferior) cambiada a " + nuevoEstado);
                }
                break;
                
            case "barra_de_info_imagen":
                JPanel topInfoPanel = registry.get("panel.info.superior");
                if (topInfoPanel != null && topInfoPanel.isVisible() != nuevoEstado) {
                    topInfoPanel.setVisible(nuevoEstado);
                    necesitaRevalidateGeneral = true;
                    logger.info("  -> Visibilidad de 'Barra_de_Info_Superior' (panel.info.superior) cambiada a " + nuevoEstado);
                }
                break;
                
                
                
            default:
                // Si llegamos aquí, realmente no sabemos qué es.
                logger.warn("  WARN [ViewManager]: uiElementId no reconocido o no manejado por ninguna lógica: '" + uiElementId + "'");
                break;
        }

        if (necesitaRevalidateGeneral) {
            SwingUtilities.invokeLater(() -> {
                mainFrame.revalidate();
                mainFrame.repaint();
            });
        }
    } // --- Fin del método solicitarActualizacionUI ---

    
    @Override
    public void onThemeChanged(Tema nuevoTema) {
        logger.debug("[ViewManager] Notificación de cambio de tema recibida...");
        SwingUtilities.invokeLater(() -> {
            
            // --- INICIO DE LA CORRECCIÓN CON CASTING SEGURO ---
            Component panelVisorComp = registry.get("panel.display.imagen");
            if (panelVisorComp instanceof ImageDisplayPanel) {
                ((ImageDisplayPanel) panelVisorComp).actualizarColorDeFondoPorTema(this.themeManager);
            }

            Component panelProyectoComp = registry.get("panel.proyecto.display");
            if (panelProyectoComp instanceof ImageDisplayPanel) {
                ((ImageDisplayPanel) panelProyectoComp).actualizarColorDeFondoPorTema(this.themeManager);
            }

            Component panelCarruselComp = registry.get("panel.display.carousel");
            if (panelCarruselComp instanceof ImageDisplayPanel) {
                ((ImageDisplayPanel) panelCarruselComp).actualizarColorDeFondoPorTema(this.themeManager);
            }
            // --- FIN DE LA CORRECCIÓN ---

            refrescarColoresDeFondoUI();
            
            // La lógica de bordes ya no está aquí, se ha movido.
            // La llamada a actualizarResaltadoDeFoco se queda para repintar las JLists.
            actualizarResaltadoDeFoco(java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());

            logger.debug("[ViewManager] Actualización de paneles por cambio de tema completada.");
        });
            
    } // --- FIN del método onThemeChanged ---
    
    
    /**
     * Helper para revalidar el contenedor de las barras de herramientas.
     */
    @Override
    public void revalidateToolbarContainer() {
        JPanel toolbarContainer = registry.get("container.toolbars");
        if (toolbarContainer != null) {
            toolbarContainer.revalidate();
            toolbarContainer.repaint();
        } else {
            logger.warn("WARN [ViewManager]: 'container.toolbars' no encontrado.");
        }
    } // --- Fin del método revalidateToolbarContainer ---

    /**
     * Orquesta un refresco completo de la apariencia de la UI aplicando el tema actual.
     */
    @Override
    public void ejecutarRefrescoCompletoUI() {
        logger.debug("\n--- [ViewManager] Ejecutando Refresco Completo de la UI ---");
        logger.debug("  -> Lógica de refresco de tema a implementar aquí.");
    } // --- Fin del método ejecutarRefrescoCompletoUI ---

    /**
     * Restablece el fondo del visor a su estado POR DEFECTO, según lo define la configuración.
     */
    @Override
    public void refrescarFondoAlPorDefecto() {
        logger.debug("[ViewManager] Refrescando fondo al estado por defecto...");

        if (registry == null || configuration == null || themeManager == null) {
            logger.error("  ERROR: Dependencias nulas (registry, config o themeManager).");
            return;
        }

        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel == null) {
            logger.error("  ERROR: 'panel.display.imagen' no encontrado en el registro.");
            return;
        }

        String configKey = "interfaz.menu.vista.fondo_a_cuadros.seleccionado";
        boolean esCuadrosPorDefecto = configuration.getBoolean(configKey, false);

        if (esCuadrosPorDefecto) {
            logger.debug("  -> El defecto es fondo a cuadros. Aplicando.");
            displayPanel.setCheckeredBackground(true);
        } else {
            Tema temaActual = themeManager.getTemaActual();
            logger.debug("  -> El defecto es color de tema. Aplicando color: " + temaActual.colorFondoSecundario());
            displayPanel.setSolidBackgroundColor(temaActual.colorFondoSecundario());
        }
    } // --- Fin del método refrescarFondoAlPorDefecto ---

    private void ajustarDivisorSplitPane(boolean panelVisible) {
        JSplitPane splitPane = registry.get("splitpane.main");
        if (splitPane != null) {
            if (panelVisible) {
                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.25));
            }
        }
    } // --- Fin del método ajustarDivisorSplitPane ---
    
    /**
     * Sincroniza las Actions de formato para la barra superior.
     */
    @Override
    public void sincronizarAccionesFormatoBarraSuperior() {
        if (this.actionMap == null) return;
        Action action1 = this.actionMap.get(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_SOLO_NOMBRE);
        if (action1 instanceof SetInfoBarTextFormatAction) {
            ((SetInfoBarTextFormatAction) action1).sincronizarSelectedKeyConConfig();
        }
        Action action2 = this.actionMap.get(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_RUTA_COMPLETA);
        if (action2 instanceof SetInfoBarTextFormatAction) {
            ((SetInfoBarTextFormatAction) action2).sincronizarSelectedKeyConConfig();
        }
    } // --- Fin del método sincronizarAccionesFormatoBarraSuperior ---

    /**
     * Sincroniza las Actions de formato para la barra inferior.
     */
    @Override
    public void sincronizarAccionesFormatoBarraInferior() {
        if (this.actionMap == null) return;
        Action action1 = this.actionMap.get(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_SOLO_NOMBRE);
        if (action1 instanceof SetInfoBarTextFormatAction) {
            ((SetInfoBarTextFormatAction) action1).sincronizarSelectedKeyConConfig();
        }
        Action action2 = this.actionMap.get(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_RUTA_COMPLETA);
        if (action2 instanceof SetInfoBarTextFormatAction) {
            ((SetInfoBarTextFormatAction) action2).sincronizarSelectedKeyConConfig();
        }
    } // --- Fin del método sincronizarAccionesFormatoBarraInferior ---
    
    /**
     * Sincroniza el estado visual inicial de todos los radios de formato.
     */
    @Override
    public void sincronizarEstadoVisualInicialDeRadiosDeFormato() {
        logger.debug("[ViewManager] Sincronizando Actions de formato...");
        sincronizarAccionesFormatoBarraSuperior();
        sincronizarAccionesFormatoBarraInferior();
    } // --- Fin del método sincronizarEstadoVisualInicialDeRadiosDeFormato ---
    
    @Override
    public void setBotonMenuEspecialVisible(boolean visible) {
        if (this.botonesPorNombre == null) return;
        
        AbstractButton boton = this.botonesPorNombre.get("interfaz.boton.especiales.Menu_48x48");

        if (boton != null) {
            if (boton.isVisible() != visible) {
                boton.setVisible(visible);
                revalidateToolbarContainer();
            }
        }
    } // --- Fin del método setBotonMenuEspecialVisible ---
    
    
    @Override
    public void refrescarColoresDeFondoUI() {
        logger.debug("  [ViewManager] Refrescando colores de fondo de los paneles principales...");
        if (registry == null || themeManager == null) {
            logger.error("  ERROR [ViewManager]: Registry o ThemeManager nulos.");
            return;
        }

        // 1. Obtener TODOS los colores que vamos a necesitar del tema actual UNA SOLA VEZ.
        Tema temaActual = themeManager.getTemaActual();
        Color colorFondoPrincipal = temaActual.colorFondoPrincipal();
        Color colorFondoSecundario = temaActual.colorFondoSecundario();
        
        // En lugar de calcular un color (ej. .darker()), leemos directamente
        // nuestra propiedad personalizada del UIManager. Esta es la única fuente de verdad.
        Color colorFondoStatusBars = UIManager.getColor(ThemeManager.KEY_STATUSBAR_BACKGROUND);
        
        // 2. Definimos qué paneles reciben qué color.
        List<String> panelesConFondoPrincipal = List.of(
            "panel.north.wrapper",
            "container.toolbars",
            "container.toolbars.left",
            "container.toolbars.center",
            "container.toolbars.right"
        );

        // Aplicamos el color de fondo principal
        for (String key : panelesConFondoPrincipal) {
            JPanel panel = registry.get(key);
            if (panel != null) {
                panel.setBackground(colorFondoPrincipal);
            }
        }
        
        // 3. Aplicamos el color ESPECIAL a las barras de estado.
        JPanel panelInfoSuperior = registry.get("panel.info.superior");
        if (panelInfoSuperior != null) {
            panelInfoSuperior.setBackground(colorFondoStatusBars);
        }
        
        JPanel panelEstadoInferior = registry.get("panel.estado.inferior");
        if (panelEstadoInferior != null) {
            panelEstadoInferior.setBackground(colorFondoStatusBars);
        }
        
        // 4. Lógica existente para Viewports y Toolbars (se mantiene igual).
        JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas.carousel");
        if (scrollMiniaturas != null) {
            scrollMiniaturas.getViewport().setBackground(colorFondoSecundario);
        }
        scrollMiniaturas = registry.get("scroll.miniaturas");
        if (scrollMiniaturas != null) {
            scrollMiniaturas.getViewport().setBackground(colorFondoSecundario);
        }
        
        if (toolbarManager != null) {
            for (JToolBar tb : toolbarManager.getManagedToolbars().values()) {
                tb.setBackground(colorFondoPrincipal);
            }
        }
        
        JFrame mainFrame = registry.get("frame.main");
        if (mainFrame != null) {
            mainFrame.repaint();
        }
    } // --- Fin del método refrescarColoresDeFondoUI ---

    
    /**
     * Cambia la vista activa en un contenedor de CardLayout específico.
     * @param containerRegistryKey La clave en el ComponentRegistry del JPanel que usa CardLayout (ej. "container.vistas", "container.displaymodes").
     * @param viewName La clave de la vista a mostrar (el nombre de la "tarjeta" en el CardLayout).
     */
    @Override 
    public void cambiarAVista(String containerRegistryKey, String viewName) { 
        if (registry == null) {
            logger.error("ERROR [ViewManager]: Registry es nulo, no se puede cambiar de vista.");
            return;
        }
        
        JPanel container = registry.get(containerRegistryKey); 
        
        if (container != null && container.getLayout() instanceof CardLayout) {
            CardLayout cl = (CardLayout) container.getLayout();
            cl.show(container, viewName);
            logger.debug("[ViewManager] Vista cambiada en '" + containerRegistryKey + "' a: " + viewName);
        } else {
            // Este es el error que estás viendo: "No se encontró 'container.vistas' o no usa CardLayout."
            logger.error("ERROR [ViewManager]: No se encontró el contenedor '" + containerRegistryKey + "' o no usa CardLayout.");
        }
    } // --- Fin del método cambiarAVista ---

    @Override
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = actionMap;
    } // --- Fin del método setActionMap ---

    @Override
    public void setBotonesPorNombre(Map<String, AbstractButton> botones) {
        this.botonesPorNombre = botones;
    } // --- Fin del método setBotonesPorNombre ---

    @Override
    public void setView(VisorView view) {
        this.view = view;
    } // --- Fin del método setView ---

    @Override
    public void setConfiguration(ConfigurationManager configuration) {
        this.configuration = configuration;
    } // --- Fin del método setConfiguration ---

    @Override
    public void setRegistry(ComponentRegistry registry) {
        this.registry = registry;
    } // --- Fin del método setRegistry ---

    @Override
    public void setThemeManager(ThemeManager themeManager) {
        this.themeManager = themeManager;
    } // --- Fin del método setThemeManager ---

    
    @Override // <-- La anotación @Override es buena práctica aquí
    public VisorView getView() {
        return this.view;
    }
    
    
    @Override
    public void reconstruirPanelesEspecialesTrasTema() {
        logger.debug("  [ViewManager] Re-colocando barras de herramientas especiales tras cambio de tema...");

        if (toolbarManager == null || registry == null) {
            logger.error("  ERROR CRÍTICO: ToolbarManager o ComponentRegistry nulos.");
            return;
        }

        // --- RECONSTRUCCIÓN DE LA BARRA DE CONTROL DE FONDO (VISOR) ---
        // Esta lógica se queda como está, ya que el panel visor es simple.
        reconstruirBarraLibre("controles_imagen_inferior", "panel.derecho.visor", BorderLayout.SOUTH);
        
        // --- RECONSTRUCCIÓN DE LA BARRA DE ACCIONES DE EXPORTACIÓN (PROYECTO) ---
        // >>> INICIO DE LA LÓGICA CORREGIDA Y SEGURA <<<
        
        // 1. Obtenemos la instancia específica del ExportPanel desde el registro.
        vista.panels.export.ExportPanel exportPanel = registry.get("panel.proyecto.exportacion");
        
        if (exportPanel != null) {
            // 2. Pedimos al ToolbarManager la barra reconstruida.
            //    Como la caché se vació, esto creará una nueva instancia con el tema correcto.
            JToolBar nuevaBarraExportacion = toolbarManager.getToolbar("acciones_exportacion");
            
            if (nuevaBarraExportacion != null) {
                // 3. Forzamos la actualización de la UI de la nueva barra y sus botones.
                SwingUtilities.updateComponentTreeUI(nuevaBarraExportacion);
                
                // 4. Le entregamos la nueva barra al ExportPanel para que él la coloque.
                //    Ahora el ViewManager no sabe ni necesita saber cómo está hecho el ExportPanel por dentro.
                exportPanel.setActionsToolbar(nuevaBarraExportacion);
                
                logger.debug("    -> Nueva barra 'acciones_exportacion' entregada al ExportPanel para su actualización.");
            } else {
                logger.warn("  WARN: ToolbarManager no pudo proporcionar la barra 'acciones_exportacion'.");
            }
        } else {
            logger.warn("  WARN: No se encontró el componente 'panel.proyecto.exportacion' en el registro.");
        }
        // >>> FIN DE LA LÓGICA CORREGIDA <<<
        
        logger.debug("  [ViewManager] Re-colocación de paneles especiales finalizada.");
    } // --- FIN del metodo reconstruirPanelesEspecialesTrasTema  ---
    
    
    public void initializeFocusBorders() {
        logger.info("[ViewManager] La gestión de bordes de foco ha sido transferida a GeneralController.");
        // Este método se deja intencionadamente vacío.
    }// --- FIN del metodo initializeFocusBorders ---
    
    
    /**
     * Ya no pinta bordes. Su única responsabilidad es forzar un repintado
     * de las JLists para que el renderer actualice el color de selección.
     */
    public void actualizarResaltadoDeFoco(Component newFocusOwner) {
        // La lógica de bordes se ha movido a GeneralController.
        // Solo repintamos las listas para el feedback de selección activa/inactiva.
        if (registry == null) return;
        
        repaintListInside(registry.get("panel.izquierdo.listaArchivos"));
        repaintListInside(registry.get("scroll.miniaturas"));
        repaintListInside(registry.get("scroll.proyecto.nombres"));
        repaintListInside(registry.get("scroll.proyecto.descartes"));
    } // --- FIN del metodo actualizarResaltadoDeFoco ---
    
    
    /**
     * Método de ayuda que busca una JList dentro de un JComponent (típicamente un JScrollPane)
     * y, si la encuentra, solicita que se repinte.
     * @param container El componente contenedor (ej. un JScrollPane).
     */
    private void repaintListInside(JComponent container) {
        if (container instanceof JScrollPane) {
            Component view = ((JScrollPane) container).getViewport().getView();
            if (view instanceof JList) {
                view.repaint();
            }
        } else {
            // Podríamos añadir lógica para buscar en JPanels si fuera necesario
            for (Component child : container.getComponents()) {
                if (child instanceof JList) {
                    child.repaint();
                    return; // Asumimos que solo hay una JList principal por panel de foco
                }
            }
        }
    } // ---FIN de metodo repaintListInside---
    
    
    /**
     * Método helper REFORZADO para reconstruir y recolocar una barra de herramientas "libre".
     * @param claveBarra La clave de la barra en el UIDefinitionService.
     * @param clavePanelContenedor La clave del panel contenedor en el ComponentRegistry.
     * @param layoutConstraint La restricción del BorderLayout (e.g., BorderLayout.NORTH).
     */
    public void reconstruirBarraLibre(String claveBarra, String clavePanelContenedor, String layoutConstraint) {
        JPanel panelContenedor = registry.get(clavePanelContenedor);
        
        if (panelContenedor == null) {
            logger.warn("  WARN [reconstruirBarraLibre]: No se encontró el panel contenedor '" + clavePanelContenedor + "'.");
            return;
        }

        // --- 1. Quitar la barra antigua del panel, si existe. ---
        // Buscamos entre los hijos del panel un componente que sea una JToolBar
        // y cuyo nombre coincida con la clave de la barra que queremos reemplazar.
        JToolBar barraAntigua = null;
        for (Component comp : panelContenedor.getComponents()) {
            if (comp instanceof JToolBar && claveBarra.equals(comp.getName())) {
                barraAntigua = (JToolBar) comp;
                break;
            }
        }
        
        if (barraAntigua != null) {
            panelContenedor.remove(barraAntigua);
            logger.debug("    -> Barra antigua '" + claveBarra + "' eliminada del panel '" + clavePanelContenedor + "'.");
        }

        // 2. Pedir al ToolbarManager la barra. Como la caché está vacía, esto la reconstruirá.
        JToolBar nuevaBarra = toolbarManager.getToolbar(claveBarra);
        
        if (nuevaBarra != null) {
            // Forzar la actualización de la UI de la nueva barra con el tema actual
            logger.debug("    -> Forzando updateComponentTreeUI en la barra '" + claveBarra + "'...");
            SwingUtilities.updateComponentTreeUI(nuevaBarra);
            
            // Aplicar configuraciones especiales si es necesario
            if ("controles_imagen_inferior".equals(claveBarra)) {
                nuevaBarra.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 3, 1));
                nuevaBarra.setOpaque(false);
            }
            
            // 3. Añadir la barra recién reconstruida y actualizada al panel.
            panelContenedor.add(nuevaBarra, layoutConstraint);
            
            // 4. Revalidar y repintar el contenedor.
            panelContenedor.revalidate();
            panelContenedor.repaint();
            
            logger.debug("    -> Nueva barra '" + claveBarra + "' re-colocada en '" + clavePanelContenedor + "'.");
        } else {
            logger.warn("  WARN [reconstruirBarraLibre]: ToolbarManager no pudo proporcionar la barra '" + claveBarra + "'.");
        }
        
    }	// --- Fin del método reconstruirBarraLibre ---
    
 // ***************************************************************************************************************************
 // ******************************************************************************* GESTIÓN DEL DIÁLOGO DE LA LISTA DE IMÁGENES
 // ***************************************************************************************************************************
    
    
    /**
     * Muestra un diálogo modal que contiene una lista de los archivos de imagen
     * actualmente cargados en el modelo principal. Permite al usuario ver la lista
     * completa y, opcionalmente, copiarla al portapapeles, mostrando nombres de archivo
     * relativos o rutas completas.
     */
     public void mostrarDialogoListaImagenes() {
         // 1. Validar dependencias (Vista y Modelo necesarios)
         if (view == null || model == null) {
             logger.error("ERROR [mostrarDialogoListaImagenes]: Vista o Modelo nulos. No se puede mostrar el diálogo.");
             // Podríamos mostrar un JOptionPane de error aquí si fuera crítico
             return;
         }
         logger.debug("[Controller] Abriendo diálogo de lista de imágenes...");

         // 2. Crear el JDialog
         //    - Lo hacemos modal (true) para que bloquee la ventana principal mientras está abierto.
         //    - Usamos view.getFrame() como padre para que se centre correctamente.
         final JDialog dialogoLista = new JDialog(view, "Lista de Imágenes Cargadas", true);
         dialogoLista.setSize(600, 400); // Tamaño inicial razonable
         dialogoLista.setLocationRelativeTo(view); // Centrar sobre la ventana principal
         dialogoLista.setLayout(new BorderLayout(5, 5)); // Layout principal del diálogo

         // 3. Crear componentes internos del diálogo
         
         // 3.1. Modelo para la JList del diálogo (será llenado dinámicamente)
         final DefaultListModel<String> modeloListaDialogo = new DefaultListModel<>();
         
         // 3.2. JList que usará el modelo anterior
         JList<String> listaImagenesDialogo = new JList<>(modeloListaDialogo);
         
         // 3.3. ScrollPane para la JList (indispensable si la lista es larga)
         JScrollPane scrollPaneListaDialogo = new JScrollPane(listaImagenesDialogo);
         
         // 3.4. CheckBox para alternar entre nombres relativos y rutas completas
         final JCheckBox checkBoxMostrarRutas = new JCheckBox("Mostrar Rutas Completas");
         
         // 3.5. Botón para copiar la lista visible al portapapeles
         JButton botonCopiarLista = new JButton("Copiar Lista");

         // 4. Configurar Panel Superior (Botón Copiar y CheckBox)
         JPanel panelSuperiorDialog = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Alineación izquierda
         panelSuperiorDialog.add(botonCopiarLista);
         panelSuperiorDialog.add(checkBoxMostrarRutas);

         // 5. Añadir Componentes al Layout del Diálogo
         dialogoLista.add(panelSuperiorDialog, BorderLayout.NORTH);  // Panel superior arriba
         dialogoLista.add(scrollPaneListaDialogo, BorderLayout.CENTER); // Lista (en scroll) en el centro

         // 6. Añadir ActionListeners a los controles interactivos
         
         // 6.1. Listener para el CheckBox (actualiza la lista cuando cambia su estado)
         checkBoxMostrarRutas.addActionListener(e -> {
             // Llama al método helper para refrescar el contenido de la lista del diálogo
             // pasándole el modelo del diálogo y el estado actual del checkbox.
             actualizarListaEnDialogo(modeloListaDialogo, checkBoxMostrarRutas.isSelected());
         });

         // 6.2. Listener para el Botón Copiar
         botonCopiarLista.addActionListener(e -> {
         
       	  // Llama al método helper que copia el contenido del modelo del diálogo
             copiarListaAlPortapapeles(modeloListaDialogo);
         });

         // 7. Cargar el contenido inicial de la lista en el diálogo
         //    Se llama una vez antes de mostrar el diálogo, usando el estado inicial del checkbox (desmarcado).
         logger.debug("  -> Actualizando contenido inicial del diálogo...");
         actualizarListaEnDialogo(modeloListaDialogo, checkBoxMostrarRutas.isSelected());

         // 8. Hacer visible el diálogo
         //    Como es modal, la ejecución se detendrá aquí hasta que el usuario cierre el diálogo.
         logger.debug("  -> Mostrando diálogo...");
         dialogoLista.setVisible(true);

         // 9. Código después de cerrar el diálogo (si es necesario)
         //    Aquí podríamos hacer algo una vez el diálogo se cierra, pero usualmente no es necesario.
         logger.debug("[Controller] Diálogo de lista de imágenes cerrado.");

     } // --- FIN mostrarDialogoListaImagenes ---
     
     
     /**
      * Actualiza el contenido del DefaultListModel proporcionado (que pertenece
      * al diálogo de la lista de imágenes) basándose en el modelo principal
      * de la aplicación (model.getModeloLista()) y el mapa de rutas completas
      * (model.getRutaCompletaMap()).
      *
      * Llena el modelo del diálogo con las claves relativas o las rutas absolutas
      * de los archivos, según el valor del parámetro 'mostrarRutas'.
      *
      * @param modeloDialogo El DefaultListModel del JList que se encuentra en el diálogo.
      *                      Este método modificará su contenido (lo limpia y lo vuelve a llenar).
      * @param mostrarRutas  boolean que indica qué formato mostrar:
      *                      - true: Muestra la ruta completa (absoluta) de cada archivo.
      *                      - false: Muestra la clave única (ruta relativa) de cada archivo.
      */
     private void actualizarListaEnDialogo(DefaultListModel<String> modeloDialogo, boolean mostrarRutas) {
         // 1. Validación de entradas
         if (modeloDialogo == null) {
             logger.error("ERROR [actualizarListaEnDialogo]: El modelo del diálogo es null.");
             return;
         }
         if (model == null || model.getModeloLista() == null || model.getRutaCompletaMap() == null) {
             logger.error("ERROR [actualizarListaEnDialogo]: El modelo principal o sus componentes internos son null.");
             modeloDialogo.clear(); // Limpiar el diálogo si no hay datos fuente
             modeloDialogo.addElement("Error: No se pudo acceder a los datos de la lista principal.");
             return;
         }

         // 2. Referencias al modelo principal y al mapa de rutas
         DefaultListModel<String> modeloPrincipal = model.getModeloLista();
         Map<String, Path> mapaRutas = model.getRutaCompletaMap();

         // 3. Log informativo
         logger.debug("  [Dialogo Lista] Actualizando contenido. Mostrar Rutas: " + mostrarRutas + ". Elementos en modelo principal: " + modeloPrincipal.getSize());

         // 4. Limpiar el modelo del diálogo antes de llenarlo
         modeloDialogo.clear();

         // 5. Iterar sobre el modelo principal y añadir elementos al modelo del diálogo
         if (modeloPrincipal.isEmpty()) {
             modeloDialogo.addElement("(La lista principal está vacía)");
         } else {
             for (int i = 0; i < modeloPrincipal.getSize(); i++) {
                 // 5.1. Obtener la clave del modelo principal
                 String claveArchivo = modeloPrincipal.getElementAt(i);
                 if (claveArchivo == null) { // Seguridad extra
                     claveArchivo = "(Clave nula en índice " + i + ")";
                 }

                 // 5.2. Determinar qué texto añadir al diálogo
                 String textoAAgregar = claveArchivo; // Por defecto, la clave

                 if (mostrarRutas) {
                     // Si se deben mostrar rutas completas, obtenerla del mapa
                     Path rutaCompleta = mapaRutas.get(claveArchivo);
                     if (rutaCompleta != null) {
                         // Usar la ruta completa si se encontró
                         textoAAgregar = rutaCompleta.toString();
                     } else {
                         // Si no se encontró la ruta (inconsistencia en datos), indicarlo
                         logger.warn("WARN [Dialogo Lista]: No se encontró ruta para la clave: " + claveArchivo);
                         textoAAgregar = claveArchivo + " (¡Ruta no encontrada!)";
                     }
                 }
                 // Si mostrarRutas es false, textoAAgregar simplemente mantiene la claveArchivo.

                 // 5.3. Añadir el texto determinado al modelo del diálogo
                 modeloDialogo.addElement(textoAAgregar);
                 
             } // Fin del bucle for
         } // Fin else (modeloPrincipal no está vacío)

         // 6. Log final (opcional)
         logger.debug("  [Dialogo Lista] Contenido actualizado. Elementos añadidos al diálogo: " + modeloDialogo.getSize());

         // Nota: No necesitamos repintar la JList del diálogo aquí.
         // El DefaultListModel notifica automáticamente a la JList asociada
         // sobre los cambios (clear y addElement disparan ListDataEvents).

     } // --- FIN actualizarListaEnDialogo ---
     
     
     /**
      * Copia el contenido actual de un DefaultListModel (que se asume contiene
      * Strings, una por línea) al portapapeles del sistema.
      * Cada elemento del modelo se añade como una línea separada en el texto copiado.
      *
      * @param listModel El DefaultListModel<String> cuyo contenido se copiará.
      *                  Típicamente, este será el modelo de la JList del diálogo
      *                  (modeloListaDialogo).
      */
     public void copiarListaAlPortapapeles(DefaultListModel<String> listModel) {
    	 
	     // 1. Validación de entrada
    	 if (listModel == null) {
    		 logger.error("ERROR [copiarListaAlPortapapeles]: El listModel proporcionado es null.");
    		 // Opcional: Mostrar mensaje al usuario si la vista está disponible
	         
	         if (view != null) {
	        	 JOptionPane.showMessageDialog(
	       	            view, // Usar 'view' directamente como el componente padre
	       	            "Error interno al intentar copiar la lista.",
	       	            "Error al Copiar",
	       	            JOptionPane.WARNING_MESSAGE
	       	            );
	         }
	         
	         return;
	     }
	
	     // 2. Construir el String a copiar
	     StringBuilder sb = new StringBuilder();
	     int numeroElementos = listModel.getSize();
	
	     logger.debug("[Portapapeles] Preparando para copiar " + numeroElementos + " elementos...");
	
	     // Iterar sobre todos los elementos del modelo
	     for (int i = 0; i < numeroElementos; i++) {
	    	 String elemento = listModel.getElementAt(i);
	         if (elemento != null) { // Añadir solo si no es null
	        	 sb.append(elemento); // Añadir el texto del elemento
	             
	             // Añadir un salto de línea después de cada elemento, excepto el último
	             if (i < numeroElementos - 1) {
	                 sb.append("\n"); // Usar salto de línea estándar del sistema
	                 // Alternativa: sb.append(System.lineSeparator());
	             }
	         }
	     }
	
	     // 3. Crear el objeto Transferable (StringSelection)
	     //    StringSelection es una implementación de Transferable para texto plano.
	     String textoCompleto = sb.toString();
	     StringSelection stringSelection = new StringSelection(textoCompleto);
	
	     // 4. Obtener el Portapapeles del Sistema
	     Clipboard clipboard = null;
	     try {
	         clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	     } catch (Exception e) {
	    	 logger.error("ERROR [copiarListaAlPortapapeles]: No se pudo acceder al portapapeles del sistema: " + e.getMessage());
	         if (view != null) {
	        	 JOptionPane.showMessageDialog(view,
	                                       "Error al acceder al portapapeles del sistema.",
	                                        "Error al Copiar", 
	                                        JOptionPane.ERROR_MESSAGE);
	         }
	         return; // Salir si no podemos obtener el clipboard
	     }
	
	
	     // 5. Establecer el contenido en el Portapapeles
	     try {
	         // El segundo argumento 'this' indica que nuestra clase VisorController
	         // actuará como "dueño" temporal del contenido (implementa ClipboardOwner).
	         clipboard.setContents(stringSelection, this);
	         logger.debug("[Portapapeles] Lista copiada exitosamente (" + numeroElementos + " líneas).");
	         
	         if (this.statusBarManager != null) {
	        	 statusBarManager.mostrarMensajeTemporal("Lista copiada al portapapeles (" + numeroElementos + " ítems)", 3000); // Muestra por 3 segundos
	         }
	         
	         // Opcional: Mostrar mensaje de éxito
	         if (view != null) {
	              // Podríamos usar un mensaje no modal o una etiqueta temporal
	              // JOptionPane.showMessageDialog(view.getFrame(), "Lista copiada al portapapeles.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
	         }
	 	 } catch (IllegalStateException ise) {
	         // Puede ocurrir si el clipboard no está disponible o está siendo usado
	         logger.error("ERROR [copiarListaAlPortapapeles]: No se pudo establecer el contenido en el portapapeles: " + ise.getMessage());
	         if (view != null) {
	        	 JOptionPane.showMessageDialog(view,
	        			 		"No se pudo copiar la lista al portapapeles.\n" +
	        					 "Puede que otra aplicación lo esté usando.",
	        					 "Error al Copiar", JOptionPane.WARNING_MESSAGE);
	         }
	 	 } catch (Exception e) {
	 		 // Capturar otros errores inesperados
	 		 logger.error("ERROR INESPERADO [copiarListaAlPortapapeles]: " + e.getMessage());
	 		 e.printStackTrace();
	 		 if (view != null) {
	 			 JOptionPane.showMessageDialog(view,
	 					 "Ocurrió un error inesperado al copiar la lista.",
	 					 "Error al Copiar", JOptionPane.ERROR_MESSAGE);
	 		 }
	 	 }
	
     } // --- FIN copiarListaAlPortapapeles ---
     
     
     /**
 	 * Método requerido por la interfaz ClipboardOwner. Se llama cuando otra
 	 * aplicación toma posesión del contenido del portapapeles que esta aplicación
 	 * había puesto previamente.
 	 * 
 	 * En la mayoría de los casos, especialmente cuando solo copiamos texto simple,
 	 * no necesitamos realizar ninguna acción específica cuando perdemos la
 	 * posesión. Dejamos el método implementado pero vacío.
 	 *
 	 * @param clipboard El portapapeles que perdió la posesión.
 	 * @param contents  El contenido Transferable que estaba en el portapapeles.
 	 */
 	@Override
 	public void lostOwnership (Clipboard clipboard, Transferable contents)
 	{
 		// 1. Log (Opcional, útil para depuración o entender el flujo)
 		// logger.debug("[Clipboard] Se perdió la propiedad del contenido del
 		// portapapeles.");

 		// 2. Lógica Adicional (Normalmente no necesaria para copia de texto simple)
 		// - Si estuvieras manejando recursos más complejos o datos que necesitan
 		// liberarse cuando ya no están en el portapapeles, podrías hacerlo aquí.
 		// - Para StringSelection, no hay nada que liberar.

 		// -> Método intencionalmente vacío en este caso. <-

 	} // --- FIN lostOwnership ---     
 	
 	
 	public void limpiarUI() {
        logger.debug("[ViewManager] Limpiando UI y Modelo a estado de bienvenida...");
        
        controlador.VisorController controller = (view != null) ? view.getController() : null;
        if (controller == null) {
            logger.error("ERROR [limpiarUI]: No se pudo obtener la referencia al VisorController desde la vista.");
            return;
        }
        
        controlador.managers.interfaces.IListCoordinator listCoordinator = controller.getListCoordinator();

        if (listCoordinator != null) {
            listCoordinator.setSincronizandoUI(true);
        }

        try {
            // --- 1. LIMPIEZA DEL MODELO Y CACHÉ ---
            if (model != null) {
                model.setCurrentImage(null);
                model.setSelectedImageKey(null);
                model.resetZoomState();
                
                if (controller.getModeloMiniaturasVisualizador() != null) controller.getModeloMiniaturasVisualizador().clear();
                if (controller.getModeloMiniaturasCarrusel() != null) controller.getModeloMiniaturasCarrusel().clear();
                
                logger.debug("  -> Estado de imagen, selección y modelos de lista en 'model' limpiados.");
            }
            
            if (controller.getServicioMiniaturas() != null) {
                controller.getServicioMiniaturas().limpiarCache();
                logger.debug("  -> Caché de miniaturas limpiado.");
            }
            
            // --- 2. ACTUALIZACIÓN VISUAL DE LA PANTALLA DE BIENVENIDA ---
            if (view != null && controller.getIconUtils() != null) {
                view.limpiarImagenMostrada();
                
                ImageDisplayPanel displayPanel = this.getActiveDisplayPanel();
                if (displayPanel != null) {
                    javax.swing.ImageIcon welcomeIcon = controller.getIconUtils().getWelcomeImage("modeltag-bienvenida-apaisado.png");
                    if (welcomeIcon != null) {
                        java.awt.image.BufferedImage welcomeImage = new java.awt.image.BufferedImage(
                            welcomeIcon.getIconWidth(),
                            welcomeIcon.getIconHeight(),
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D g2d = welcomeImage.createGraphics();
                        welcomeIcon.paintIcon(null, g2d, 0, 0);
                        g2d.dispose();
                        displayPanel.setWelcomeImage(welcomeImage);
                        displayPanel.showWelcomeMessage();
                    } else {
                        displayPanel.limpiar();
                    }
                }
            }
            
            // --- 3. OCULTAMIENTO DE PANELES POR ESTADO ---
            if (registry != null) {
                 logger.debug("  -> Ocultando paneles de lista y miniaturas por estado de bienvenida.");
                
                 JPanel panelIzquierdo = registry.get("panel.izquierdo.contenedorPrincipal");
                 if (panelIzquierdo != null) panelIzquierdo.setVisible(false);

                 JScrollPane scrollMiniaturasVisor = registry.get("scroll.miniaturas");
                 if (scrollMiniaturasVisor != null) scrollMiniaturasVisor.setVisible(false);
                 
                 JScrollPane scrollMiniaturasCarousel = registry.get("scroll.miniaturas.carousel");
                 if (scrollMiniaturasCarousel != null) scrollMiniaturasCarousel.setVisible(false);
            }

            // --- 4. PREVENCIÓN DEL NULLPOINTEREXCEPTION ---
            if (controller.getInfobarImageManager() != null) {
                 controller.getInfobarImageManager().actualizar();
            }
            if (statusBarManager != null) {
                statusBarManager.actualizar();
            }
            
            // --- 5. ACTUALIZACIÓN FINAL DE ACCIONES ---
            if (listCoordinator != null) {
                listCoordinator.forzarActualizacionEstadoAcciones();
            }

            if (view != null) {
                logger.debug("  -> Forzando revalidate() y repaint() de la ventana principal para imponer el estado de bienvenida.");
                view.revalidate();
                view.repaint();
            }
            
        } finally {
            if (listCoordinator != null) {
                SwingUtilities.invokeLater(() -> listCoordinator.setSincronizandoUI(false));
            }
        }
        
        logger.debug("[ViewManager] Limpieza de UI y Modelo completada.");
        
    } // Fin del metodo limpiarUI ---
 	
 	
 	public void actualizarTituloVentana() {
 	    controlador.VisorController controller = (view != null) ? view.getController() : null;
 	    if (controller == null || model == null) {
 	        return;
 	    }
 	    
 	    servicios.ProjectManager projectManager = controller.getProjectManager();
 	    if (projectManager == null) {
 	        return;
 	    }
 	    
 	    String tituloBase = "ModelTag - Your visual STL manager";
 	    String tituloFinal;
 	    String prefijoDirty = projectManager.hayCambiosSinGuardar() ? "*" : "";

 	    String nombreProyecto = projectManager.getNombreProyectoActivo();
 	    
 	    if ("Proyecto Temporal".equals(nombreProyecto) && model.getRutaProyectoActivoConNombre() != null) {
 	        nombreProyecto = model.getRutaProyectoActivoConNombre().getFileName().toString();
 	    }

 	    if (!"Proyecto Temporal".equals(nombreProyecto) || !projectManager.getImagenesMarcadas().isEmpty()) {
 	        tituloFinal = prefijoDirty + tituloBase + " - [Proyecto: " + nombreProyecto + "]";
 	    } else {
 	        tituloFinal = tituloBase;
 	    }
 	    
 	    view.setTitle(tituloFinal);
 	    logger.debug("Título de la ventana actualizado a: {}", tituloFinal);
 	    
 	} // ---FIN de metodo actualizarTituloVentana---
    
    
 // ***************************************************************************************************************************
 // ****************************************************************************FIN GESTIÓN DEL DIÁLOGO DE LA LISTA DE IMÁGENES
 // ***************************************************************************************************************************
    
 // ***************************************************************************************************************************
 // ******************************************************************************************************** GESTIÓN DE COLORES
 // ***************************************************************************************************************************
    
    public void sincronizarColoresDePanelesPorTema(Tema temaAFlejar) {
	    if (registry == null || temaAFlejar == null) {
	        logger.warn("WARN [sincronizarColoresDePanelesPorTema]: Dependencias nulas (registry o tema nulo).");
	        return;
	    }
	    
	    logger.debug("  [Sync Colors] Sincronizando colores de paneles para el tema: {}", temaAFlejar.nombreDisplay());

	    // Obtenemos los colores específicos para las barras de estado desde el objeto Tema.
	    Color backgroundColor = temaAFlejar.colorBarraEstadoFondo();
	    Color foregroundColor = temaAFlejar.colorBarraEstadoTexto();

	    // LÓGICA DE FALLBACK: Si el tema actual NO define estos colores específicos, 
	    // usamos los colores genéricos del LookAndFeel como alternativa segura.
	    if (backgroundColor == null) {
	        backgroundColor = UIManager.getColor("Panel.background"); 
	        logger.debug("    -> Usando color de fondo de fallback (UIManager) para la barra de estado.");
	    }
	    if (foregroundColor == null) {
	        foregroundColor = UIManager.getColor("Label.foreground");
	        logger.debug("    -> Usando color de texto de fallback (UIManager) para la barra de estado.");
	    }

	    // --- APLICACIÓN A LOS PANELES DE STATUSBAR ---

	    // 1. Panel de estado inferior (StatusBar de la aplicación)
	    JPanel panelEstadoInferior = registry.get("panel.estado.inferior");
	    if (panelEstadoInferior != null) {
	        panelEstadoInferior.setBackground(backgroundColor);
	        // Usamos el método recursivo para asegurar que todos los JLabels y otros componentes
	        // dentro de este panel (incluso si están anidados) reciban el color de texto correcto.
	        this.actualizarColoresDeTextoRecursivamente(panelEstadoInferior, foregroundColor);
	        logger.debug("    -> Colores personalizados aplicados a 'panel.estado.inferior'.");
	    }
	    
	    // 2. Panel de información superior (StatusBar de la imagen)
	    JPanel panelInfoSuperior = registry.get("panel.info.superior");
	    if (panelInfoSuperior != null) {
	        panelInfoSuperior.setBackground(backgroundColor);
	        // Hacemos lo mismo para la barra superior.
	        this.actualizarColoresDeTextoRecursivamente(panelInfoSuperior, foregroundColor);
	        logger.debug("    -> Colores personalizados aplicados a 'panel.info.superior'.");
	    }

	} // --- Fin del método sincronizarColoresDePanelesPorTema ---
    
    
	/**
	 * Sincroniza los colores de los paneles usando el tema actualmente activo en el ThemeManager.
	 * Este es un método de conveniencia para la inicialización y otros refrescos generales
	 * que no tienen un 'nuevoTema' a mano.
	 */
	public void sincronizarColoresDePanelesPorTema() {
	    if (themeManager != null) {
	        // Llama a la versión detallada pasando el tema que ya está activo.
	        sincronizarColoresDePanelesPorTema(themeManager.getTemaActual());
	    } else {
	        logger.error("ERROR [sincronizarColoresDePanelesPorTema]: ThemeManager es nulo. No se puede sincronizar colores.");
	    }
	} // ---FIN de metodo [sincronizarColoresDePanelesPorTema]---
	
	
	/**
     * Recorre un componente contenedor y todos sus hijos (y los hijos de sus hijos, etc.)
     * para aplicarles un color de texto (foreground) específico.
     * Este método es crucial para asegurar que todos los elementos dentro de paneles
     * con colores de fondo personalizados (como la barra de estado) hereden el color
     * de texto correcto para mantener la legibilidad.
     *
     * @param container El componente raíz desde el que empezar a aplicar colores.
     * @param color El color de texto a aplicar.
     */
    private void actualizarColoresDeTextoRecursivamente(java.awt.Container container, Color color) {
        // Itera sobre todos los componentes directos del contenedor.
        for (java.awt.Component component : container.getComponents()) {
            // Aplica el color de texto al componente actual.
            component.setForeground(color);
            
            // Si el componente es a su vez un contenedor (como un JToolBar o un JPanel anidado),
            // se llama a este mismo método de forma recursiva para que actualice a sus hijos.
            if (component instanceof java.awt.Container) {
                actualizarColoresDeTextoRecursivamente((java.awt.Container) component, color);
            }
        }
    } // --- fin del método actualizarColoresDeTextoRecursivamente ---     
    
    
//  FIXME (Opcionalmente, podría estar en una clase de Utilidades si se usa en más sitios)

	/**
	* Convierte una cadena de texto que representa un color en formato "R, G, B"
	* (donde R, G, B son números enteros entre 0 y 255) en un objeto java.awt.Color.
	*
	* Ignora espacios alrededor de los números y las comas.
	* Valida que los componentes numéricos estén en el rango [0, 255].
	*
	* @param rgbString La cadena de texto a parsear (ej. "238, 238, 238", " 0, 0,0 ").
	*                  Si es null, vacía o tiene un formato incorrecto, se devolverá
	*                  un color por defecto (gris claro).
	* @return El objeto Color correspondiente a la cadena RGB, o Color.LIGHT_GRAY
	*         si la cadena no se pudo parsear correctamente.
	*/
    private Color parseColor(String rgbString) {
    	// 1. Manejar entrada nula o vacía
    	if (rgbString == null || rgbString.trim().isEmpty()) {
    		logger.warn("WARN [parseColor]: Cadena RGB nula o vacía. Usando color por defecto (Gris Claro).");
    		return Color.LIGHT_GRAY; // Color por defecto seguro
    	}

    	// 2. Separar la cadena por las comas
    	String[] components = rgbString.split(",");

    	// 3. Validar que tengamos exactamente 3 componentes
    	if (components.length == 3) {
    		try {
    			// 3.1. Parsear cada componente a entero, quitando espacios (trim)
    			int r = Integer.parseInt(components[0].trim());
    			int g = Integer.parseInt(components[1].trim());
    			int b = Integer.parseInt(components[2].trim());

    			// 3.2. Validar el rango [0, 255] para cada componente
    			//      Usamos Math.max/min para asegurar que el valor quede dentro del rango.
    			r = Math.max(0, Math.min(255, r));
    			g = Math.max(0, Math.min(255, g));
    			b = Math.max(0, Math.min(255, b));

    			// 3.3. Crear y devolver el objeto Color
    			return new Color(r, g, b);

    		} catch (NumberFormatException e) {
    			// Error si alguno de los componentes no es un número entero válido
    			logger.warn("WARN [parseColor]: Formato numérico inválido en '" + rgbString + "'. Usando color por defecto (Gris Claro). Error: " + e.getMessage());
    			return Color.LIGHT_GRAY; // Devolver color por defecto
    		} catch (Exception e) {
    			// Capturar otros posibles errores inesperados durante el parseo
    			logger.error("ERROR INESPERADO [parseColor] parseando '" + rgbString + "': " + e.getMessage());
    			e.printStackTrace();
    			return Color.LIGHT_GRAY; // Devolver color por defecto
    		}
    		
    	} else {
    		// Error si no se encontraron exactamente 3 componentes después de split(',')
    		logger.warn("WARN [parseColor]: Formato de color debe ser R,G,B. Recibido: '" + rgbString + "'. Usando color por defecto (Gris Claro).");
    		return Color.LIGHT_GRAY; // Devolver color por defecto
     	}
    	
 	} // --- FIN parseColor ---
    
    
 // ***************************************************************************************************************************
 // ************************************************************************************************* FIN DE GESTIÓN DE COLORES
 // ***************************************************************************************************************************
           
    
    
 // ***************************************************************************************************************************  
 // ********************************************************************************************************* GETTERS Y SETTERS    
 // ***************************************************************************************************************************
    
    /**
     * Devuelve la instancia del ImageDisplayPanel que está actualmente activa y visible
     * basándose en el WorkMode actual del modelo. Este método centraliza la lógica
     * para determinar qué panel de visualización debe usarse para operaciones como
     * zoom y paneo.
     *
     * @return El ImageDisplayPanel activo, o null si no se encuentra o las dependencias no están listas.
     */
    @Override
    public ImageDisplayPanel getActiveDisplayPanel() {
        // 1. Validar dependencias (necesitamos el modelo y el registro)
        if (model == null || registry == null) {
            logger.error("ERROR [ViewManager.getActiveDisplayPanel]: Modelo o Registry son nulos.");
            return null;
        }

        // 2. Determinar la clave del panel basándose en el modo de trabajo actual
        String panelKey;
        switch (model.getCurrentWorkMode()) {
            case VISUALIZADOR:
                panelKey = "panel.display.imagen";
                break;
            case PROYECTO:
                panelKey = "panel.proyecto.display";
                break;
            case CARROUSEL:
                panelKey = "panel.display.carousel";
                break;
            case DATOS:
            case EDICION:
                logger.warn("WARN [ViewManager.getActiveDisplayPanel]: El modo " + model.getCurrentWorkMode() + " no tiene un ImageDisplayPanel asociado.");
                return null;
            default:
                logger.error("ERROR [ViewManager.getActiveDisplayPanel]: WorkMode no reconocido: " + model.getCurrentWorkMode());
                return null;
        }

        // 3. Obtener y devolver el panel desde el registro usando la clave determinada.
        return registry.get(panelKey);
        
    } // --- FIN del método getActiveDisplayPanel ---
    
    
    public void setToolbarManager(ToolbarManager toolbarManager) {
        this.toolbarManager = toolbarManager;
    }
    
    public void setViewBuilder(ViewBuilder viewBuilder) {
        this.viewBuilder = viewBuilder;
    }
    
    @Override
    public void setModel(VisorModel model) {
        this.model = model;
    }
    
    
    public void setDisplayModeManager(DisplayModeManager displayModeManager) { this.displayModeManager = displayModeManager;}
    public DisplayModeManager getDisplayModeManager() {return this.displayModeManager;}
    public void setStatusBarManager(InfobarStatusManager statusBarManager) {this.statusBarManager = statusBarManager;}
    
// ***************************************************************************************************** FIN GETTERS Y SETTERS
// ***************************************************************************************************************************      
    
// *********************************************************************************************************************************    
// *********************************************************************************************************************************
// *********************************************************************************************************************************
    
    /**
     * Una clase de borde "inteligente" que no almacena un color, sino la clave de UIManager
     * para buscar el color. Cada vez que se pinta, obtiene el color más reciente del tema,
     * solucionando así problemas de timing durante la inicialización.
     */
    private static class DynamicAccentBorder extends javax.swing.border.AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final int thickness;
        private final String colorKey;

        public DynamicAccentBorder(String colorKey, int thickness) {
            this.colorKey = colorKey;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, java.awt.Graphics g, int x, int y, int width, int height) {
            Color accentColor = UIManager.getColor(colorKey);
            if (accentColor == null) {
                accentColor = Color.MAGENTA; // Fallback de error visible
            }
            g.setColor(accentColor);
            for (int i = 0; i < thickness; i++) {
                g.drawRect(x + i, y + i, width - 1 - (i * 2), height - 1 - (i * 2));
            }
        }

        @Override
        public java.awt.Insets getBorderInsets(Component c, java.awt.Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = thickness;
            return insets;
        }

        @Override
        public java.awt.Insets getBorderInsets(Component c) {
            return new java.awt.Insets(thickness, thickness, thickness, thickness);
        }
    } // --- FIN de la clase DynamicAccentBorder ---
    
} // --- Fin de la clase ViewManager ---

