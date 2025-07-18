package controlador.managers;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GraphicsDevice;
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

import controlador.actions.config.SetInfoBarTextFormatAction;
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IViewManager;
import controlador.utils.ComponentRegistry;
import servicios.ConfigurationManager;
import vista.VisorView;
import vista.panels.ImageDisplayPanel;
import vista.theme.Tema;
import vista.theme.ThemeManager;

public class ViewManager implements IViewManager {

    private VisorView view;
    private ConfigurationManager configuration;
    private ComponentRegistry registry;
    private ThemeManager themeManager;
    private Map<String, Action> actionMap;
    private Map<String, AbstractButton> botonesPorNombre;
    
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
            System.err.println("ERROR [ViewManager.setFullScreen]: La vista (JFrame) es nula.");
            return;
        }

        GraphicsDevice device = view.getGraphicsConfiguration().getDevice();
        
        System.out.println("  [ViewManager] Ejecutando cambio a pantalla completa. Estado solicitado: " + (fullScreenState ? "ACTIVADO" : "DESACTIVADO"));

        // Comprobar si el cambio es realmente necesario para evitar trabajo extra.
        // device.getFullScreenWindow() devuelve la ventana actual en pantalla completa, o null si no hay ninguna.
        boolean isCurrentlyFullScreen = (device.getFullScreenWindow() == view);
        if (isCurrentlyFullScreen == fullScreenState) {
            System.out.println("  -> La ventana ya está en el estado solicitado. No se realiza ninguna acción.");
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
        System.out.println("[ViewManager] setComponentePrincipalVisible: " + identificadorComponente + " -> " + nuevoEstadoVisible);

        if (view == null || configuration == null) {
            System.err.println("ERROR [ViewManager]: Vista o Configuración nulas.");
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
                    System.err.println("WARN [ViewManager]: 'scroll.miniaturas' no encontrado en el registro.");
                }
                break;
            default:
                System.err.println("WARN [ViewManager]: Identificador de componente no manejado: '" + identificadorComponente + "'");
                return;
        }

        if (configKeyParaEstado != null && !configKeyParaEstado.isBlank()) {
            configuration.setString(configKeyParaEstado, String.valueOf(nuevoEstadoVisible));
            System.out.println("  -> [ViewManager] Configuración '" + configKeyParaEstado + "' actualizada a: " + nuevoEstadoVisible);
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
            System.err.println("ERROR [ViewManager]: Registry o ConfigurationManager nulos.");
            return;
        }

        String configKey = "interfaz.menu.vista.fondo_a_cuadros.seleccionado";
        configuration.setString(configKey, String.valueOf(activar));
        
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel != null) {
            displayPanel.setCheckeredBackground(activar);
        } else {
            System.err.println("ERROR [ViewManager]: No se pudo encontrar 'panel.display.imagen' en el registro.");
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
            System.err.println("ERROR [ViewManager]: No se puede abrir JColorChooser, falta 'frame.main' o 'panel.display.imagen' en el registro.");
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
     * @param uiElementId El identificador del componente o zona a actualizar.
     * @param configKey La clave de configuración que cambió (puede ser null).
     * @param nuevoEstado El nuevo estado de visibilidad (true para visible, false para oculto).
     */
    @Override
    public void solicitarActualizacionUI(String uiElementId, String configKey, boolean nuevoEstado) {
        System.out.println("[ViewManager] Solicitud de actualización para UI: '" + uiElementId + "' -> " + nuevoEstado);
        
        if (registry == null) {
            System.err.println("  ERROR: ComponentRegistry es nulo en ViewManager.");
            return;
        }
        
        boolean necesitaRevalidateGeneral = false;
        JFrame mainFrame = registry.get("frame.main");
        if (mainFrame == null) {
            System.err.println("  ERROR: El frame principal no está en el registro.");
            return;
        }

        switch (uiElementId) {
            case "Barra_de_Menu":
                JMenuBar menuBar = mainFrame.getJMenuBar();
                if (menuBar != null && menuBar.isVisible() != nuevoEstado) {
                    menuBar.setVisible(nuevoEstado);
                    necesitaRevalidateGeneral = true;
                }
                
                boolean visibilidadBotonEspecial = !nuevoEstado;
                setBotonMenuEspecialVisible(visibilidadBotonEspecial);
                
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
                Component comp = registry.get(uiElementId);
                if (comp instanceof JToolBar) {
                    JToolBar toolbar = (JToolBar) comp;
                    if (toolbar.isVisible() != nuevoEstado) {
                        toolbar.setVisible(nuevoEstado);
                        revalidateToolbarContainer();
                    }
                } else {
                     System.err.println("  WARN [ViewManager]: uiElementId no reconocido o no manejado: '" + uiElementId + "'");
                }
                break;
        }

        if (necesitaRevalidateGeneral) {
            SwingUtilities.invokeLater(() -> {
                mainFrame.revalidate();
                mainFrame.repaint();
            });
        }
    } // --- Fin del método solicitarActualizacionUI ---

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
            System.err.println("WARN [ViewManager]: 'container.toolbars' no encontrado.");
        }
    } // --- Fin del método revalidateToolbarContainer ---

    /**
     * Orquesta un refresco completo de la apariencia de la UI aplicando el tema actual.
     */
    @Override
    public void ejecutarRefrescoCompletoUI() {
        System.out.println("\n--- [ViewManager] Ejecutando Refresco Completo de la UI ---");
        System.out.println("  -> Lógica de refresco de tema a implementar aquí.");
    } // --- Fin del método ejecutarRefrescoCompletoUI ---

    /**
     * Restablece el fondo del visor a su estado POR DEFECTO, según lo define la configuración.
     */
    @Override
    public void refrescarFondoAlPorDefecto() {
        System.out.println("[ViewManager] Refrescando fondo al estado por defecto...");

        if (registry == null || configuration == null || themeManager == null) {
            System.err.println("  ERROR: Dependencias nulas (registry, config o themeManager).");
            return;
        }

        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel == null) {
            System.err.println("  ERROR: 'panel.display.imagen' no encontrado en el registro.");
            return;
        }

        String configKey = "interfaz.menu.vista.fondo_a_cuadros.seleccionado";
        boolean esCuadrosPorDefecto = configuration.getBoolean(configKey, false);

        if (esCuadrosPorDefecto) {
            System.out.println("  -> El defecto es fondo a cuadros. Aplicando.");
            displayPanel.setCheckeredBackground(true);
        } else {
            Tema temaActual = themeManager.getTemaActual();
            System.out.println("  -> El defecto es color de tema. Aplicando color: " + temaActual.colorFondoSecundario());
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
        System.out.println("[ViewManager] Sincronizando Actions de formato...");
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
    
    /**
     * **CORRECCIÓN CLAVE:** Cambia la vista activa en un contenedor de CardLayout específico.
     * @param containerRegistryKey La clave en el ComponentRegistry del JPanel que usa CardLayout (ej. "container.vistas", "container.displaymodes").
     * @param viewName La clave de la vista a mostrar (el nombre de la "tarjeta" en el CardLayout).
     */
    @Override // <--- Asegúrate de que esta anotación esté presente
    public void cambiarAVista(String containerRegistryKey, String viewName) { // <--- MODIFICADO
        if (registry == null) {
            System.err.println("ERROR [ViewManager]: Registry es nulo, no se puede cambiar de vista.");
            return;
        }
        
        JPanel container = registry.get(containerRegistryKey); // Obtener el contenedor específico
        
        if (container != null && container.getLayout() instanceof CardLayout) {
            CardLayout cl = (CardLayout) container.getLayout();
            cl.show(container, viewName);
            System.out.println("[ViewManager] Vista cambiada en '" + containerRegistryKey + "' a: " + viewName);
        } else {
            // Este es el error que estás viendo: "No se encontró 'container.vistas' o no usa CardLayout."
            System.err.println("ERROR [ViewManager]: No se encontró el contenedor '" + containerRegistryKey + "' o no usa CardLayout.");
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
    
} // --- Fin de la clase ViewManager ---

