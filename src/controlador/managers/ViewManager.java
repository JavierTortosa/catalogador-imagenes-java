package controlador.managers;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
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

public class ViewManager implements IViewManager, ThemeChangeListener {

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
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
     * Cambia la visibilidad de un componente principal de la UI y actualiza la configuración.
     *
     * @param identificadorComponente String que identifica el componente en la VisorView.
     * @param nuevoEstadoVisible El nuevo estado de visibilidad.
     * @param configKeyParaEstado La clave en ConfigurationManager para guardar este estado.
     */
    @Override
    public void setComponentePrincipalVisible(String identificadorComponente, boolean nuevoEstadoVisible, String configKeyParaEstado) {
        logger.debug("[ViewManager] setComponentePrincipalVisible: " + identificadorComponente + " -> " + nuevoEstadoVisible);

        if (view == null || configuration == null) {
            logger.error("ERROR [ViewManager]: Vista o Configuración nulas.");
            return;
        }

        boolean cambioRealizadoEnVista = false;

        switch (identificadorComponente) {
            case "Barra_de_Menu":
                if (view.getJMenuBar() != null && view.getJMenuBar().isVisible() != nuevoEstadoVisible) {
                    view.setJMenuBarVisible(nuevoEstadoVisible); // Llama al método existente en VisorView
                    cambioRealizadoEnVista = true;
                }
                break;
            case "Barra_de_Botones":
                if (view.getPanelDeBotones() != null && view.getPanelDeBotones().isVisible() != nuevoEstadoVisible) {
                    view.setToolBarVisible(nuevoEstadoVisible); // Llama al método existente en VisorView
                    cambioRealizadoEnVista = true;
                }
                break;
            case "mostrar_ocultar_la_lista_de_archivos":
                JPanel panelIzquierdo = this.registry.get("panel.izquierdo.listaArchivos");
                JSplitPane splitPane = this.registry.get("splitpane.main");

                if (panelIzquierdo != null && splitPane != null) {
                    if (panelIzquierdo.isVisible() != nuevoEstadoVisible) {
                        panelIzquierdo.setVisible(nuevoEstadoVisible);
                        if (nuevoEstadoVisible) {
                            SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.25));
                        }
                        cambioRealizadoEnVista = true;
                    }
                }
                break;
            case "imagenes_en_miniatura":
                JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");

                if (scrollMiniaturas != null) {
                    if (scrollMiniaturas.isVisible() != nuevoEstadoVisible) {
                        scrollMiniaturas.setVisible(nuevoEstadoVisible);
                        cambioRealizadoEnVista = true;
                    }
                } else {
                    logger.warn("WARN [ViewManager]: 'scroll.miniaturas' no encontrado en el registro.");
                }
                break;
            default:
                logger.warn("WARN [ViewManager]: Identificador de componente no manejado: '" + identificadorComponente + "'");
                return;
        }

        if (configKeyParaEstado != null && !configKeyParaEstado.isBlank()) {
            configuration.setString(configKeyParaEstado, String.valueOf(nuevoEstadoVisible));
            logger.debug("  -> [ViewManager] Configuración '" + configKeyParaEstado + "' actualizada a: " + nuevoEstadoVisible);
        }
    } // --- Fin del método setComponentePrincipalVisible ---
    
    
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
                JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");
                if (panelIzquierdo != null && panelIzquierdo.isVisible() != nuevoEstado) {
                    panelIzquierdo.setVisible(nuevoEstado);
                    ajustarDivisorSplitPane(nuevoEstado);
                    necesitaRevalidateGeneral = true;
                }
                break;

            case "imagenes_en_miniatura":
                JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");
                if (scrollMiniaturas != null && scrollMiniaturas.isVisible() != nuevoEstado) {
                    scrollMiniaturas.setVisible(nuevoEstado);
                    necesitaRevalidateGeneral = true;
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
        logger.debug("[ViewManager] Notificación de cambio de tema recibida. Actualizando paneles de visualización y UI...");
        
        // Es crucial ejecutar las actualizaciones de UI en el Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            
            // 1. Actualizar los paneles de visualización de imagen
            //    Usamos las claves del registro para obtener cada panel y llamar a su método de actualización.
            
            ImageDisplayPanel panelVisor = registry.get("panel.display.imagen");
            if (panelVisor != null) {
                panelVisor.actualizarColorDeFondoPorTema(this.themeManager);
            }

            ImageDisplayPanel panelProyecto = registry.get("panel.proyecto.display");
            if (panelProyecto != null) {
                panelProyecto.actualizarColorDeFondoPorTema(this.themeManager);
            }

            ImageDisplayPanel panelCarrusel = registry.get("panel.display.carousel");
            if (panelCarrusel != null) {
                panelCarrusel.actualizarColorDeFondoPorTema(this.themeManager);
            }

            // 1. Actualizar el panel contenedor (esto ya lo hacías y está bien)
            refrescarColoresDeFondoUI();

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

        // 1. Obtener los colores del tema actual
        Color colorFondoPrincipal = themeManager.getTemaActual().colorFondoPrincipal();
        Color colorFondoSecundario = themeManager.getTemaActual().colorFondoSecundario();
        
        // 2. Lista de paneles estructurales clave
        List<String> panelesAActualizar = List.of(
            "panel.north.wrapper",
            "container.toolbars",
            "container.toolbars.left",
            "container.toolbars.center",
            "container.toolbars.right",
            "panel.info.superior",
            "panel.estado.inferior"
        );

        for (String key : panelesAActualizar) {
            JPanel panel = registry.get(key);
            if (panel != null) {
                panel.setBackground(colorFondoPrincipal);
            }
        }
        
        // 3. Actualizar explícitamente los viewports de los JScrollPane
        JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas.carousel");
        if (scrollMiniaturas != null) {
            scrollMiniaturas.getViewport().setBackground(colorFondoSecundario);
        }
        scrollMiniaturas = registry.get("scroll.miniaturas");
        if (scrollMiniaturas != null) {
            scrollMiniaturas.getViewport().setBackground(colorFondoSecundario);
        }
        
        // 4. Actualizar las JToolBars individuales
        if (toolbarManager != null) {
            for (JToolBar tb : toolbarManager.getManagedToolbars().values()) {
                tb.setBackground(colorFondoPrincipal);
            }
        }
        
        // 5. Repintar el frame principal
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
    
    
    /**
     * Método helper REFORZADO para reconstruir y recolocar una barra de herramientas "libre".
     * @param claveBarra La clave de la barra en el UIDefinitionService.
     * @param clavePanelContenedor La clave del panel contenedor en el ComponentRegistry.
     * @param layoutConstraint La restricción del BorderLayout (e.g., BorderLayout.NORTH).
     */
    private void reconstruirBarraLibre(String claveBarra, String clavePanelContenedor, String layoutConstraint) {
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
    
} // --- Fin de la clase ViewManager ---

