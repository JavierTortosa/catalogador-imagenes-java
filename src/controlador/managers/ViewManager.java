package controlador.managers;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import controlador.actions.config.SetInfoBarTextFormatAction;
import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import servicios.ConfigurationManager;
import vista.VisorView;
import vista.panels.ImageDisplayPanel;
import vista.theme.Tema;
import vista.theme.ThemeManager;

public class ViewManager {

    private VisorView view;
    private ConfigurationManager configuration;
    private final ComponentRegistry registry;
    private final ConfigurationManager config;
    private ThemeManager themeManager;
    private Map<String, Action> actionMap;
    private Map<String, JButton> botonesPorNombre;
    
    /**
     * Constructor refactorizado de ViewManager.
     * @param view La instancia de VisorView (aún puede ser necesaria para obtener el frame principal).
     * @param config El gestor de configuración.
     * @param registry El registro central de componentes de la UI.
     */
    // <<< PASO 2: MODIFICAR EL CONSTRUCTOR >>>
    public ViewManager(VisorView view, ConfigurationManager config, ComponentRegistry registry) {
        // this.view = view;
        this.config = config;
        this.registry = registry; // Guardar la referencia
    }

    /**
     * Cambia la visibilidad de un componente principal de la UI y actualiza la configuración.
     *
     * @param identificadorComponente String que identifica el componente en la VisorView.
     * @param nuevoEstadoVisible El nuevo estado de visibilidad.
     * @param configKeyParaEstado La clave en ConfigurationManager para guardar este estado.
     */
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
                // <<< AHORA ESTE CÓDIGO FUNCIONARÁ >>>
                // Porque `this.registry` ya existe como campo de la clase.
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
                // 1. Obtener el JScrollPane desde el registro
                JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");

                if (scrollMiniaturas != null) {
                    // 2. Comprobar si el estado actual es diferente al deseado
                    if (scrollMiniaturas.isVisible() != nuevoEstadoVisible) {
                        // 3. Aplicar directamente la visibilidad
                        scrollMiniaturas.setVisible(nuevoEstadoVisible);
                        
                        // 4. Marcar que se hizo un cambio para el revalidate/repaint final
                        cambioRealizadoEnVista = true;
                    }
                } else {
                    System.err.println("WARN [ViewManager]: 'scroll.miniaturas' no encontrado en el registro.");
                }
                break;
//            case "linea_de_ubicacion_del_archivo":
//                if (view.getTextoRuta() != null && view.getTextoRuta().isVisible() != nuevoEstadoVisible) {
//                    view.setLocationBarVisible(nuevoEstadoVisible); // Llama al método existente en VisorView
//                    cambioRealizadoEnVista = true;
//                }
//                break;
            // Los casos "fondo_a_cuadros" y "mantener_ventana_siempre_encima"
            // son manejados por sus Actions específicas que llaman a métodos directos en VisorView.
            // No necesitan pasar por este método genérico si su lógica es diferente.
            default:
                System.err.println("WARN [ViewManager]: Identificador de componente no manejado: '" + identificadorComponente + "'");
                return;
        }

        // Actualizar configuración en memoria
        if (configKeyParaEstado != null && !configKeyParaEstado.isBlank()) {
            configuration.setString(configKeyParaEstado, String.valueOf(nuevoEstadoVisible));
            System.out.println("  -> [ViewManager] Configuración '" + configKeyParaEstado + "' actualizada a: " + nuevoEstadoVisible);
        }

        // VisorView (en sus métodos setJMenuBarVisible, setToolBarVisible, etc.)
        // ya debería encargarse de revalidate/repaint. Si no, tendrías que hacerlo aquí
        // o asegurar que el cambio de visibilidad del componente lo dispare.
        // Si el cambio se hizo y el método en VisorView no hizo revalidate/repaint:
        // if (cambioRealizadoEnVista) {
        //     SwingUtilities.invokeLater(() -> {
        //         view.getFrame().revalidate();
        //         view.getFrame().repaint();
        //     });
        // }
    }
    
    
    /**
     * Activa o desactiva el fondo a cuadros como opción por defecto.
     * Esta acción es persistente y es llamada por la Action del menú.
     * @param activar true para activar el fondo a cuadros, false para desactivarlo.
     */
    public void setCheckeredBackgroundEnabled(boolean activar) {
        if (registry == null || config == null) {
            System.err.println("ERROR [ViewManager]: Registry o ConfigurationManager nulos.");
            return;
        }

        // 1. Guardar el estado en la configuración
        String configKey = "interfaz.menu.vista.fondo_a_cuadros.seleccionado";
        config.setString(configKey, String.valueOf(activar));
        
        // 2. Aplicar el cambio visual llamando al método del panel ejecutor
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
    public void requestCustomBackgroundColor() {
        if (registry == null) return;

        // Necesitamos un componente padre para el diálogo, lo obtenemos del registro.
        // Asumo que el JFrame principal está registrado con la clave "frame.main".
        javax.swing.JFrame mainFrame = registry.get("frame.main"); 
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");

        if (displayPanel == null || mainFrame == null) {
            System.err.println("ERROR [ViewManager]: No se puede abrir JColorChooser, falta 'frame.main' o 'panel.display.imagen' en el registro.");
            return;
        }
        
        // El color actual se puede obtener del propio panel, pero getBackground() podría
        // no ser fiable si el panel es opaco. Es mejor tener un getter para el color sólido.
        // Por ahora, asumimos que getBackground() funciona.
        java.awt.Color colorActual = displayPanel.getBackground();
        java.awt.Color colorElegido = javax.swing.JColorChooser.showDialog(mainFrame, "Seleccionar Color de Fondo", colorActual);
        
        if (colorElegido != null) {
            setSessionBackgroundColor(colorElegido);
        }
    } // --- Fin del método requestCustomBackgroundColor ---

    
    /**
     * MÉTODO MIGRADO Y CENTRALIZADO: Punto de entrada para solicitar actualizaciones
     * de visibilidad de componentes de la UI. Es llamado por las Actions.
     *
     * @param uiElementId El identificador del componente o zona a actualizar.
     * @param configKey La clave de configuración que cambió (puede ser null).
     * @param nuevoEstado El nuevo estado de visibilidad (true para visible, false para oculto).
     */
    public void solicitarActualizacionUI(String uiElementId, String configKey, boolean nuevoEstado) {
        System.out.println("[ViewManager] Solicitud de actualización para UI: '" + uiElementId + "' -> " + nuevoEstado);
        
        if (registry == null) {
            System.err.println("  ERROR: ComponentRegistry es nulo en ViewManager.");
            return;
        }
        
        boolean necesitaRevalidateGeneral = false;
        JFrame mainFrame = (JFrame) registry.get("frame.main");
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
            
            case "Barra_de_Botones": // Maneja el contenedor principal de toolbars
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
                // Caso para manejar toolbars individuales por su nombre/ID
                Component comp = registry.get(uiElementId);
                if (comp instanceof JToolBar) {
                    JToolBar toolbar = (JToolBar) comp;
                    if (toolbar.isVisible() != nuevoEstado) {
                        toolbar.setVisible(nuevoEstado);
                        // Revalidar solo el contenedor de toolbars es más eficiente
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
    } // --- FIN del metodo solicitarActualizacionUI ---

    /**
     * MÉTODO MIGRADO: Helper para revalidar el contenedor de las barras de herramientas.
     * Se mantiene público por si se necesita desde fuera, pero su uso principal es interno.
     */
    public void revalidateToolbarContainer() {
        JPanel toolbarContainer = registry.get("container.toolbars");
        if (toolbarContainer != null) {
            toolbarContainer.revalidate();
            toolbarContainer.repaint();
        } else {
            System.err.println("WARN [ViewManager]: 'container.toolbars' no encontrado.");
        }
    }

    /**
     * MÉTODO MIGRADO: Orquesta un refresco completo de la apariencia de la UI aplicando el tema actual.
     */
    public void ejecutarRefrescoCompletoUI() {
        System.out.println("\n--- [ViewManager] Ejecutando Refresco Completo de la UI ---");
        // Este método necesitará una referencia al ThemeManager, que ya tiene.
        // También necesitará acceso a los componentes, que obtiene del registry.
        // La implementación exacta dependerá de cómo tu ThemeApplier funcione.
        // Asumiendo un `ThemeApplier` que toma el registro y el tema:
        
        // ThemeApplier themeApplier = new ThemeApplier(registry);
        // Tema temaActual = themeManager.getTemaActual();
        // SwingUtilities.invokeLater(() -> {
        //     themeApplier.applyTheme(temaActual);
        //     JFrame frame = registry.get("frame.main");
        //     if(frame != null) {
        //        frame.revalidate();
        //        frame.repaint();
        //     }
        // });
        
        // Por ahora, lo dejamos como un placeholder si no tienes el ThemeApplier
        System.out.println("  -> Lógica de refresco de tema a implementar aquí.");
    }

    /**
     * MÉTODO MIGRADO: Restablece el fondo del visor a su estado POR DEFECTO, 
     * según lo define la configuración.
     */
    public void refrescarFondoAlPorDefecto() {
        System.out.println("[ViewManager] Refrescando fondo al estado por defecto...");

        if (registry == null || config == null || themeManager == null) {
            System.err.println("  ERROR: Dependencias nulas (registry, config o themeManager).");
            return;
        }

        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel == null) {
            System.err.println("  ERROR: 'panel.display.imagen' no encontrado en el registro.");
            return;
        }

        // 1. Leer el estado por defecto desde la configuración.
        String configKey = "interfaz.menu.vista.fondo_a_cuadros.seleccionado";
        boolean esCuadrosPorDefecto = config.getBoolean(configKey, false);

        // 2. Aplicar el estado al panel de visualización.
        if (esCuadrosPorDefecto) {
            System.out.println("  -> El defecto es fondo a cuadros. Aplicando.");
            displayPanel.setCheckeredBackground(true);
        } else {
            // Si no, el defecto es el color de fondo secundario del tema actual.
            Tema temaActual = themeManager.getTemaActual();
            System.out.println("  -> El defecto es color de tema. Aplicando color: " + temaActual.colorFondoSecundario());
            displayPanel.setSolidBackgroundColor(temaActual.colorFondoSecundario());
        }
    } // --- FIN del metodo refrescarFondoAlPorDefecto ---

    
    // --- Métodos Helper Privados para la nueva lógica ---
    
    private boolean actualizarComponenteIndividual(String configKey, boolean nuevoEstado) {
        // Intenta encontrar el componente en los mapas de la vista (que deberían estar en el registry o pasados)
        // Esta parte es compleja sin ver cómo se registran los botones.
        // Asumiremos que el componente se busca y se actualiza aquí.
        // Por ahora, retornamos false para que el switch principal se ejecute.
        // TODO: Implementar la búsqueda del botón/menú por configKey y actualizar su visibilidad.
        return false;
    }
    
    private void ajustarDivisorSplitPane(boolean panelVisible) {
        JSplitPane splitPane = registry.get("splitpane.main");
        if (splitPane != null) {
            if (panelVisible) {
                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.25));
            } else {
                // No es necesario, al ocultar el componente, el split pane se ajusta solo.
                // splitPane.resetToPreferredSizes();
            }
        }
    }
    
    /**
     * MÉTODO MOVIDO: Sincroniza las Actions de formato para la barra superior.
     */
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
    }

    /**
     * MÉTODO MOVIDO: Sincroniza las Actions de formato para la barra inferior.
     */
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
    }
    
    /**
     * MÉTODO MOVIDO: Sincroniza el estado visual inicial de todos los radios de formato.
     */
    public void sincronizarEstadoVisualInicialDeRadiosDeFormato() {
        System.out.println("[ViewManager] Sincronizando Actions de formato...");
        sincronizarAccionesFormatoBarraSuperior();
        sincronizarAccionesFormatoBarraInferior();
    }
    
    public void setBotonMenuEspecialVisible(boolean visible) {
        if (this.botonesPorNombre == null) return;
        
        JButton boton = this.botonesPorNombre.get("interfaz.boton.especiales.Menu_48x48");

        if (boton != null) {
            if (boton.isVisible() != visible) {
                boton.setVisible(visible);
                revalidateToolbarContainer(); // Revalidamos para que el cambio se vea
            }
        }
    }
    
    /**
     * Cambia la vista activa en el contenedor principal de vistas.
     * @param nombreVista El identificador de la vista a mostrar (ej. "VISTA_VISUALIZADOR").
     */
    public void cambiarAVista(String nombreVista) {
        if (registry == null) {
            System.err.println("ERROR [ViewManager]: Registry es nulo, no se puede cambiar de vista.");
            return;
        }
        
        // Obtenemos el panel que usa CardLayout desde el registro
        JPanel vistasContainer = registry.get("container.vistas");
        
        if (vistasContainer != null && vistasContainer.getLayout() instanceof CardLayout) {
            CardLayout cl = (CardLayout) vistasContainer.getLayout();
            
            // Le decimos al CardLayout que muestre la "tarjeta" con el nombre que nos han pasado
            cl.show(vistasContainer, nombreVista);
            
            System.out.println("[ViewManager] Vista cambiada a: " + nombreVista);
        } else {
            System.err.println("ERROR [ViewManager]: No se encontró 'container.vistas' o no usa CardLayout.");
        }
    }
    
    // Aquí podrías añadir métodos para cambiar tema, etc. en el futuro.
    // public void aplicarTema(String nombreTema) { ... }
    
    public void setActionMap(Map<String, Action> actionMap) {this.actionMap = actionMap;}
    public void setBotonesPorNombre(Map<String, JButton> botones) {this.botonesPorNombre = botones;}
    public void setView(VisorView view) {this.view = view;}
} // FIN de la clase ViewManager