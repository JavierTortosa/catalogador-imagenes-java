package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder; // Para panelIzquierdo

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.renderers.MiniaturaListCellRenderer; // Asumiendo que lo usas en inicializarPanelImagenesMiniatura
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


public class VisorView extends JFrame {

	private static final Logger logger = LoggerFactory.getLogger(VisorView.class);
	
    private static final long serialVersionUID = 4L; // Incrementado por refactorización mayor
    
    // Componentes Comunes (Barras, etc.)
    private JPanel panelDeBotones;        
    private Map<String, JToolBar> barrasDeHerramientas;

    // --- REFERENCIAS EXTERNAS Y CONFIGURACIÓN ---
    private final VisorModel model;
    private final ThumbnailService servicioThumbs;
    private final ThemeManager themeManagerRef;
    
    private Map<String, JMenuItem> menuItemsPorNombre; 
    private int miniaturaScrollPaneHeight;
    
    // --- Estado Interno de la Vista (para pintura, etc.) ---
    private Image imagenReescaladaView; // Usada por paintComponent de etiquetaImagen
    private double zoomFactorView = 1.0;
    private int imageOffsetXView = 0;
    private int imageOffsetYView = 0;
    private boolean fondoACuadrosActivado = false;
    
    // --- Icono de Error de Imagen
    
    private Rectangle lastNormalBounds;
    private final ConfigurationManager configurationManagerRef; 
    private final IconUtils iconUtilsRef;
    
    private final ComponentRegistry registry;
    private JLabel etiquetaImagen; 
    
    private DefaultListModel<String> modeloListaMiniaturas;
    
    // --- BORDES PARA EL ESTADO DE SYNC ---
    private final Border syncActiveBorder = javax.swing.BorderFactory.createLineBorder(java.awt.Color.GREEN.darker(), 3);
    private final Border syncInactiveBorder = javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3);
    
    /**
      * Constructor principal de la ventana VisorView.
      * Inicializa las dependencias y llama al método de construcción de la UI.
      *
      * @param miniaturaPanelHeight Altura deseada para el scrollpane de miniaturas.
      * @param config Objeto ViewUIConfig con parámetros de apariencia y referencias (como IconUtils).
      * @param modelo El VisorModel principal.
      * @param servicioThumbs El ThumbnailService para renderers de miniaturas.
      * @param themeManager Parametro para la gestion del fondo del label de imagen      
      * @param menuBar La JMenuBar pre-construida (o un placeholder si se construye después).
      * @param toolbarPanel El JPanel de la toolbar pre-construido (o un placeholder).
      * @param menuItems Mapa de JMenuItems generados (puede ser vacío inicialmente).
      * @param botones Mapa de JButtons generados (puede ser vacío inicialmente).
      */
    public VisorView(
            int miniaturaPanelHeight,
            VisorModel modelo,
            ThumbnailService servicioThumbs,
            ThemeManager themeManager,
            ConfigurationManager configurationManager,
            ComponentRegistry registry,
            IconUtils iconUtils
    ) {
        super("Visor/Catalogador de Imágenes");
        logger.debug("[VisorView Constructor SIMPLIFICADO] Iniciando...");

        // --- 1. ASIGNACIÓN DE DEPENDENCIAS ESENCIALES ---
        this.model = Objects.requireNonNull(modelo, "VisorModel no puede ser null");
        this.servicioThumbs = Objects.requireNonNull(servicioThumbs, "ThumbnailService no puede ser null");
        this.themeManagerRef = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null");
        this.configurationManagerRef = Objects.requireNonNull(configurationManager, "ConfigurationManager no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
        this.iconUtilsRef = Objects.requireNonNull(iconUtils, "IconUtils no puede ser null");
        this.miniaturaScrollPaneHeight = miniaturaPanelHeight;
        this.modeloListaMiniaturas = new DefaultListModel<>();

        // Inicializar mapas vacíos. Se poblarán desde fuera.
        this.menuItemsPorNombre = new HashMap<>();
        this.barrasDeHerramientas = new HashMap<>();

        // --- 2. CONFIGURACIÓN BÁSICA DEL JFRAME ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        Tema temaActual = this.themeManagerRef.getTemaActual();
        getContentPane().setBackground(temaActual.colorFondoPrincipal());
        
        // --- 3. LÓGICA INTERNA DE LA VENTANA (NO DE COMPONENTES) ---
        // Restaurar tamaño y posición
        if (this.configurationManagerRef != null) {
            int x = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_X, -1);
            int y = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_Y, -1);
            int w = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_WIDTH, 1280);
            int h = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_HEIGHT, 720);

            if (w > 50 && h > 50 && x != -1 && y != -1) {
                setBounds(x, y, w, h);
            } else {
                setSize(w, h);
                setLocationRelativeTo(null);
            }
            
            this.lastNormalBounds = getBounds();
            
            boolean wasMaximized = this.configurationManagerRef.getBoolean(ConfigKeys.WINDOW_MAXIMIZED, false);
            if (wasMaximized) {
                SwingUtilities.invokeLater(() -> setExtendedState(JFrame.MAXIMIZED_BOTH));
            }
        } else {
            setSize(1280, 720);
            setLocationRelativeTo(null);
            this.lastNormalBounds = getBounds();
        }

        // Listener para guardar las dimensiones al mover/redimensionar
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (getExtendedState() == JFrame.NORMAL) {
                    lastNormalBounds = getBounds();
                }
            }
            @Override
            public void componentResized(ComponentEvent e) {
                if (getExtendedState() == JFrame.NORMAL) {
                    lastNormalBounds = getBounds();
                }
            }
        });

        actualizarBordeDeSincronizacion(modelo.isSyncVisualizadorCarrusel());
        
    } // --- FIN del constructor VisorView ---
    
    

    
     /**
      * Devuelve los últimos bounds conocidos de la ventana cuando estaba en estado normal (no maximizada).
      * Si la ventana está actualmente en estado normal, devuelve sus bounds actuales.
      * Si está maximizada, devuelve el último valor guardado por el ComponentListener.
      * @return Un objeto Rectangle con los bounds normales, o null si no se han podido determinar.
      */
     public Rectangle getLastNormalBounds() {
         if (getExtendedState() == JFrame.NORMAL) {
             // Si la ventana ya está en estado normal, sus bounds actuales son los correctos.
             return getBounds();
         }
         // Si está maximizada o minimizada, debemos devolver el valor que guardamos
         // la última vez que SÍ estuvo en estado normal.
         return this.lastNormalBounds; 
     }
     

     public void setListaImagenesModel(DefaultListModel<String> nuevoModelo) {
        // 1. Obtener la JList de nombres desde el registro.
        JList<String> listaNombres = registry.get("list.nombresArchivo");

        // 2. Comprobar si la lista existe antes de asignarle el modelo.
        if (listaNombres != null) {
            listaNombres.setModel(nuevoModelo);
            logger.debug("[VisorView] Modelo asignado a 'list.nombresArchivo'. Tamaño: " + nuevoModelo.getSize());
        } else {
            logger.warn("WARN [setListaImagenesModel]: El componente 'list.nombresArchivo' no se encontró en el registro.");
        }
    } // --- FIN del metodo setListaImagenesModel ---
    

    /**
     * Limpia el área de visualización de la imagen principal, eliminando la imagen
     * actual, cualquier icono de error, o texto de carga.
     * El paintComponent dibujará el fondo (a cuadros o sólido).
     */
    public void limpiarImagenMostrada() {
        if (this.etiquetaImagen == null) return;

        // this.errorAlCargarImagenActual = false; // Si usas un flag para paintComponent
        this.imagenReescaladaView = null; // No hay imagen para paintComponent
        this.zoomFactorView = 1.0;
        this.imageOffsetXView = 0;
        this.imageOffsetYView = 0;

        // Limpiar cualquier texto o icono de error/carga del JLabel
        this.etiquetaImagen.setText(null);
        this.etiquetaImagen.setIcon(null);

        this.etiquetaImagen.repaint(); // paintComponent dibujará el fondo
    }


    public void setTituloPanelIzquierdo(String titulo) {
    	
    	JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");
    	
        if (panelIzquierdo == null || !(panelIzquierdo.getBorder() instanceof TitledBorder)) {
            logger.warn("WARN [setTituloPanelIzquierdo]: panelIzquierdo o su TitledBorder nulos.");
            return;
        }
        TitledBorder borde = (TitledBorder) panelIzquierdo.getBorder();
        borde.setTitle(titulo != null ? titulo : "");
        panelIzquierdo.repaint();
    }

    public void setJMenuBarVisible(boolean visible) {
        JMenuBar mb = getJMenuBar();
        if (mb != null && mb.isVisible() != visible) {
            mb.setVisible(visible);
            SwingUtilities.invokeLater(this::revalidateFrame);
        }
    }
    
    public void setToolBarVisible(boolean visible) {
        if (this.panelDeBotones != null && this.panelDeBotones.isVisible() != visible) {
            this.panelDeBotones.setVisible(visible);
            // El padre de panelDeBotones es el contentPane del JFrame.
             SwingUtilities.invokeLater(this::revalidateFrame);
        }
    }

    
    public void setFileListVisible(boolean visible) {
        // 1. Obtener los componentes necesarios desde el registro.
        // La clave para el panel izquierdo del split podría ser algo como "panel.split.izquierdo"
        // Asumiré que ViewBuilder lo registra con esa clave. Si no, ajusta el nombre.
        // NOTA: ViewBuilder crea un "panelIzquierdo" que contiene una JList, así que es ese el que debemos mostrar/ocultar.
        JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");
        JSplitPane splitPane = registry.get("splitpane.main");

        // 2. Validar que los componentes existan en el registro.
        if (panelIzquierdo == null) {
            logger.error("ERROR [VisorView setFileListVisible]: 'panel.izquierdo.listaArchivos' no encontrado en el registro.");
            return;
        }

        // 3. Aplicar la lógica de visibilidad.
        if (panelIzquierdo.isVisible() != visible) {
            logger.debug("  [VisorView setFileListVisible] Cambiando visibilidad del panel izquierdo a: " + visible);
            panelIzquierdo.setVisible(visible);

            if (splitPane != null) {
                if (visible) {
                    // Restaurar la posición del divisor cuando se vuelve a mostrar el panel.
                    SwingUtilities.invokeLater(() -> {
                        double dividerLocationPercentage = 0.25;
                        int newDividerLocation = (int) (splitPane.getWidth() * dividerLocationPercentage);
                        splitPane.setDividerLocation(newDividerLocation);
                        splitPane.revalidate();
                        splitPane.repaint();
                    });
                } else {
                    // Al ocultar el panel izquierdo, algunos L&F contraen el divisor.
                    // Podríamos guardarlo si quisiéramos restaurarlo al valor exacto.
                    splitPane.resetToPreferredSizes();
                }
            } else {
                logger.warn("WARN [VisorView setFileListVisible]: 'splitpane.main' no encontrado en el registro. No se puede ajustar el divisor.");
            }

            revalidateFrame();
            logger.debug("  [VisorView setFileListVisible] Frame revalidado y repintado.");
        } else {
            logger.debug("  [VisorView setFileListVisible] El panel izquierdo ya está en el estado de visibilidad deseado: " + visible);
        }
    }

    
    private void revalidateFrame() {
        this.revalidate();
        this.repaint();
    }

    public void setCheckeredBackgroundEnabled(boolean activado) {
        if (this.fondoACuadrosActivado != activado) {
            this.fondoACuadrosActivado = activado;
            
            JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
            if (etiquetaImagen != null) {
                etiquetaImagen.repaint();
            }
        }
    }

    

    public void setEstadoMenuActivarZoomManual(boolean seleccionado) {
        String zoomMenuKey = "interfaz.menu.zoom.Activar_Zoom_Manual";
        if (menuItemsPorNombre == null) return;
        JMenuItem zoomMenuItem = this.menuItemsPorNombre.get(zoomMenuKey);
        if (zoomMenuItem instanceof JCheckBoxMenuItem) {
            JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem) zoomMenuItem;
            if (cbItem.isSelected() != seleccionado) cbItem.setSelected(seleccionado);
        }
    }// --- FIN del metodo

    
    
    
    /**
     * Muestra un indicador de "Cargando..." en el área principal de la imagen.
     * Limpia cualquier imagen o icono de error previo.
     *
     * @param mensaje El mensaje a mostrar (ej. "Cargando: nombre_archivo...").
     */
    public void mostrarIndicadorCargaImagenPrincipal(String mensaje) {
        // 1. Obtener la etiqueta de imagen desde el registro.
        JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
        if (etiquetaImagen == null) {
            logger.error("ERROR [mostrarIndicadorCargaImagenPrincipal]: 'label.imagenPrincipal' no encontrado en el registro.");
            return;
        }

        String mensajeAMostrar = (mensaje != null && !mensaje.trim().isEmpty()) ? mensaje : "Cargando...";

        // 2. Modificar el estado interno de la vista (esto es correcto, ya que afecta al paintComponent).
        this.imagenReescaladaView = null; // No hay imagen para dibujar mientras se carga.

        // 3. Modificar el componente JLabel obtenido del registro.
        etiquetaImagen.setIcon(null);
        etiquetaImagen.setText(mensajeAMostrar);

        Tema temaActual = this.themeManagerRef.getTemaActual();
        if (temaActual != null) {
             etiquetaImagen.setForeground(temaActual.colorTextoPrimario());
        } else {
             etiquetaImagen.setForeground(Color.BLACK); // Fallback
        }
        
        etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
        etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
        etiquetaImagen.setOpaque(false); // Para que el fondo personalizado de paintComponent se vea.
        etiquetaImagen.repaint();
        
    } // --- FIN del metodo mostrarIndicadorCargaImagenPrincipal ---
    
    
    /**
     * Actualiza el borde de la ventana principal para indicar visualmente
     * si el modo de sincronización está activo o no.
     * @param activado true para mostrar un borde rojo, false para quitarlo.
     */
    public void actualizarBordeDeSincronizacion(boolean activado) {
        SwingUtilities.invokeLater(() -> {
            logger.debug("[VisorView] Actualizando borde de sincronización a: {}", activado);
            getRootPane().setBorder(activado ? syncActiveBorder : syncInactiveBorder);
        });
    } // --- Fin del método actualizarBordeDeSincronizacion ---
    
    
    // Métodos para actualizar los mapas y barras (recibidos de AppInitializer)
    public void setActualJMenuBar(JMenuBar menuBar) {
        if (menuBar != null) this.setJMenuBar(menuBar);
    }
    public void setActualToolbarPanel(JPanel toolbarPanel) {
        if (toolbarPanel != null) {
            if (this.panelDeBotones != null) remove(this.panelDeBotones); // Quitar el placeholder si existe
            this.panelDeBotones = toolbarPanel;
            add(this.panelDeBotones, BorderLayout.NORTH); // Añadir el real
            revalidateFrame();
        }
    }
    public void setActualMenuItemsMap(Map<String, JMenuItem> menuItems) {
        this.menuItemsPorNombre = (menuItems != null) ? menuItems : new HashMap<>();
    }
    
    
    
    
    /**
     * Solicita un refresco completo de los renderers de la lista de miniaturas.
     * Esto es útil cuando cambian las configuraciones que afectan la apariencia
     * de las celdas de las miniaturas (ej. mostrar/ocultar nombres, cambio de tema,
     * cambio de tamaño de miniaturas).
     * Crea una nueva instancia del renderer con la configuración actual y la asigna
     * a la JList de miniaturas, forzando una revalidación y repintado.
     */
    public void solicitarRefrescoRenderersMiniaturas() {
        logger.debug("[VisorView] Iniciando solicitud de refresco para renderers de miniaturas...");

        // --- 1. OBTENER EL COMPONENTE DESDE EL REGISTRO ---
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        if (listaMiniaturas == null) {
            logger.error("ERROR [solicitarRefrescoRenderersMiniaturas]: 'list.miniaturas' no encontrado en el registro.");
            return;
        }

        // --- 2. VALIDAR OTRAS DEPENDENCIAS (sin cambios) ---
        if (this.model == null || this.configurationManagerRef == null || this.servicioThumbs == null || this.themeManagerRef == null || this.iconUtilsRef == null) {
             logger.error("ERROR [solicitarRefrescoRenderersMiniaturas]: Faltan dependencias esenciales.");
             return;
        }

        // --- 3. OBTENER CONFIGURACIONES (sin cambios) ---
        boolean mostrarNombresActual = this.configurationManagerRef.getBoolean(ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, true);
        int thumbWidth = this.configurationManagerRef.getInt("miniaturas.tamano.normal.ancho", 40);
        int thumbHeight = this.configurationManagerRef.getInt("miniaturas.tamano.normal.alto", 40);

        // --- 4. CREAR EL NUEVO RENDERER (sin cambios) ---
        MiniaturaListCellRenderer newRenderer = new MiniaturaListCellRenderer(
            this.servicioThumbs,
            this.model,
            this.themeManagerRef,
            this.iconUtilsRef,
            thumbWidth,
            thumbHeight,
            mostrarNombresActual
        );

        // --- 5. APLICAR EL NUEVO RENDERER A LA JLIST OBTENIDA DEL REGISTRO ---
        listaMiniaturas.setCellRenderer(newRenderer);
        listaMiniaturas.setFixedCellHeight(newRenderer.getAlturaCalculadaDeCelda());
        listaMiniaturas.setFixedCellWidth(newRenderer.getAnchoCalculadaDeCelda());
        listaMiniaturas.revalidate();
        listaMiniaturas.repaint();

        logger.debug("[VisorView] Refresco de renderers de miniaturas completado.");
    } // --- FIN del metodo solicitarRefrescoRenderersMiniaturas ---
    
    
    public void addListaNombresSelectionListener(javax.swing.event.ListSelectionListener listener) {
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres != null) {
            listaNombres.addListSelectionListener(listener);
        }
    }

    public void addListaMiniaturasSelectionListener(javax.swing.event.ListSelectionListener listener) {
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        if (listaMiniaturas != null) {
            listaMiniaturas.addListSelectionListener(listener);
        }
    }

    public void addEtiquetaImagenMouseWheelListener(java.awt.event.MouseWheelListener listener) {
        // etiquetaImagen SÍ es un campo de VisorView, por lo que este método puede quedar igual.
        // Solo añadimos una comprobación por si se refactoriza en el futuro.
        JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
        if (etiquetaImagen != null) {
            etiquetaImagen.addMouseWheelListener(listener);
        }
    }

    public void addEtiquetaImagenMouseListener(java.awt.event.MouseListener listener) {
        JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
        if (etiquetaImagen != null) {
            etiquetaImagen.addMouseListener(listener);
        }
    }

    public void addEtiquetaImagenMouseMotionListener(java.awt.event.MouseMotionListener listener) {
        JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
        if (etiquetaImagen != null) {
            etiquetaImagen.addMouseMotionListener(listener);
        }
    }
    
    public Image getImagenReescaladaView() {
        return this.imagenReescaladaView;
    }

   
    // Getters para la barra de botones
    public JPanel getPanelDeBotones() {return this.panelDeBotones; }
    public Map<String, JToolBar> getToolbars() {return this.barrasDeHerramientas;}
    public boolean isFondoACuadrosActivado() {return this.fondoACuadrosActivado;}
	public DefaultListModel<String> getModeloListaMiniaturas(){ return this.modeloListaMiniaturas; }
    
    
} // Fin clase VisorView