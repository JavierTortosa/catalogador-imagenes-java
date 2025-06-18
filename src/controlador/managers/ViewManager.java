package controlador.managers;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import controlador.utils.ComponentRegistry;
import servicios.ConfigurationManager;
import vista.VisorView;
import vista.panels.ImageDisplayPanel;

public class ViewManager {

    private VisorView view;
    private ConfigurationManager configuration;
    private final ComponentRegistry registry;
    private final ConfigurationManager config;
    
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

    // Aquí podrías añadir métodos para cambiar tema, etc. en el futuro.
    // public void aplicarTema(String nombreTema) { ... }
} // FIN de la clase ViewManager